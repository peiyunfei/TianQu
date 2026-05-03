// tianqu-mcp-server 模块的构建脚本
// 这是一个纯 Kotlin/JVM 的独立程序，通过 stdio 与 Claude Code 通信
// 打包为 Fat JAR 后，接入方通过 java -jar 启动，无需任何额外环境

plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    application
    // Shadow 插件：将所有依赖打包进一个独立的 Fat JAR
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "shijing.tianqu"
version = "1.0.0"

kotlin {
    jvmToolchain(17)
}

application {
    // MCP Server 的程序入口
    mainClass.set("shijing.tianqu.mcp.MainKt")
}

dependencies {
    // Kotlin MCP SDK：提供 MCP Server / Tool 注册 / stdio 传输层等核心能力
    implementation("io.modelcontextprotocol:kotlin-sdk:0.4.0")
    // 协程：MCP SDK 基于协程运行
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    // JSON 序列化：用于解析 capabilities.json 与构造工具响应
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    // kotlinx-io：StdioServerTransport 需要 kotlinx.io.Source / Sink，用于桥接 System.in / System.out
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.5.4")
}

// Shadow 打包配置：输出文件名为 tianqu-mcp-server-all.jar
tasks.shadowJar {
    archiveBaseName.set("tianqu-mcp-server")
    archiveClassifier.set("all")
    archiveVersion.set("")
    // 合并 META-INF/services 文件，避免 ServiceLoader 失效
    mergeServiceFiles()
}

// 默认 build 任务依赖 shadowJar，确保每次构建都生成 Fat JAR
tasks.build {
    dependsOn(tasks.shadowJar)
}
