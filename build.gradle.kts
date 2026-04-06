val minecraft = project.property("minecraft_version").toString()
val imageio = project.property("imageio_version").toString()

plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:$minecraft-R0.1-SNAPSHOT")
}

buildscript {
    dependencies {
        classpath("org.apache.commons:commons-compress:1.26.1")
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

// build task for each target
val buildModes = listOf("debug", "release")
val buildMode = project.findProperty("mode")?.toString() ?: "debug"

fun currentPlatform(): String {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()

    return when {
        os.contains("win") && arch.contains("64") -> "x86_64-pc-windows-msvc"
        os.contains("linux") && arch.contains("64") -> "x86_64-unknown-linux-gnu"
        os.contains("linux") && arch.contains("aarch64") -> "aarch64-unknown-linux-gnu"
        os.contains("mac") && arch.contains("x86_64") -> "x86_64-apple-darwin"
        os.contains("mac") && arch.contains("aarch64") -> "aarch64-apple-darwin"
        else -> error("Unsupported OS/arch: $os/$arch")
    }
}

val targetsToBuild = if (buildMode == "debug") {
    listOf(currentPlatform())
} else {
    listOf(
        "x86_64-pc-windows-msvc",
        "x86_64-unknown-linux-gnu",
        "aarch64-unknown-linux-gnu",
        "x86_64-apple-darwin",
        "aarch64-apple-darwin"
    )
}

buildModes.forEach { mode ->
    targetsToBuild.forEach { target ->
        val taskName = "cargoBuild_${target}_$mode"
        tasks.register<Exec>(taskName) {
            group = "build"
            workingDir = file("ffi")

            val argsList = mutableListOf("--target", target)
            if (buildMode == "release") argsList.add("--release")

            commandLine(listOf("cargo", "build") + argsList)
        }
    }
}

tasks {
    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    runServer {
        minecraftVersion(minecraft)
        jvmArgs("-Dcom.mojang.eula.agree=true")

        doFirst {
            val propsFile = file("run/server.properties");
            propsFile.parentFile.mkdirs()
            propsFile.writeText(
                """
                difficulty=peaceful
                level-type=flat
                generator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}
                """.trimIndent()
            )
        }
    }

    // copy native libraries
    register<Copy>("copyNativeLibs") {
        group = "ffi";
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        dependsOn(targetsToBuild.map { target -> "cargoBuild_${target}_$buildMode" })

        targetsToBuild.forEach { target ->
            val targetDir = layout.projectDirectory.dir("ffi/target/$target/$buildMode")
            val libName = when (target) {
                "x86_64-pc-windows-msvc" -> "ffi.dll"
                "x86_64-unknown-linux-gnu", "aarch64-unknown-linux-gnu" -> "libffi.so"
                "x86_64-apple-darwin", "aarch64-apple-darwin" -> "libffi.dylib"
                else -> error("Unsupported target: $target")
            }
            val libFile = targetDir.file(libName)

            from(libFile) {
                rename { _ -> target }
            }
        }

        into(layout.buildDirectory.dir("natives"))
    }

    named<Jar>("jar") {
        dependsOn("copyNativeLibs")

        from(layout.buildDirectory.dir("natives")) {
            into("natives")
        }
    }
}