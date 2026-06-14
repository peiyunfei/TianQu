import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    `maven-publish`
}

apply(from = "$rootDir/maven-publish-config.gradle.kts")

group = findProperty("TIANQU_GROUP").toString()
version = findProperty("TIANQU_ANNOTATIONS_VERSION").toString()

publishing {
    publications.withType<MavenPublication>().configureEach {
        artifactId = artifactId.replace(project.name, "tianqu-${project.name}")
    }
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    iosArm64()
    iosSimulatorArm64()
    ohosArm64()
}

android {
    namespace = "shijing.tianqu.router.annotations"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}