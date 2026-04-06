import java.io.FileInputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

val minecraft = project.property("minecraft_version").toString()

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
        jvmArgs("-Dcom.mojang.eula.agree=true", "--enable-preview")
    }

    // need preview features
    withType<JavaCompile>().configureEach {
        options.compilerArgs.add("--enable-preview")
    }

    // jextract
    register<Exec>("jextract") {
        group = "ffi";
        workingDir = file(".")

        dependsOn("cargoBuild_${currentPlatform()}_${buildMode}")

        var dir = file(".jextract");
        var exe = dir.resolve("bin/jextract.bat");

        // Build 21-jextract+1-2 (2023/9/25)
        val osName = System.getProperty("os.name").lowercase()
        var url = when {
            osName.contains("windows") -> "https://download.java.net/java/early_access/jextract/21/1/openjdk-21-jextract+1-2_windows-x64_bin.tar.gz"
            osName.contains("mac") -> "https://download.java.net/java/early_access/jextract/21/1/openjdk-21-jextract+1-2_macos-x64_bin.tar.gz"
            osName.contains("linux") -> "https://download.java.net/java/early_access/jextract/21/1/openjdk-21-jextract+1-2_linux-x64_bin.tar.gz"
            else -> error("Unsupported OS: $osName")
        }

        doFirst {
            if (!exe.exists()) {
                println("jextract not found, downloading...")
                dir.mkdirs()

                // download to disk
                val archiveFile = dir.resolve("jdk21.tar.gz")
                URI.create(url).toURL().openStream().use { input ->
                    Files.copy(input, archiveFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }

                // extract from archive
                println("extracting...")

                FileInputStream(archiveFile).use { fis ->
                    GzipCompressorInputStream(fis).use { gis ->
                        TarArchiveInputStream(gis).use { tis ->
                            var entry = tis.nextEntry
                            while (entry != null) {
                                val strippedName = entry.name.substringAfter("/")
                                if (strippedName.isEmpty()) {
                                    entry = tis.nextEntry
                                    continue
                                }

                                val outFile = dir.resolve(strippedName)

                                if (entry.isDirectory) outFile.mkdirs()
                                else {
                                    outFile.parentFile?.mkdirs()
                                    Files.copy(tis, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                                }
                                entry = tis.nextEntry
                            }
                        }
                    }
                }

                // delete archive
                archiveFile.delete()
                println("got jextract!")
            }
        }

        commandLine(
            exe.absolutePath, "ffi/target/generated/mcpixel.h",   // input header
            "--output", "src/main/java",                                       // output folder
            "-t", "${project.properties["group"]}.mcpixel.ffi",                // Java package
            "--source"                                                         // generate source files
        )
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

    named("compileJava") {
        dependsOn("jextract")
    }

    named<Jar>("jar") {
        dependsOn("copyNativeLibs")

        from(layout.buildDirectory.dir("natives")) {
            into("natives")
        }
    }
}