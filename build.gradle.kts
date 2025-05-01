@file:Suppress("FunctionName")

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.jetbrains.kotlin.gradle.internal.ensureParentDirsCreated
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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

val KotlinSourceSet.resourcesDir
    get() = resources.srcDirs.also { check(it.size == 1) }.first()!!

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

val assetsRoot = "${rootDir}/assets"

// region Asset Generation
val generateFonts by tasks.registering {
    group = "assets"
    doLast {
        mkdir("${assetsRoot}/generated/fonts")

        exec {
            workingDir(assetsRoot)
            executable("${assetsRoot}/tools/bin/msdf-atlas-gen.exe")
            args(
                "-varfont", "./raw/fonts/NotoSans-VariableFont_wdth,wght.ttf?wght=400",
                "-type", "mtsdf",
                "-size", "36",
                "-chars", "[0x20, 0x7E]",
                "-imageout", "./generated/fonts/NotoSans.png",
                "-json", "./generated/fonts/NotoSans.json"
            )
        }

        run {
            val metaFile = file("${assetsRoot}/generated/fonts/NotoSans.json")
            val gson = GsonBuilder().create()
            val jsonText = metaFile.readText()
            val data = gson.fromJson(jsonText, JsonObject::class.java)
            data.addProperty("name", metaFile.nameWithoutExtension)
            data["atlas"].asJsonObject.remove("distanceRangeMiddle")
            metaFile.writeText(gson.toJson(data))
        }
    }
}

fun ExecSpec.prepFileDir(file: String) = Unit.also {
    workingDir.resolve(file).ensureParentDirsCreated()
}

val processImgs by tasks.registering {
    group = "assets"
    doLast {
        exec {
            workingDir(assetsRoot)
            executable("${assetsRoot}/tools/bin/magick.exe")
            args(
                "raw/textures/misc/starmap_2020_16k.exr",
                "-set", "colorspace", "RGB",
                "-modulate", 1000,
                "-colorspace", "sRGB",
                "-depth", 8,
                "tmp/textures/misc/background_starmap.png".also { prepFileDir(it) }
            )
        }
    }
}

val generateCubemaps by tasks.registering {
    group = "assets"
    doLast {
        fun gen(input: String, output: String, size: Int, orientation: String = "map") = exec {
            println("$input -> $output (size=$size)")
            workingDir("${assetsRoot}/tools")
            executable("${assetsRoot}/tools/bin/hatch.exe")
            args("run", "python", "src/cubemapper.py")
            args(
                "--input", "${assetsRoot}/${input}",
                "--output", "${assetsRoot}/${output}",
                "--size", size,
                "--orientation-mode", orientation
            )
        }

        fun gen_r2g(input: String, output: String, size: Int) =
            gen("raw/textures/${input}", "generated/textures/${output}", size)

        val SIZE = 2048
        gen_r2g("celestial_body/earth/ppe_color_10k.jpg", "celestial_body/earth/color.png", SIZE)
        gen_r2g("celestial_body/jupiter/ppe_color_6k.jpg", "celestial_body/jupiter/color.png", SIZE)
        gen_r2g("celestial_body/mars/ppe_color_12k.jpg", "celestial_body/mars/color.png", SIZE)
        gen_r2g("celestial_body/mercury/ppe_color_1k.jpg", "celestial_body/mercury/color.png", SIZE)
        gen_r2g("celestial_body/moon/ppe_color_4k.jpg", "celestial_body/moon/color.png", SIZE)
        gen_r2g("celestial_body/neptune/ppe_color_1k.jpg", "celestial_body/neptune/color.png", SIZE)
        gen_r2g("celestial_body/saturn/ppe_color_2k.jpg", "celestial_body/saturn/color.png", SIZE)
        gen_r2g("celestial_body/sun/ppe_color_1k.jpg", "celestial_body/sun/color.png", SIZE)
        gen_r2g("celestial_body/uranus/ppe_color_1k.jpg", "celestial_body/uranus/color.png", SIZE)
        gen_r2g("celestial_body/venus/ppe_color_2k.jpg", "celestial_body/venus/color.png", SIZE)

        gen("tmp/textures/misc/background_starmap.png", "generated/textures/misc/background_starmap.png", 2048)
    }
}

val generateAssetsSetup by tasks.registering {
    group = "assets"
    doLast {
        if (!org.gradle.internal.os.OperatingSystem.current().isWindows)
            throw GradleException("processAssets task only works on windows.")

        // clean
        delete("${rootDir}/assets/tmp")
        delete("${rootDir}/assets/generated")
        mkdir("${assetsRoot}/generated")
    }
}

@Suppress("unused")
val generateAssets by tasks.registering {
    group = "assets"
    val genTasks = arrayOf(
        generateFonts,
        processImgs,
        generateCubemaps
    )
    dependsOn(generateAssetsSetup, *genTasks)
    generateCubemaps.get().mustRunAfter(processImgs)
    genTasks.forEach { it.get().mustRunAfter(generateAssetsSetup) }
}
// endregion

fun _cleanMergedAndDeployedResources() {
    delete("${rootDir}/assets/all")
    delete(kotlin.sourceSets["jvmMain"].resourcesDir / "assets")
    delete(kotlin.sourceSets["jsMain"].resourcesDir / "assets")
}

// region Asset Deploy
val cleanMergedAndDeployedResources by tasks.registering {
    clean.dependsOn(this)

    doLast {
        _cleanMergedAndDeployedResources()
    }
}

val deployAssets by tasks.registering {
    group = "assets"

    inputs.dir("${assetsRoot}/static/")
    inputs.dir("${assetsRoot}/generated/")
    outputs.dir("${assetsRoot}/all/")
    outputs.dir(kotlin.sourceSets["jvmMain"].resourcesDir / "assets")
    outputs.dir(kotlin.sourceSets["jsMain"].resourcesDir / "assets")

    doFirst {
        _cleanMergedAndDeployedResources()
    }

    // merge
    doLast {
        copy {
            from("${assetsRoot}/static/")
            into("${assetsRoot}/all/")
        }
        copy {
            from("${assetsRoot}/generated/")
            into("${assetsRoot}/all/")
        }
    }

    // deploy
    doLast {
        copy {
            from("${assetsRoot}/all/")
            into(kotlin.sourceSets["jvmMain"].resourcesDir / "assets")
        }
        copy {
            from("${assetsRoot}/all/")
            into(kotlin.sourceSets["jsMain"].resourcesDir / "assets")
        }
    }
}

@Suppress("unused")
val jvmProcessResources by tasks.getting(Task::class) {
    dependsOn(deployAssets)
}

@Suppress("unused")
val jsProcessResources by tasks.getting(Task::class) {
    dependsOn(deployAssets)
}
// endregion