import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    listOf(ohosArm64()).forEach { ohosTarget ->
        ohosTarget.binaries.sharedLib {
            baseName = "kn"
            if (buildType == NativeBuildType.RELEASE) {
                optimized = false
            }
            export(libs.compose.multiplatform.export)
            linkerOpts("-lz")
            linkerOpts(
                "-lnative_drawing",
                "-limage_source",
                "-lpixelmap",
                "-lpixelmap_ndk.z",
                "-lnative_window",
                "-lace_napi.z",
                "-lhilog_ndk.z",
                "-lhitrace_ndk.z",
                "-luv",
                "-lunwind",
                "-licu",
            )
        }
        ohosTarget.compilations.getByName("main") {
            val resource by cinterops.creating {
                defFile(file("src/ohosMain/cinterop/resource.def"))
                includeDirs(file("src/ohosMain/cinterop/include"))
            }
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.material)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.serialization.json)

            // 路由框架依赖
            implementation(project(":router-annotations"))
            implementation(project(":router-runtime"))
            implementation(project(":feature-b"))
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        val ohosMain by creating {
            dependsOn(commonMain.get())
        }
        ohosMain.dependencies {
            api(libs.compose.multiplatform.export)
        }
        sourceSets["ohosArm64Main"].dependsOn(ohosMain)
    }
}

android {
    namespace = "shijing.tianqu"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "shijing.tianqu"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

ksp {
    arg("tianqu.moduleName", "ComposeApp")
    arg("tianqu.isApp", "true")
}

dependencies {
    debugImplementation(compose.uiTooling)

    // KSP 处理器 — 仅使用 commonMainMetadata 方式
    add("kspCommonMainMetadata", project(":router-processor"))
}

// 把 KSP 生成的 commonMain 代码目录加到 sourceSets 里
kotlin.sourceSets.commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}

// 确保各平台编译任务依赖 KSP commonMain 元数据生成任务
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

// 发布 KMP 产物到鸿蒙 DevEco 工程目录
val harmonyAppDir: File = run {
    val cliPath = project.findProperty("harmonyAppPath") as String?
    if (cliPath.isNullOrBlank()) rootProject.file("harmonyApp") else file(cliPath)
}

fun String.capitalizeFirst(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

arrayOf("debug", "release").forEach { type ->
    tasks.register<Copy>("publish${type.capitalizeFirst()}BinariesToHarmonyApp") {
        group = "harmony"
        dependsOn(
            "link${type.capitalizeFirst()}SharedOhosArm64",
        )
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        into(harmonyAppDir)
        from("build/bin/ohosArm64/${type}Shared/libkn_api.h") {
            into("entry/src/main/cpp/include/arm64-v8a/")
        }
        from("build/bin/ohosArm64/${type}Shared/libkn.so") {
            into("entry/libs/arm64-v8a/")
        }
        val resourcesPkg = "${rootProject.name.lowercase()}.${project.name.lowercase()}.generated.resources"
        from("src/commonMain/composeResources") {
            into("entry/src/main/resources/rawfile/composeResources/$resourcesPkg/")
        }
    }
}