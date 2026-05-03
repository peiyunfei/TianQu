package shijing.tianqu.mcp.tools

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.buildJsonObject
import java.io.InputStreamReader

/**
 * tianqu_get_capabilities
 * 返回天衢支持的功能清单及明确的不支持边界。
 * 供 AI 在接受指令时优先查询，若需求超出边界则直接拒绝。
 */
val getCapabilitiesTool = Tool(
    name = "tianqu_get_capabilities",
    description = "查询天衢(TianQu)框架支持的功能清单、API入口及不支持的功能边界。在尝试使用天衢实现任何功能前，必须先调用此工具验证可行性。",
    inputSchema = Tool.Input(
        properties = buildJsonObject {},
        required = emptyList()
    )
)

fun handleGetCapabilities(): CallToolResult {
    val inputStream = GetCapabilitiesToolMarker::class.java.getResourceAsStream("/capabilities.json")
    val content = if (inputStream != null) {
        InputStreamReader(inputStream, Charsets.UTF_8).readText()
    } else {
        "[]"
    }

    return CallToolResult(
        content = listOf(TextContent(content))
    )
}

private object GetCapabilitiesToolMarker
