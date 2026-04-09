// properties
val minecraft = project.findProperty("minecraft").toString()
val cloud = project.findProperty("cloud").toString()
val bstats = project.findProperty("bstats").toString()

// constants
val validProfiles = listOf("debug", "release");
val validTargets = listOf(
    "x86_64-pc-windows-msvc",
    "x86_64-unknown-linux-gnu",
    "aarch64-unknown-linux-gnu",
    "x86_64-apple-darwin",
    "aarch64-apple-darwin"
)

// config
fun computeTargets(): List<String> {
    val target = project.findProperty("target")?.toString()
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()

    if (target == null) {
        return listOf(
            when {
                os.contains("win") && arch.contains("64") -> "x86_64-pc-windows-msvc"
                os.contains("linux") && arch.contains("64") -> "x86_64-unknown-linux-gnu"
                os.contains("linux") && arch.contains("aarch64") -> "aarch64-unknown-linux-gnu"
                os.contains("mac") && arch.contains("x86_64") -> "x86_64-apple-darwin"
                os.contains("mac") && arch.contains("aarch64") -> "aarch64-apple-darwin"
                else -> error("Unsupported OS/arch: $os/$arch")
            }
        )
    } else if (!validTargets.contains(target)) {
        error("Target $target is not supported")
    }

    return listOf(target)
}

val isCi = project.hasProperty("ci")

val profile = project.findProperty("mode")?.toString() ?: "debug";
if (!validProfiles.contains(profile)) {
    error("Profile $profile is not supported")
}

val targets = computeTargets()

// build.gradle
plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.4.1"
    id("xyz.jpenilla.run-paper") version "3.0.2"
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21)) // jdk21
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // spigot
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:$minecraft-R0.1-SNAPSHOT") // spigot
    implementation("org.incendo:cloud-paper:$cloud")                // cloud
    implementation("org.bstats:bstats-bukkit:$bstats")              // bstats
}

tasks {
    // === resources ===
    processResources {
        // fill properties into plugin.yml
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    // rust
    targets.forEach { target ->
        register<Exec>("cargo_$target") {
            group = "ffi"
            workingDir = file("ffi")

            val args = mutableListOf("--target", target)
            if (profile == "release") args.add("--release")

            commandLine(listOf("cargo", "build") + args)
        }
    }

    register<Copy>("copyNatives") {
        group = "ffi";
        duplicatesStrategy = DuplicatesStrategy.INCLUDE

        if (!isCi) {
            dependsOn(targets.map { target -> "cargo_$target" })
        }

        targets.forEach { target ->
            val dir = layout.projectDirectory.dir("ffi/target/$target/$profile");
            var lib = dir.file(
                when (target) {
                    "x86_64-pc-windows-msvc" -> "ffi.dll"
                    "x86_64-unknown-linux-gnu", "aarch64-unknown-linux-gnu" -> "libffi.so"
                    "x86_64-apple-darwin", "aarch64-apple-darwin" -> "libffi.dylib"
                    else -> error("Unsupported target: $target")
                }
            )

            from(lib) {
                rename { _ -> target }
            }
        }

        into(layout.buildDirectory.dir("natives"))
    }

    // === shadow ===
    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        // include natives
        dependsOn("copyNatives")

        from(layout.buildDirectory.dir("natives")) {
            into("natives")
        }

        // include bstats
        configurations = project.configurations.runtimeClasspath.map { setOf(it) }

        dependencies {
            exclude { it.moduleGroup != "org.bstats" }
        }

        relocate("org.bstats", "${project.group}.bstats")
    }

    // === dev ===
    runServer {
        minecraftVersion(minecraft)
        jvmArgs("-Dcom.mojang.eula.agree=true") // agree to the EULA

        // copy server.properties
        doFirst {
            file("server.properties")
                .copyTo(file("run/server.properties"), overwrite = true)
        }
    }

    // === cleanup ===
    register<Exec>("cleanCargo") {
        group = "ffi"
        workingDir = file("ffi")

        commandLine(listOf("cargo", "clean"))
    }

    clean {
        dependsOn("cleanCargo")
    }
}