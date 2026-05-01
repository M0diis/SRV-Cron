import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "9.2.2"
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

group = "me.m0dii"
version = "2.11.0"

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

    maven {
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }

    maven {
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }

    maven {
        url = uri("https://repo.extendedclip.com/releases/")
    }

    maven {
        url = uri("https://ci.ender.zone/plugin/repository/everything/")
    }

    maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }

    maven {
        url = uri("https://oss.sonatype.org/content/repositories/central")
    }
}

dependencies {
    implementation("org.bstats:bstats-bukkit:3.0.0")

    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")

    compileOnly("net.md-5:bungeecord-api:1.21-R0.1")

    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
}

