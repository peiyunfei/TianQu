package shijing.tianqu.runtime

/**
 * URL 解析器，负责处理深层链接、查询参数和路径参数的提取（策略/工具类）
 */
object UrlParser {
    /**
     * URL 解析工具
     * @return Pair(path, query)
     */
    fun parseUrl(url: String): Pair<String, String> {
        // 处理深层链接（去掉 scheme 和 host，例如 app://example.com/user/123 -> /user/123）
        var processedUrl = url
        if (processedUrl.contains("://")) {
            val uriWithoutScheme = processedUrl.substringAfter("://")
            processedUrl = "/" + uriWithoutScheme.substringAfter("/")
        }

        val parts = processedUrl.split("?", limit = 2)
        val path = parts[0]
        val query = if (parts.size > 1) parts[1] else ""
        return Pair(path, query)
    }

    /**
     * 解析 Query 字符串，并进行 URL 跨平台解码
     */
    fun parseQuery(query: String): Map<String, List<String>> {
        if (query.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, MutableList<String>>()
        query.split("&").forEach {
            val kv = it.split("=", limit = 2)
            if (kv.isNotEmpty()) {
                val key = decode(kv[0])
                val value = if (kv.size > 1) decode(kv[1]) else ""
                result.getOrPut(key) { mutableListOf() }.add(value)
            }
        }
        return result
    }

    /**
     * 提取路径参数，并进行 URL 跨平台解码
     */
    fun extractPathParams(template: String, regexPattern: String, actualPath: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        
        // 提取模板中的参数名 (例如 "/user/{id}" -> ["id"])
        val paramNames = Regex("\\{([^/]+)\\}").findAll(template).map { it.groupValues[1] }.toList()
        if (paramNames.isEmpty()) return result
        
        // 使用正则提取实际值
        val matchResult = Regex(regexPattern).matchEntire(actualPath)
        if (matchResult != null) {
            // groupValues 的第 0 个元素是完整匹配，从第 1 个开始是捕获组
            val values = matchResult.groupValues.drop(1)
            paramNames.forEachIndexed { index, name ->
                if (index < values.size) {
                    result[name] = decode(values[index])
                }
            }
        }
        
        return result
    }

    /**
     * 简单的纯 Kotlin URL 解码实现，支持跨平台 (Android/iOS)。
     * 正确处理 UTF-8 编码的多字节字符（如中文）。
     */
    fun decode(encoded: String): String {
        if (!encoded.contains("%") && !encoded.contains("+")) return encoded
        
        val builder = StringBuilder()
        var i = 0
        val len = encoded.length
        while (i < len) {
            val ch = encoded[i]
            when {
                ch == '+' -> {
                    builder.append(' ')
                    i++
                }
                ch == '%' && i + 2 < len -> {
                    // 收集连续的 URL 编码字节以正确解码 UTF-8 多字节字符
                    val bytes = mutableListOf<Byte>()
                    while (i < len && encoded[i] == '%' && i + 2 < len) {
                        try {
                            val hex = encoded.substring(i + 1, i + 3)
                            bytes.add(hex.toInt(16).toByte())
                            i += 3
                        } catch (e: Exception) {
                            // 如果解析失败，把 '%' 放回去并中断字节收集
                            if (bytes.isEmpty()) {
                                builder.append(encoded[i])
                                i++
                            }
                            break
                        }
                    }
                    if (bytes.isNotEmpty()) {
                        builder.append(bytes.toByteArray().decodeToString())
                    }
                }
                else -> {
                    builder.append(ch)
                    i++
                }
            }
        }
        return builder.toString()
    }
}
