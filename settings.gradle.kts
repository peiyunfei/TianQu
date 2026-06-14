rootProject.name = "TianQu"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        // 设置全局 Kotlin 版本默认值，子模块省略 version 时使用此版本
        kotlin("jvm") version "2.2.21-0.3.0"
        kotlin("plugin.serialization") version "2.2.21-0.3.0"
    }
}

dependencyResolutionManagement {
    repositories {
        // 鸿蒙 fork 版本（带 -0.3.0 后缀）只存在于此仓库，MavenCentral 无这些版本，不会产生冲突
        maven("https://maven.eazytec-cloud.com/nexus/repository/maven-public/")
        mavenLocal()
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(":composeApp")
include(":router-annotations")
include(":router-processor")
include(":router-runtime")
include(":feature-b")
// 天衢 MCP Server 模块：为 Claude Code 提供天衢能力查询、工程分析、接入指南等工具
include(":tianqu-mcp-server")