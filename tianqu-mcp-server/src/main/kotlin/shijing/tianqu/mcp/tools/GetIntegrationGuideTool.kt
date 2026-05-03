package shijing.tianqu.mcp.tools

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.InputStreamReader

/**
 * tianqu_get_integration_guide
 * 根据模块类型（app 或 feature）返回精确的天衢接入指令。
 * 指南模板从 resources/integration-guides/ 目录读取，版本号从 tianqu-version.json 动态注入。
 */
val getIntegrationGuideTool = Tool(
    name = "tianqu_get_integration_guide",
    description = "根据模块类型（app 或 feature）返回天衢框架的完整接入指南，包含 Gradle 配置模板、KSP 配置、sourceSet 配置、任务依赖配置及代码初始化模板。",
    inputSchema = Tool.Input(
        properties = buildJsonObject {
            put("moduleType", buildJsonObject {
                put("type", "string")
                put("description", "模块类型：app（主工程/壳模块）或 feature（业务子模块）")
                put("enum", buildJsonArray {
                    add(JsonPrimitive("app"))
                    add(JsonPrimitive("feature"))
                })
            })
            put("moduleName", buildJsonObject {
                put("type", "string")
                put("description", "模块名称，例如 composeApp 或 feature-login")
            })
        },
        required = listOf("moduleType", "moduleName")
    )
)

fun handleGetIntegrationGuide(args: Map<String, JsonElement>?): CallToolResult {
    val moduleType = (args?.get("moduleType") as? JsonPrimitive)?.content ?: ""
    val moduleName = (args?.get("moduleName") as? JsonPrimitive)?.content ?: "module"

    val guide = when (moduleType.lowercase()) {
        "app"     -> buildGuideFromResource("integration-guides/app.md", moduleName)
        "feature" -> buildGuideFromResource("integration-guides/feature.md", moduleName)
        else      -> "moduleType 参数无效，只支持 \"app\" 或 \"feature\""
    }

    return CallToolResult(
        content = listOf(TextContent(guide))
    )
}

// ─────────────────────────────────────────────────────────────────
// 从 resources 读取模板并替换占位符
// ─────────────────────────────────────────────────────────────────

private fun buildGuideFromResource(resourcePath: String, moduleName: String): String {
    // 1. 读取版本元数据
    val versionJson = loadVersionJson()
    val frameworkVersion  = versionJson?.get("frameworkVersion")?.jsonPrimitive?.content ?: "1.0.5"
    val kspVersion        = versionJson?.get("kspVersion")?.jsonPrimitive?.content       ?: "2.1.10-1.0.31"
    val minJavaVersion    = versionJson?.get("minJavaVersion")?.jsonPrimitive?.content   ?: "17"

    // 2. 从模块名派生常用变体
    val moduleNameUnderscore = moduleName.replace("-", "_")   // feature-login → feature_login
    val moduleNamePascal     = moduleName                      // feature-login → FeatureLogin
        .split("-")
        .joinToString("") { it.replaceFirstChar(Char::uppercase) }

    // 3. 读取模板文件
    val stream = IntegrationGuideMarker::class.java.getResourceAsStream("/$resourcePath")
        ?: return "内部错误：找不到接入指南模板文件 $resourcePath"
    var template = InputStreamReader(stream, Charsets.UTF_8).readText()

    // 4. 替换占位符
    val replacements = mapOf(
        "{{moduleName}}"          to moduleName,
        "{{moduleNameUnderscore}}" to moduleNameUnderscore,
        "{{moduleNamePascal}}"    to moduleNamePascal,
        "{{frameworkVersion}}"    to frameworkVersion,
        "{{kspVersion}}"          to kspVersion,
        "{{minJavaVersion}}"      to minJavaVersion,
    )
    replacements.forEach { (placeholder, value) ->
        template = template.replace(placeholder, value)
    }

    return template
}

/** 读取本地 tianqu-version.json，返回 JsonObject 或 null */
internal fun loadVersionJson(): JsonObject? = runCatching {
    val stream = IntegrationGuideMarker::class.java.getResourceAsStream("/tianqu-version.json")
        ?: return null
    val text = InputStreamReader(stream, Charsets.UTF_8).readText()
    kotlinx.serialization.json.Json.parseToJsonElement(text).jsonObject
}.getOrNull()

private object IntegrationGuideMarker
