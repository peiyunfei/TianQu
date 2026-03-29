plugins {
    alias(libs.plugins.kotlinJvm)
    `maven-publish`
}

java {
    withSourcesJar()
}

apply(from = "$rootDir/maven-publish-config.gradle.kts")

group = findProperty("TIANQU_GROUP").toString()
version = findProperty("TIANQU_PROCESSOR_VERSION").toString()

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "tianqu-router-processor"
        }
    }
}

dependencies {
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    implementation(project(":router-annotations"))
}