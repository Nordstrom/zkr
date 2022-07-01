pluginManagement {
    repositories {
        maven {
            url = uri("https://plugins.gradle.org/m2/")
        }
    }
    plugins {
        id("org.jetbrains.kotlin.jvm") version "1.6.10"
        id("org.jetbrains.kotlin.plugin.serialization") version "1.6.10"
        id("com.github.johnrengelman.shadow") version "7.1.2"
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            version("kotlinx-coroutines", "1.6.3")
            version("kotlinx-serialization", "1.3.3")
            version("logback", "1.2.11")

            library("aws-s3", "software.amazon.awssdk", "s3").version("2.17.223")
            library("curator-test", "org.apache.curator", "curator-test").version("5.2.1")
            library("kotest-assertions", "io.kotest", "kotest-assertions-core-jvm").version("5.3.0")
            library("kotlinx-coroutines", "org.jetbrains.kotlinx", "kotlinx-coroutines-core").versionRef("kotlinx-coroutines")
            library("kotlinx-coroutines-test", "org.jetbrains.kotlinx", "kotlinx-coroutines-test").versionRef("kotlinx-coroutines")
            library("kotlinx-serialization", "org.jetbrains.kotlinx", "kotlinx-serialization-core").versionRef("kotlinx-serialization")
            library("kotlinx-serialization-json", "org.jetbrains.kotlinx", "kotlinx-serialization-json-jvm").versionRef("kotlinx-serialization")
            library("micrometer-jmx", "io.micrometer", "micrometer-registry-jmx").version("1.9.1")
            library("picocli", "info.picocli", "picocli").version("4.6.3")
            library("zookeeper", "org.apache.zookeeper", "zookeeper").version("3.5.10")
            library("slf4j-api", "org.slf4j", "slf4j-api").version("1.7.36")
            library("logback-classic", "ch.qos.logback", "logback-classic").versionRef("logback")
            library("logback-core", "ch.qos.logback", "logback-core").versionRef("logback")

            bundle("aws-sdk", listOf("aws-s3"))
            bundle("serialization", listOf("kotlinx-serialization", "kotlinx-serialization-json"))
            bundle("logging", listOf("logback-classic", "logback-core", "slf4j-api"))
        }
    }
}

rootProject.name = "zkr"
