import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlugin.Companion.kotlinNodeJsEnvSpec
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    kotlin("multiplatform") version "2.1.20"
}

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
}

kotlin {
    // kotlin multiplatform (jvm + js) setup:
    jvm { }
    jvmToolchain(21)

    js {
        binaries.executable()
        browser {
            @OptIn(ExperimentalDistributionDsl::class)
            distribution {
                outputDirectory.set(File("${rootDir}/dist/js"))
            }
            commonWebpackConfig {
                outputFileName = "index.js"
                //mode = KotlinWebpackConfig.Mode.PRODUCTION
                mode = KotlinWebpackConfig.Mode.DEVELOPMENT
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

        val jsMain by getting {
            dependencies {
                implementation(devNpm("uglify-js", "3.19.3"))
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

@Suppress("unused")
val clean by tasks.getting(Task::class) {
    doLast {
        delete("${rootDir}/dist")
        delete(fileTree("${rootDir}/wechat/miniprogram/index/src") {
            exclude("README.md")
        })

        delete("${rootDir}/assets/all")
        delete("${rootDir}/src/commonMain/resources")
        delete("${rootDir}/src/jsMain/assets")
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

@Suppress("unused")
val jsWeChatMinifiedBuild by tasks.registering {
    group = "wechat"
    dependsOn(jsWeChatBuild)
    doLast {
        exec {
            commandLine(
                kotlinNodeJsEnvSpec.executable.get(),
                "${rootDir}/build/js/node_modules/uglify-js/bin/uglifyjs",
                "${rootDir}/wechat/miniprogram/index/src/index.js",
                "-o", "${rootDir}/wechat/miniprogram/index/src/index.js",
                "--source-map", "url='${rootDir}/wechat/miniprogram/index/src/index.js.map'",
                "--compress"
            )
        }
    }
}

val assetsRoot = "${rootDir}/assets"

@Suppress("unused")
val generateAssets by tasks.registering {
    group = "assets"
    doFirst {
        if (!org.gradle.internal.os.OperatingSystem.current().isWindows)
            throw GradleException("processAssets task only works on windows.")
    }

    // clean
    doFirst {
        delete("${rootDir}/assets/generated")
        mkdir("${assetsRoot}/generated")
    }

    // fonts
    doLast {
        mkdir("${assetsRoot}/generated/fonts")

        exec {
            workingDir(assetsRoot)
            executable("${assetsRoot}/msdf-atlas-gen.exe")
            args(
                "-varfont", "NotoSans-VariableFont_wdth,wght.ttf?wght=400",
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

val deployAssets by tasks.registering {
    group = "assets"
    doFirst {
        delete("${rootDir}/assets/all")
        delete("${rootDir}/src/commonMain/resources")
        delete("${rootDir}/src/jsMain/assets")
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
            into("${rootDir}/src/commonMain/resources/")
        }
        copy {
            from("${assetsRoot}/all/")
            into("${rootDir}/src/jsMain/resources/assets/")
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