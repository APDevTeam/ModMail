plugins {
    `java-library`
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
version = "0.6.9"
description = "ModMail"
java.toolchain.languageVersion = JavaLanguageVersion.of(21)

tasks.jar {
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
