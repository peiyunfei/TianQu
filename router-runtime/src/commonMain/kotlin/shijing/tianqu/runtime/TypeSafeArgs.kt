package shijing.tianqu.runtime

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import shijing.tianqu.router.RouterContext
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * 序列化配置：忽略未知键，保证前后端数据版本的兼容性
 */
val RouterJson = Json { 
    ignoreUnknownKeys = true 
    encodeDefaults = true 
}

/**
 * 类型安全导航扩展：接收一个使用 @Serializable 注解的数据类对象，将其序列化为 JSON 并进行 UrlSafe Base64 编码，附加在 URL 中。
 * 这种方式既能在编译期保证参数类型安全，又能保证 URL 的纯洁性，同时支持 DeepLink 分享。
 * 
 * @param path 目标路由的路径，例如 "/user"
 * @param args 实现了 kotlinx.serialization 的 Data Class 对象
 * @param saveState 是否保存当前栈的状态（适用于多 Tab 场景）
 * @param restoreState 是否尝试恢复目标栈的状态
 */
@OptIn(ExperimentalEncodingApi::class)
inline fun <reified T : Any> Navigator.navigateArgs(
    path: String, 
    args: T,
    saveState: Boolean = false, 
    restoreState: Boolean = false
) {
    val jsonString = RouterJson.encodeToString(serializer<T>(), args)
    val base64Payload = Base64.UrlSafe.encode(jsonString.encodeToByteArray())
    
    // 拼接参数到 URL 后面。如果原本已经有问号，就用 & 连接
    val separator = if (path.contains("?")) "&" else "?"
    val url = "$path${separator}__typed_args=$base64Payload"
    
    navigateTo(url, saveState = saveState, restoreState = restoreState)
}

/**
 * 替换当前页面（类型安全传参版）
 */
@OptIn(ExperimentalEncodingApi::class)
inline fun <reified T : Any> Navigator.replaceArgs(path: String, args: T) {
    val jsonString = RouterJson.encodeToString(serializer<T>(), args)
    val base64Payload = Base64.UrlSafe.encode(jsonString.encodeToByteArray())
    val separator = if (path.contains("?")) "&" else "?"
    replace("$path${separator}__typed_args=$base64Payload")
}

/**
 * 清空并入栈（类型安全传参版）
 */
@OptIn(ExperimentalEncodingApi::class)
inline fun <reified T : Any> Navigator.clearAndPushArgs(path: String, args: T) {
    val jsonString = RouterJson.encodeToString(serializer<T>(), args)
    val base64Payload = Base64.UrlSafe.encode(jsonString.encodeToByteArray())
    val separator = if (path.contains("?")) "&" else "?"
    clearAndPush("$path${separator}__typed_args=$base64Payload")
}

/**
 * 从 RouterContext 中获取类型安全的参数。
 * 配合 [navigateArgs] 使用。自动完成 Base64 解码与 JSON 反序列化。
 *
 * @return 反序列化成功的数据类对象，如果不存在或解析失败则返回 null
 */
@OptIn(ExperimentalEncodingApi::class)
inline fun <reified T : Any> RouterContext.getTypedArgs(): T? {
    val base64List = queryParams["__typed_args"]
    if (!base64List.isNullOrEmpty()) {
        return try {
            val jsonString = Base64.UrlSafe.decode(base64List.first()).decodeToString()
            RouterJson.decodeFromString(serializer<T>(), jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    return null
}
