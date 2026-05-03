package shijing.tianqu.mcp

import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered
import shijing.tianqu.mcp.tools.analyzeProjectTool
import shijing.tianqu.mcp.tools.checkUpdateTool
import shijing.tianqu.mcp.tools.getCapabilitiesTool
import shijing.tianqu.mcp.tools.getIntegrationGuideTool
import shijing.tianqu.mcp.tools.handleAnalyzeProject
import shijing.tianqu.mcp.tools.handleCheckUpdate
import shijing.tianqu.mcp.tools.handleGetCapabilities
import shijing.tianqu.mcp.tools.handleGetIntegrationGuide

/**
 * 天衢 MCP Server 入口
 *
 * 本程序通过 stdio 与 Claude Code 通信，暴露四个核心工具：
 *   - tianqu_check_update         ：检查天衢接入组件是否有新版本（每日一次缓存）
 *   - tianqu_get_capabilities     ：查询天衢支持的功能及不支持的边界
 *   - tianqu_analyze_project      ：扫描接入方工程的模块结构和接入现状
 *   - tianqu_get_integration_guide：按模块类型返回精确的接入指令与代码模板
 *
 * 启动方式：java -jar tianqu-mcp-server-all.jar
 * 注意：不要向 stdout 输出任何额外内容，MCP 协议消息走 stdin/stdout，日志请写 stderr。
 */
fun main() {
    val server = Server(
        serverInfo = Implementation(
            name = "tianqu-mcp-server",
            version = "1.0.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = false)
            )
        )
    )

    // 注册四个核心 Tool。
    // Tool 对象描述元信息（name / description / inputSchema），handler 处理实际请求。
    // description 用 ?: "" 兜底以满足 SDK 的非空要求。
    server.addTool(
        name = checkUpdateTool.name,
        description = checkUpdateTool.description ?: "",
        inputSchema = checkUpdateTool.inputSchema,
        handler = { request -> handleCheckUpdate(request.arguments) }
    )
    server.addTool(
        name = getCapabilitiesTool.name,
        description = getCapabilitiesTool.description ?: "",
        inputSchema = getCapabilitiesTool.inputSchema,
        handler = { _ -> handleGetCapabilities() }
    )
    server.addTool(
        name = analyzeProjectTool.name,
        description = analyzeProjectTool.description ?: "",
        inputSchema = analyzeProjectTool.inputSchema,
        handler = { request -> handleAnalyzeProject(request.arguments) }
    )
    server.addTool(
        name = getIntegrationGuideTool.name,
        description = getIntegrationGuideTool.description ?: "",
        inputSchema = getIntegrationGuideTool.inputSchema,
        handler = { request -> handleGetIntegrationGuide(request.arguments) }
    )

    // StdioServerTransport 需要 kotlinx.io.Source / Sink，通过扩展函数桥接 System.in / System.out
    val transport = StdioServerTransport(
        inputStream = System.`in`.asSource().buffered(),
        outputStream = System.out.asSink().buffered()
    )

    runBlocking {
        server.connect(transport)
        System.err.println("[tianqu-mcp-server] 已启动，等待 Claude Code 连接...")
        // 保持进程运行，直到 Claude Code 关闭连接
        val done = kotlinx.coroutines.CompletableDeferred<Unit>()
        server.onClose {
            done.complete(Unit)
        }
        done.await()
    }
}
