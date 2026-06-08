plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    iosArm64()
    iosSimulatorArm64()
    
    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
        }
    }
}