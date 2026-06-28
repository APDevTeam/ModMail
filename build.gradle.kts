plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.4.3"
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("net.dv8tion:JDA:6.4.2") {
      exclude(module="opus-java")
      exclude(module="tink")
    }
    api("com.electronwill.night-config:toml:3.9.0")
}

group = "io.github.apdevteam"
version = System.getenv("RELEASE_VERSION")?.takeIf { it.isNotBlank() }
    ?: runCatching {
        val sha = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
            .start().inputStream.bufferedReader().readLine() ?: "unknown"
        val tag = ProcessBuilder("git", "describe", "--tags", "--abbrev=0")
            .start().inputStream.bufferedReader().readLine() ?: "untagged"
        val dirty = ProcessBuilder("git", "status", "--porcelain")
            .start().inputStream.bufferedReader().readLine() != null
        if (dirty) "$tag+$sha-dirty" else "$tag+$sha"
    }.getOrElse { "unknown" }
description = "ModMail"
java.toolchain.languageVersion = JavaLanguageVersion.of(25)

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "io.github.apdevteam.ModMail",
            "Implementation-Version" to project.version,
            "Implementation-Title" to project.name
        )
    }
}

tasks.shadowJar {
    archiveBaseName.set("ModMail")
    archiveClassifier.set("")
    archiveVersion.set("")
}

tasks.processResources {
    from(rootProject.file("LICENSE.md"))
    filesMatching("*.yml") {
        expand(mapOf("projectVersion" to project.version))
    }
}
