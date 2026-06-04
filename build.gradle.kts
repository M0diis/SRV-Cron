import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "9.2.2"
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

group = "me.m0dii"
version = "2.12.1"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.named<ShadowJar>("shadowJar") {
    relocate("org.bstats", "me.m0dii.srvcron")
    archiveFileName.set("M0-SRVCron-${project.version}.jar")
}

tasks.processResources {
    filesMatching("**/*.yml") {
        expand("version" to project.version)
    }
}

repositories {
    mavenLocal()
    mavenCentral()

    listOf(
        "https://repo.papermc.io/repository/maven-public/",
        "https://repo.extendedclip.com/content/repositories/placeholderapi/",
        "https://repo.extendedclip.com/releases/",
        "https://ci.ender.zone/plugin/repository/everything/",
        "https://oss.sonatype.org/content/repositories/snapshots",
        "https://oss.sonatype.org/content/repositories/central"
    ).forEach { repoUrl ->
        maven { url = uri(repoUrl) }
    }
}

dependencies {
    implementation("org.bstats:bstats-bukkit:3.0.0")

    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")

    compileOnly("net.md-5:bungeecord-api:1.21-R0.1")

    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

