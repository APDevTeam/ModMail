plugins {
    `java-library`
    id("com.gradleup.shadow") version "9.4.2"
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
    api(libs.com.electronwill.night.config.toml)
}

group = "io.github.apdevteam"
version = "0.7.2"
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
