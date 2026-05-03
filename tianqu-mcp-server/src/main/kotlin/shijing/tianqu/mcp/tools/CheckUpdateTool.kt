package shijing.tianqu.mcp.tools

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.TextContent
import io.modelcontextprotocol.kotlin.sdk.Tool
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.File
import java.io.InputStreamReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private const val REMOTE_VERSION_URL =
    "https://gitee.com/zhongte/TianQu/raw/master/version.json"

private const val UPGRADE_COMMAND =
    "curl -fsSL https://gitee.com/zhongte/TianQu/raw/master/install-claude.sh | bash"

private const val CACHE_TTL_HOURS = 24L

private val DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

/**
 * tianqu_check_update
 *
 * 检查天衢 Claude Code 接入组件（MCP Server / Skill / Capabilities）是否有新版本。
 *
 * 策略：
 * - 首次或距上次检查超过 24 小时 → 请求远端 Gitee Raw version.json，结果写入本地缓存。
 * - 24 小时内 → 直接返回缓存结果，不发起网络请求。
 * - 网络失败 → 降级返回本地版本信息，声明无法访问远端，不中断后续流程。
 *
 * 缓存位置：{projectRoot}/.claude/tianqu/last-update-check.json
 * 若 projectRoot 为空，缓存文件写入系统临时目录。
 */
val checkUpdateTool = Tool(
    name = "tianqu_check_update",
    description = """
        检查天衢 Claude Code 接入组件是否有新版本。
        每次使用 /tianqu 技能时应优先调用此工具。
        若发现新版本，工具会在结果中给出升级命令，由 Claude 询问用户是否立即执行。
        每 24 小时最多访问一次远端，其余时间使用本地缓存，不影响使用速度。
    """.trimIndent(),
    inputSchema = Tool.Input(
        properties = buildJsonObject {
            put("projectRoot", buildJsonObject {
                put("type", JsonPrimitive("string"))
                put("description", JsonPrimitive(
                    "接入方项目的根目录绝对路径，用于存储每日检查缓存文件。" +
                    "若不传则写入系统临时目录（跨项目共享同一缓存）。"
                ))
            })
            put("force", buildJsonObject {
                put("type", JsonPrimitive("boolean"))
                put("description", JsonPrimitive("如果为 true，则忽略本地的 24 小时缓存记录，强制访问远端。"))
            })
        },
        required = emptyList()
    )
)

fun handleCheckUpdate(args: Map<String, JsonElement>?): CallToolResult {
    val projectRoot = (args?.get("projectRoot") as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
    val force       = (args?.get("force") as? JsonPrimitive)?.content?.toBoolean() ?: false
    val cacheFile   = resolveCacheFile(projectRoot)

    // ── 1. 尝试读取缓存 ──────────────────────────────────────────
    if (!force) {
        val cached = readCache(cacheFile)
        if (cached != null) {
            return CallToolResult(content = listOf(TextContent(cached)))
        }
    }

    // ── 2. 读取本地版本 ──────────────────────────────────────────
    val localVersion = loadVersionJson()
    val localServerVersion = localVersion?.get("serverVersion")?.jsonPrimitive?.content ?: "unknown"

    // ── 3. 请求远端版本 ──────────────────────────────────────────
    val (remoteJson, networkError) = fetchRemoteVersion()

    if (remoteJson == null) {
        // 网络失败，返回本地版本信息，不写缓存（下次仍会重试）
        val result = buildResultJson(
            localVersion  = localServerVersion,
            remoteVersion = null,
            hasUpdate     = false,
            source        = "local-fallback",
            networkError  = networkError
        )
        return CallToolResult(content = listOf(TextContent(result)))
    }

    val remoteServerVersion = remoteJson.get("serverVersion")?.jsonPrimitive?.content ?: "unknown"
    val hasUpdate           = localServerVersion != "unknown"
                           && remoteServerVersion != "unknown"
                           && localServerVersion != remoteServerVersion

    val checkedAt = LocalDateTime.now().format(DATETIME_FMT)

    // ── 4. 写入缓存 ──────────────────────────────────────────────
    writeCache(cacheFile, remoteServerVersion, hasUpdate, checkedAt)

    // ── 5. 构造返回结果 ──────────────────────────────────────────
    val result = buildResultJson(
        localVersion  = localServerVersion,
        remoteVersion = remoteServerVersion,
        hasUpdate     = hasUpdate,
        source        = if (force) "remote-forced" else "remote",
        remoteExtra   = remoteJson,
        checkedAt     = checkedAt
    )
    return CallToolResult(content = listOf(TextContent(result)))
}

// ─────────────────────────────────────────────────────────────────
// 缓存读写
// ─────────────────────────────────────────────────────────────────

private fun resolveCacheFile(projectRoot: String?): File {
    val dir = if (projectRoot != null) {
        File(projectRoot, ".claude/tianqu")
    } else {
        File(System.getProperty("java.io.tmpdir"), ".tianqu-mcp-cache")
    }
    dir.mkdirs()
    return File(dir, "last-update-check.json")
}

/**
 * 读取缓存。若缓存存在且未过期（< 24h），返回格式化 JSON 字符串；否则返回 null。
 */
private fun readCache(cacheFile: File): String? {
    if (!cacheFile.exists()) return null
    return runCatching {
        val text = cacheFile.readText()
        val json = Json.parseToJsonElement(text).jsonObject
        val checkedAt = json["checkedAt"]?.jsonPrimitive?.content ?: return null
        val cacheTime = LocalDateTime.parse(checkedAt, DATETIME_FMT)
        if (Duration.between(cacheTime, LocalDateTime.now()).toHours() < CACHE_TTL_HOURS) {
            // 缓存有效，追加 source 标记后返回
            val latestVersion = json["latestVersion"]?.jsonPrimitive?.content ?: "unknown"
            val hasUpdate     = json["hasUpdate"]?.jsonPrimitive?.content?.toBoolean() ?: false
            buildResultJson(
                localVersion  = json["localVersion"]?.jsonPrimitive?.content ?: "unknown",
                remoteVersion = latestVersion,
                hasUpdate     = hasUpdate,
                source        = "cache",
                checkedAt     = checkedAt
            )
        } else null
    }.getOrNull()
}

private fun writeCache(cacheFile: File, latestVersion: String, hasUpdate: Boolean, checkedAt: String) {
    runCatching {
        // 同时记录 localVersion，便于缓存命中时一并返回
        val localVersion = loadVersionJson()?.get("serverVersion")?.jsonPrimitive?.content ?: "unknown"
        cacheFile.writeText(
            """{"checkedAt":"$checkedAt","latestVersion":"$latestVersion","hasUpdate":$hasUpdate,"localVersion":"$localVersion"}"""
        )
    }
}

// ─────────────────────────────────────────────────────────────────
// 远端请求
// ─────────────────────────────────────────────────────────────────

private fun fetchRemoteVersion(): Pair<JsonObject?, String?> {
    return runCatching {
        val client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(REMOTE_VERSION_URL))
            .timeout(Duration.ofSeconds(8))
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() == 200) {
            val json = Json.parseToJsonElement(response.body()).jsonObject
            Pair(json, null)
        } else {
            Pair(null, "HTTP ${response.statusCode()}")
        }
    }.getOrElse { e -> Pair(null, e.message ?: "网络异常") }
}

// ─────────────────────────────────────────────────────────────────
// 结果构造
// ─────────────────────────────────────────────────────────────────

private fun buildResultJson(
    localVersion:  String,
    remoteVersion: String?,
    hasUpdate:     Boolean,
    source:        String,
    networkError:  String?  = null,
    remoteExtra:   JsonObject? = null,
    checkedAt:     String   = LocalDateTime.now().format(DATETIME_FMT)
): String {
    // 从本地资源补齐 current 详情
    val localJson = loadVersionJson()
    val frameworkVersion = localJson?.get("frameworkVersion")?.jsonPrimitive?.content ?: "unknown"
    val skillVersion     = localJson?.get("skillVersion")?.jsonPrimitive?.content     ?: "unknown"

    val remoteFramework = remoteExtra?.get("frameworkVersion")?.jsonPrimitive?.content
    val releaseDate     = remoteExtra?.get("releaseDate")?.jsonPrimitive?.content
    val updateMessage   = remoteExtra?.get("updateMessage")?.jsonPrimitive?.content?.takeIf { it.isNotEmpty() }

    val summary = when {
        networkError != null ->
            "⚠️ 无法连接远端（$networkError），将基于本地版本（$localVersion）继续分析。"
        source == "cache" && !hasUpdate ->
            "✅ 天衢接入组件已是最新版本（$localVersion），缓存有效，无需重新检查。"
        source == "cache" && hasUpdate ->
            "🔔 检测到新版本（缓存：当前 $localVersion → 最新 $remoteVersion）。"
        !hasUpdate ->
            "✅ 天衢接入组件已是最新版本（$localVersion）。"
        else ->
            "🔔 检测到新版本！当前 MCP Server 版本 $localVersion，远端最新版本 $remoteVersion。${if (updateMessage != null) "\n📢 $updateMessage" else ""}"
    }

    // 使用 kotlinx.serialization 原生构造完整 JSON 结果
    val rootObj = buildJsonObject {
        put("hasUpdate", JsonPrimitive(hasUpdate))
        put("source", JsonPrimitive(source))
        put("checkedAt", JsonPrimitive(checkedAt))
        put("summary", JsonPrimitive(summary))

        put("current", buildJsonObject {
            put("serverVersion", JsonPrimitive(localVersion))
            put("frameworkVersion", JsonPrimitive(frameworkVersion))
            put("skillVersion", JsonPrimitive(skillVersion))
        })

        if (remoteVersion != null) {
            put("latest", buildJsonObject {
                put("serverVersion", JsonPrimitive(remoteVersion))
                if (remoteFramework != null) put("frameworkVersion", JsonPrimitive(remoteFramework))
                if (releaseDate != null) put("releaseDate", JsonPrimitive(releaseDate))
            })
        }

        if (hasUpdate) {
            put("upgradeCommand", JsonPrimitive(UPGRADE_COMMAND))
            put("upgradeNote", JsonPrimitive("升级后 MCP Server 进程和 Skill 文件均会更新，完成后必须重启 Claude Code 才能加载新版本。"))
        }

        if (networkError != null) {
            put("networkError", JsonPrimitive(networkError))
        }
    }

    // 使用带缩进的 Json 配置格式化输出
    val format = Json { prettyPrint = true }
    return format.encodeToString(JsonElement.serializer(), rootObj)
}

private object CheckUpdateToolMarker
