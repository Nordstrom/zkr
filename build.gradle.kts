plugins {
    id("java")
    id("application")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.github.johnrengelman.shadow")
}

application {
    mainClass.set("zkr.Zkr")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation(libs.zookeeper)
    implementation(libs.bundles.aws.sdk)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.bundles.serialization)
    implementation(libs.bundles.logging)
    implementation(libs.micrometer.jmx)
    implementation(libs.picocli)

    implementation("com.fasterxml.jackson.core:jackson-core:2.12.7")
    implementation("com.google.guava:guava:30.0-jre")
}

testing {
    suites {
        named("test", JvmTestSuite::class) {
            useJUnitJupiter()

            dependencies {
                implementation(libs.curator.test)
                implementation(libs.kotest.assertions)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}
