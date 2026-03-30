apply(plugin = "signing")

val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    val localProps = java.util.Properties()
    localProps.load(java.io.InputStreamReader(java.io.FileInputStream(localPropsFile), "UTF-8"))
    localProps.forEach { key, value ->
        project.extensions.extraProperties.set(key.toString(), value)
    }
}

val projectUrl = "https://github.com/peiyunfei/TianQu"

// Create empty Javadoc JAR to satisfy Maven Central requirements
val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

configure<PublishingExtension> {
    publications.withType<MavenPublication>().configureEach {
        // Add javadoc jar to all publications
        artifact(javadocJar)

        pom {
            name.set(project.name)
            description.set("天衢 是一个专为 Kotlin Multiplatform (KMP) 打造的纯 Kotlin + 协程驱动的现代路由框架，支持 Android、iOS 以及桌面端。它彻底摆脱了传统 Android 路由框架对 JVM ASM 字节码插桩的依赖与深层嵌套的回调地狱，全面拥抱挂起函数，为您提供无侵入、强解耦、类型安全以及功能极其丰富的跨模块导航与服务发现解决方案。")
            url.set(projectUrl)

            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("zhongte")
                    name.set("zhongte")
                }
            }
            scm {
                connection.set("scm:git:git://github.com/peiyunfei/TianQu.git")
                developerConnection.set("scm:git:ssh://git@github.com:peiyunfei/TianQu.git")
                url.set(projectUrl)
            }
        }
    }

    repositories {
        maven {
            name = "CentralPortalBundle"
            // 新版 Central Portal 不支持直接通过传统 Maven 部署网关 PUT
            // 我们将其发布到根目录下的 build/central-bundle 文件夹，稍后打成 ZIP 上传
            url = uri(rootProject.layout.buildDirectory.dir("central-bundle"))
        }
    }
}

configure<org.gradle.plugins.signing.SigningExtension> {
    val signingKeyId = project.findProperty("signing.keyId")?.toString() ?: System.getenv("SIGNING_KEY_ID")
    val signingPassword = project.findProperty("signing.password")?.toString() ?: System.getenv("SIGNING_PASSWORD")
    val signingKey = project.findProperty("signing.key")?.toString() ?: System.getenv("SIGNING_KEY")
    val signingKeyRingFile = project.findProperty("signing.secretKeyRingFile")?.toString() ?: System.getenv("SIGNING_SECRET_KEY_RING_FILE")

    if (signingKeyId != null && signingPassword != null) {
        if (signingKey != null) {
            useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        } else if (signingKeyRingFile != null) {
            val keyFile = file(signingKeyRingFile)
            if (keyFile.exists()) {
                useInMemoryPgpKeys(signingKeyId, keyFile.readText(), signingPassword)
            }
        }
        sign(extensions.getByType<PublishingExtension>().publications)
    }
}

// No workaround needed
tasks.withType<PublishToMavenRepository>().configureEach {
    dependsOn(tasks.withType<Sign>())
}