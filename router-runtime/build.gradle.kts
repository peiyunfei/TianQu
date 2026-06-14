import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    `maven-publish`
}

apply(from = "$rootDir/maven-publish-config.gradle.kts")

group = findProperty("TIANQU_GROUP").toString()
version = findProperty("TIANQU_RUNTIME_VERSION").toString()

publishing {
    publications.withType<MavenPublication>().configureEach {
        artifactId = artifactId.replace(project.name, "tianqu-${project.name}")
    }
}

kotlin {
    androidTarget {
        publishLibraryVariants("release", "debug")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    iosArm64()
    iosSimulatorArm64()

    listOf(ohosArm64()).forEach { ohosTarget ->
        ohosTarget.binaries.sharedLib {
            baseName = "kn"
            export(libs.compose.multiplatform.export)
            linkerOpts("-lz")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(project(":router-annotations"))
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
        }

        val ohosMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.compose.ui.backhandler)
            }
        }
        sourceSets["ohosArm64Main"].dependsOn(ohosMain)
    }
}

android {
    namespace = "shijing.tianqu.router.runtime"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}
