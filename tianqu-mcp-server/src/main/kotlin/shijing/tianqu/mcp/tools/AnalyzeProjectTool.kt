package shijing.tianqu.mcp.tools

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File

/**
 * tianqu_analyze_project
 * 扫描接入方工程结构，识别各模块类型及天衢接入现状。
 */
val analyzeProjectTool = Tool(
    name = "tianqu_analyze_project",
    description = "扫描接入方 KMP/Android 工程的模块结构，识别哪些是 app 主模块、哪些是 feature 子模块，并检测各模块的 KSP 和天衢依赖配置现状。",
    inputSchema = Tool.Input(
        properties = buildJsonObject {
            put("projectRoot", buildJsonObject {
                put("type", JsonPrimitive("string"))
                put("description", JsonPrimitive("接入方工程的根目录绝对路径，例如 /Users/xxx/MyApp"))
            })
        },
        required = listOf("projectRoot")
    )
)

fun handleAnalyzeProject(args: Map<String, JsonElement>?): CallToolResult {
    val projectRoot = (args?.get("projectRoot") as? JsonPrimitive)?.content ?: ""

    val result = analyzeProject(projectRoot)

    return CallToolResult(
        content = listOf(TextContent(result))
    )
}

/**
 * 分析工程结构，返回 JSON 格式的分析结果。
 */
private fun analyzeProject(projectRoot: String): String {
    if (projectRoot.isBlank()) {
        return """{"error": "projectRoot 不能为空，请传入工程根目录的绝对路径"}"""
    }

    val rootDir = File(projectRoot)
    if (!rootDir.exists() || !rootDir.isDirectory) {
        return """{"error": "目录不存在或路径无效: $projectRoot"}"""
    }

    // 1. 读取 settings.gradle.kts 或 settings.gradle，获取模块列表
    val settingsFile = rootDir.resolve("settings.gradle.kts").takeIf { it.exists() }
        ?: rootDir.resolve("settings.gradle").takeIf { it.exists() }

    val modules = mutableListOf<ModuleInfo>()

    if (settingsFile != null) {
        val content = settingsFile.readText()
        // 匹配 include(":module-name") 或 include("module-name")
        val includeRegex = Regex("""include\(["'](:?[\w\-]+)["']\)""")
        includeRegex.findAll(content).forEach { match ->
            val modulePath = match.groupValues[1].trimStart(':')
            if (modulePath.isNotBlank()) {
                val moduleDir = rootDir.resolve(modulePath)
                val info = inspectModule(rootDir, modulePath, moduleDir)
                modules.add(info)
            }
        }
    } else {
        // 没有 settings 文件时，扫描一级子目录作为候选模块
        rootDir.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") && it.name != "build" }
            ?.forEach { dir ->
                if (dir.resolve("build.gradle.kts").exists() || dir.resolve("build.gradle").exists()) {
                    val info = inspectModule(rootDir, dir.name, dir)
                    modules.add(info)
                }
            }
    }

    // 2. 检查根目录 libs.versions.toml 是否已有天衢依赖
    val tomlFile = rootDir.resolve("gradle/libs.versions.toml")
    val tomlContent = if (tomlFile.exists()) tomlFile.readText() else ""
    val hasTianquInToml = tomlContent.contains("tianqu", ignoreCase = true)

    // 3. 组装 JSON 输出
    val modulesJson = modules.joinToString(",\n    ") { it.toJson() }
    return """
{
  "projectRoot": "$projectRoot",
  "hasVersionCatalog": ${tomlFile.exists()},
  "tianquInVersionCatalog": $hasTianquInToml,
  "modules": [
    $modulesJson
  ]
}
    """.trimIndent()
}

/**
 * 检查单个模块的 build.gradle.kts，识别模块类型及天衢接入现状。
 */
private fun inspectModule(rootDir: File, moduleName: String, moduleDir: File): ModuleInfo {
    val buildFile = moduleDir.resolve("build.gradle.kts").takeIf { it.exists() }
        ?: moduleDir.resolve("build.gradle").takeIf { it.exists() }

    if (buildFile == null) {
        return ModuleInfo(moduleName, ModuleType.UNKNOWN, false, false, false)
    }

    val content = buildFile.readText()

    // 判断模块类型：包含 androidApplication 或 isApp=true 则为 app 主模块
    val isApp = content.contains("androidApplication") ||
            content.contains("""tianqu.isApp""") ||
            content.contains("""arg("tianqu.isApp"""")

    // 是否已配置 KSP
    val hasKsp = content.contains("ksp") &&
            (content.contains("kspCommonMainMetadata") || content.contains("kspCommonMainKotlinMetadata"))

    // 是否已有天衢依赖
    val hasTianqu = content.contains("tianqu", ignoreCase = true)

    val type = when {
        isApp -> ModuleType.APP
        moduleName.startsWith("feature") || moduleName.startsWith("module") -> ModuleType.FEATURE
        moduleName == "composeApp" -> ModuleType.APP
        else -> ModuleType.FEATURE
    }

    return ModuleInfo(moduleName, type, hasKsp, hasTianqu, isApp)
}

private enum class ModuleType { APP, FEATURE, UNKNOWN }

private data class ModuleInfo(
    val name: String,
    val type: ModuleType,
    val hasKsp: Boolean,
    val hasTianqu: Boolean,
    val isApp: Boolean
) {
    fun toJson() = """
{
      "name": "$name",
      "type": "${type.name.lowercase()}",
      "hasKspConfigured": $hasKsp,
      "hasTianquDependency": $hasTianqu,
      "integrationStatus": "${if (hasTianqu && hasKsp) "已接入" else if (hasTianqu || hasKsp) "部分配置" else "未接入"}"
    }""".trimIndent()
}
