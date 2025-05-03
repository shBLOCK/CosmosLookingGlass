@file:Suppress("FunctionName", "UNNECESSARY_NOT_NULL_ASSERTION", "PropertyName", "ClassName")

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.Executors

plugins {
    kotlin("multiplatform") version "2.1.20"
}

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

tasks.wrapper { distributionType = Wrapper.DistributionType.ALL }

kotlin {
    // kotlin multiplatform (jvm + js) setup:
    jvm { }
    jvmToolchain(21)

    compilerOptions {
        freeCompilerArgs.add("-Xnested-type-aliases")
    }

    js {
        binaries.executable()
        browser {
            @OptIn(ExperimentalDistributionDsl::class)
            distribution {
                outputDirectory.set(File("${rootDir}/dist/js"))
            }
            commonWebpackConfig {
                outputFileName = "index.js"
                sourceMaps = true
            }
            testTask {
                enabled = false
            }
        }
        compilerOptions {
            target.set("es2015")
        }
    }

    sourceSets {
        val lwjglVersion = "3.3.6"

        // JVM target platforms, you can remove entries from the list in case you want to target
        // only a specific platform
        val targetPlatforms = listOf("natives-windows", "natives-linux", "natives-macos", "natives-macos-arm64")

        @Suppress("unused")
        val commonMain by getting {
            dependencies {
                // add additional kotlin multi-platform dependencies here...

                implementation("kool:kool-core")

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                implementation("org.jetbrains.kotlinx:atomicfu:0.27.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
            }
        }

        @Suppress("unused")
        val jvmMain by getting {
            dependencies {
                // add additional jvm-specific dependencies here...

                // add required runtime libraries for lwjgl
                for (platform in targetPlatforms) {
                    // lwjgl runtime libs
                    runtimeOnly("org.lwjgl:lwjgl:$lwjglVersion:$platform")
                    listOf("glfw", "opengl", "jemalloc", "nfd", "stb", "vma", "shaderc").forEach { lib ->
                        runtimeOnly("org.lwjgl:lwjgl-$lib:$lwjglVersion:$platform")
                    }
                }
            }
        }

        @Suppress("unused")
        val jsMain by getting {
            dependencies {
                implementation(devNpm("terser", "5.39.0"))
            }
        }
    }
}

task("runnableJar", Jar::class) {
    dependsOn("jvmJar")

    group = "app"
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    archiveAppendix.set("runnable")
    manifest {
        attributes["Main-Class"] = "platform.JvmLauncherKt"
    }

    configurations
        .asSequence()
        .filter { it.name.startsWith("common") || it.name.startsWith("jvm") }
        .map { it.copyRecursive().fileCollection { true } }
        .flatten()
        .distinct()
        .filter { it.exists() }
        .map { if (it.isDirectory) it else zipTree(it) }
        .forEach { from(it) }
    from(layout.buildDirectory.files("classes/kotlin/jvm/main"))

    doLast {
        copy {
            from(layout.buildDirectory.file("libs/${archiveBaseName.get()}-runnable.jar"))
            into("${rootDir}/dist/jvm")
        }
    }
}

task("runApp", JavaExec::class) {
    group = "app"
    dependsOn("jvmMainClasses")

    classpath = layout.buildDirectory.files("classes/kotlin/jvm/main")
    configurations
        .filter { it.name.startsWith("common") || it.name.startsWith("jvm") }
        .map { it.copyRecursive().filter { true } }
        .forEach { classpath += it }

    mainClass.set("platform.JvmLauncherKt")
}

@Suppress("unused")
val build by tasks.getting(Task::class) {
    dependsOn("runnableJar")
}

operator fun File.div(relative: String) = resolve(relative)
operator fun File.div(relative: File) = resolve(relative)
fun File.withStem(stem: String) = this.parentFile.resolve("${stem}.${extension}")
fun File.withExtension(ext: String) = this.parentFile.resolve("${nameWithoutExtension}.${ext}")

val KotlinSourceSet.resourcesDir
    get() = resources.srcDirs.also { check(it.size == 1) }.first()!!

fun unreachable(): Nothing = throw IllegalStateException("unreachable")

@Suppress("unused")
val clean by tasks.getting(Task::class) {
    doLast {
        delete("${rootDir}/dist")
        delete(fileTree("${rootDir}/wechat/miniprogram/index/src") {
            exclude("README.md")
        })
    }
}

val jsWeChatBuild by tasks.registering {
    group = "wechat"
    dependsOn(tasks["jsBrowserDistribution"])
    doLast {
        delete(fileTree("${rootDir}/wechat/miniprogram/index/src") {
            exclude("README.md")
        })
        copy {
            from(files("${rootDir}/dist/js")) {
                exclude("index.html")
            }
            into("${rootDir}/wechat/miniprogram/index/src")
        }
    }
}

val jsWeChatMinify by tasks.registering(Exec::class) {
    group = "wechat"

    val srcRoot = "${rootDir}/wechat/miniprogram/index/src"

    fun File.KB() = "%.1fKB".format(length() / 1024.0)

    doFirst { println("Source: ${file("${srcRoot}/index.js").KB()}") }

    executable = kotlinNodeJsEnvSpec.executable.get()
    args(
        "${rootDir}/build/js/node_modules/terser/bin/terser",
        "--source-map", "\"url='${srcRoot}/index.min.js.map'\"",
        "--ecma", "2015",
        "--compress", "--mangle",
        "--timings",
        "--output", "${srcRoot}/index.min.js", "${srcRoot}/index.js"
    )

    doLast { println("Result: ${file("${srcRoot}/index.min.js").KB()}") }

    doLast {
        Files.move(
            file("${srcRoot}/index.min.js").toPath(),
            file("${srcRoot}/index.js").toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
        Files.move(
            file("${srcRoot}/index.min.js.map").toPath(),
            file("${srcRoot}/index.js.map").toPath(),
            StandardCopyOption.REPLACE_EXISTING
        )
    }
}

@Suppress("unused")
val jsWeChatMinifiedBuild by tasks.registering {
    group = "wechat"
    dependsOn(jsWeChatBuild)
    dependsOn(jsWeChatMinify)
    jsWeChatMinify.get().mustRunAfter(jsWeChatBuild)
}

val assetsRoot = file("${rootDir}/assets")

// region Asset Generation
enum class AssetPlat(val path: String) {
    Desktop("desktop"), Web("web")
}

abstract class PlatformMedia(
    /** Should be a relative file that's relative to `assets/tmp`. */
    val file: File
) {
    companion object {
        val registry = mutableListOf<PlatformMedia>()
    }
}
PlatformMedia.registry.clear() // o..kay...

class _PlatformMedia_Img(
    pFile: File,
    val quality: Double,
    val lossless: Boolean
) : PlatformMedia(pFile)

private fun PlatformMedia.Companion._pmFile(orgFile: File): File {
    val file = if (orgFile.isAbsolute) orgFile else (assetsRoot / orgFile).absoluteFile!!
    check(file.startsWith(assetsRoot / "tmp")) { "Invalid PlatformMedia path: $orgFile" }
    return file.relativeTo(assetsRoot / "tmp")
}

fun PlatformMedia.Companion.img(file: File, quality: Double = 0.8, lossless: Boolean = false) {
    registry += _PlatformMedia_Img(_pmFile(file), quality, lossless)
}

enum class CubeMapFace(val suffix: String) {
    NEG_X("_neg_x"), POS_X("_pos_x"),
    NEG_Y("_neg_y"), POS_Y("_pos_y"),
    NEG_Z("_neg_z"), POS_Z("_pos_z")
}

fun PlatformMedia.Companion.cubemap(file: File, quality: Double = 0.8, lossless: Boolean = false) {
    for (face in CubeMapFace.values()) {
        val faceFile = file.withStem("${file.nameWithoutExtension}${face.suffix}")
        img(faceFile, quality, lossless)
    }
}

class Abort : RuntimeException("Abort")

inline fun runAbortable(block: () -> Any) {
    try {
        block()
    } catch (_: Abort) {
    }
}

fun deferredExec(action: ExecSpec.(isInit: Boolean) -> Unit): () -> ExecResult {
    runAbortable {
        exec {
            action(true)
            throw Abort()
        }
    }
    return { exec { action(false) } }
}

fun Iterable<() -> Any?>.runAll() = forEach { it() }

val gen_fonts by tasks.registering {
    group = "assets"

    PlatformMedia.img(File("tmp/fonts/NotoSans.png"), lossless = true)

    val actions = mutableListOf(
        {
            mkdir("${assetsRoot}/generated/common/fonts")
            mkdir("${assetsRoot}/tmp/fonts")
        },
        deferredExec { isInit ->
            workingDir(assetsRoot)
            executable("${assetsRoot}/tools/bin/msdf-atlas-gen.exe")
            args(
                "-varfont",
                "raw/fonts/NotoSans-VariableFont_wdth,wght.ttf"
                    .also { if (isInit) inputs.file(workingDir / it) }
                    + "?wght=400",
                "-type", "mtsdf",
                "-size", "36",
                "-chars", "[0x20, 0x7E]",
                "-imageout", "tmp/fonts/NotoSans.png".also { if (isInit) outputs.file(workingDir / it) },
                "-json", "generated/common/fonts/NotoSans.json".also { if (isInit) outputs.file(workingDir / it) }
            )
        },
        {
            val metaFile = file("${assetsRoot}/generated/common/fonts/NotoSans.json")
            val gson = GsonBuilder().create()
            val jsonText = metaFile.readText()
            val data = gson.fromJson(jsonText, JsonObject::class.java)
            data.addProperty("name", metaFile.nameWithoutExtension)
            data["atlas"].asJsonObject.remove("distanceRangeMiddle")
            metaFile.writeText(gson.toJson(data))
        }
    )

    doLast { actions.runAll() }
}
gen_fonts.get()

fun ExecSpec.prepFileDir(file: File) = Unit.also {
    (if (!file.isAbsolute) workingDir.resolve(file) else file).ensureParentDirsCreated()
}

fun ExecSpec.prepFileDir(file: String) = prepFileDir(File(file))

val gen_preprocessImgs by tasks.registering {
    group = "assets"

    val actions = mutableListOf(
        deferredExec { isInit ->
            workingDir(assetsRoot)
            executable("${assetsRoot}/tools/bin/magick.exe")
            args(
                "raw/textures/misc/starmap_2020_16k.exr".also { if (isInit) inputs.file(workingDir / it) },
                "-set", "colorspace", "RGB",
                "-modulate", 1000,
                "-colorspace", "sRGB",
                "-depth", 8,
                "tmp/textures/misc/background_starmap_equirec.png"
                    .also { if (!isInit) prepFileDir(it) }
                    .also { if (isInit) outputs.file(workingDir / it) }
            )
        }
    )

    doLast { actions.runAll() }
}
gen_preprocessImgs.get()

val gen_cubemaps by tasks.registering {
    group = "assets"

    val pythonScript = assetsRoot / "tools/src/cubemapper.py"
    inputs.file(pythonScript)

    val actions = mutableListOf<() -> Unit>()

    fun gen(input: String, output: String, size: Int, orientation: String = "map") {
        val outputFile = File(output)
        inputs.file(assetsRoot / input)
        CubeMapFace.values().forEach { face ->
            outputs.file(assetsRoot / outputFile.withStem(outputFile.nameWithoutExtension + face.suffix))
        }

        actions += {
            exec {
                println("$input -> $output (size=$size)")
                workingDir("${assetsRoot}/tools")
                executable("${assetsRoot}/tools/bin/hatch.exe")
                args("run", "python", pythonScript)
                args(
                    "--input", assetsRoot / input,
                    "--output", assetsRoot / output,
                    "--size", size,
                    "--orientation-mode", orientation
                )
            }
        }
    }

    fun cb_color(input: String, output: String, sizeHighRes: Int = 2048, size: Int = 256) {
        gen(
            "raw/textures/celestial_body/${input}",
            "tmp/textures/celestial_body/${output}.png"
                .also { PlatformMedia.cubemap(File(it), quality = 0.7) },
            size
        )
        gen(
            "raw/textures/celestial_body/${input}",
            "tmp/cdn/textures/celestial_body/${output}_highres.png"
                .also { PlatformMedia.cubemap(File(it)) },
            sizeHighRes
        )
    }

    cb_color("earth/ppe_color_10k.jpg", "earth/color")
    cb_color("jupiter/ppe_color_6k.jpg", "jupiter/color")
    cb_color("mars/ppe_color_12k.jpg", "mars/color")
    cb_color("mercury/ppe_color_1k.jpg", "mercury/color")
    cb_color("moon/ppe_color_4k.jpg", "moon/color")
    cb_color("neptune/ppe_color_1k.jpg", "neptune/color")
    cb_color("saturn/ppe_color_2k.jpg", "saturn/color")
    cb_color("sun/ppe_color_1k.jpg", "sun/color")
    cb_color("uranus/ppe_color_1k.jpg", "uranus/color")
    cb_color("venus/ppe_color_2k.jpg", "venus/color")

    gen(
        "tmp/textures/misc/background_starmap_equirec.png",
        "tmp/cdn/textures/misc/background_starmap_highres.png"
            .also { PlatformMedia.cubemap(File(it)) },
        2048
    )
    gen(
        "tmp/textures/misc/background_starmap_equirec.png",
        "tmp/textures/misc/background_starmap.png"
            .also { PlatformMedia.cubemap(File(it), quality = 0.7) },
        256
    )

    doLast { actions.runAll() }
}
gen_cubemaps.get()

val gen_mediaPlatformFormat by tasks.registering {
    group = "assets"

    data class Conversion(
        val srcFile: File,
        val dstFile: File,
        val action: () -> Any?
    )

    val cntLock = object {}
    var cnt = 0

    val conversions = mutableListOf<Conversion>()
    for (pm in PlatformMedia.registry) {
        when (pm) {
            is _PlatformMedia_Img -> {
                for (platform in AssetPlat.values()) {
                    val srcFile = File("tmp") / pm.file
                    val type = when (platform) {
                        AssetPlat.Desktop -> if (pm.lossless) "png" else "jpg"
//                        AssetPlat.Web -> "webp" // TODO: wechat bug: https://developers.weixin.qq.com/community/develop/doc/0000a27e0a4bb0115bb11af8360000?highLine=webp%2520cdn https://developers.weixin.qq.com/community/develop/doc/000a24dddb0978ed5462f60556bc00?highLine=webp%2520cdn
                        AssetPlat.Web -> if (pm.file.startsWith("cdn/")) "webp" else "jpg"
                    }
                    val dstFile = File("generated") / platform.path / pm.file.withExtension(type)
                    inputs.file(assetsRoot / srcFile)
                    outputs.file(assetsRoot / dstFile)
                    val action = {
                        exec {
                            workingDir(assetsRoot)
                            executable("${assetsRoot}/tools/bin/magick.exe")
                            args(
                                srcFile,
                                "-quality", (pm.quality * 100).toInt().coerceIn(1..100),
                                "-define", "webp:lossless=${pm.lossless}",
                                "-define", "webp:method=6",
                                "-define", "png:compression-level=9",
                                dstFile.also { prepFileDir(it) }
                            )
                            synchronized(cntLock) {
                                println("[${++cnt}/${conversions.size}] $srcFile -> $dstFile")
                            }
                        }
                    }
                    conversions += Conversion(srcFile, dstFile, action)
                }
            }

            else -> unreachable()
        }
    }

    doLast {
        val pool = Executors.newFixedThreadPool(8)
        conversions.forEach { pool.submit { it.action() } }
        pool.shutdown()
        pool.awaitTermination(10L, TimeUnit.HOURS)
    }
}
gen_mediaPlatformFormat.get()

@Suppress("unused")
val cleanGeneratedAssets by tasks.registering {
    group = "assets"
    clean.dependsOn(this)
    doLast {
        delete(assetsRoot / "tmp")
        delete(assetsRoot / "generated")
    }
}

@Suppress("unused")
val generateAssets by tasks.registering {
    group = "assets"
    doFirst {
        if (!org.gradle.internal.os.OperatingSystem.current().isWindows)
            throw GradleException("processAssets task only works on windows.")
        mkdir(assetsRoot / "generated")
    }
    doFirst { println("Note: when generated assets are removed, the cleanGeneratedAssets task must manually ran to clear the remaining old generated files.") }
    val genTasks = listOf(
        gen_fonts,
        gen_preprocessImgs,
        gen_cubemaps,
        gen_mediaPlatformFormat,
    )
    dependsOn(*genTasks.toTypedArray())
    // run one-by-one
    for (i in genTasks.indices.drop(1)) {
        genTasks[i].get().mustRunAfter(genTasks[i - 1])
    }
}
// endregion

// region Asset Deploy
@Suppress("unused")
val cleanMergedAndDeployedAssets by tasks.registering {
    group = "assets"

    clean.dependsOn(this)

    doLast {
        delete(assetsRoot / "merged")
        delete(kotlin.sourceSets["jvmMain"].resourcesDir / "assets")
        delete(kotlin.sourceSets["jsMain"].resourcesDir / "assets")
    }
}

val mergeAssets by tasks.registering {
    group = "assets"

    inputs.dir("${assetsRoot}/static/")
    inputs.dir("${assetsRoot}/generated/")
    outputs.dir("${assetsRoot}/merged/")

    doFirst {
        delete("${rootDir}/assets/merged")
    }

    doLast {
        for (platform in AssetPlat.values()) {
            for (type in arrayOf("static", "generated")) {
                for (path in arrayOf("common", platform.path)) {
                    val fromPath = "${type}/${path}"
                    val intoPath = "merged/${platform.path}"
                    println("$fromPath -> $intoPath")
                    copy {
                        from(assetsRoot / fromPath)
                        into(assetsRoot / intoPath)
                    }
                }
            }
        }
    }
}

val deployAssetsDesktop by tasks.registering {
    group = "assets"

    dependsOn(mergeAssets)

    inputs.dir("${assetsRoot}/merged/desktop/")
    outputs.dir(kotlin.sourceSets["jvmMain"].resourcesDir / "assets")

    doFirst {
        delete(kotlin.sourceSets["jvmMain"].resourcesDir / "assets")
    }

    doLast {
        copy {
            from("${assetsRoot}/merged/desktop/")
            into(kotlin.sourceSets["jvmMain"].resourcesDir / "assets")
        }
    }
}

val deployAssetsWeb by tasks.registering {
    group = "assets"

    dependsOn(mergeAssets)

    inputs.dir("${assetsRoot}/merged/web/")
    outputs.dir(kotlin.sourceSets["jsMain"].resourcesDir / "assets")

    doFirst {
        delete(kotlin.sourceSets["jsMain"].resourcesDir / "assets")
    }

    doLast {
        copy {
            from("${assetsRoot}/merged/web/")
            into(kotlin.sourceSets["jsMain"].resourcesDir / "assets")
        }
    }
}

@Suppress("unused")
val jvmProcessResources by tasks.getting(Task::class) {
    dependsOn(deployAssetsDesktop)
}

@Suppress("unused")
val jsProcessResources by tasks.getting(Task::class) {
    dependsOn(deployAssetsWeb)
}
// endregion