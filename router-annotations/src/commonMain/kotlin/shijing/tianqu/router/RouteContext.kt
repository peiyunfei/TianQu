package shijing.tianqu.router

/**
 * 路由上下文，包含当前导航的完整信息
 * 用于取代之前直接传递的参数 Map，强制使用基于 URL 的参数传递
 *
 * @param url 完整的导航 URL，例如 "/user/123?ref=home"
 * @param pathParams 从路径中提取的参数，如 /user/{id} 提取的 id="123"
 * @param queryParams URL 中的查询参数，如 ?ref=home 提取的 ref=["home"]
 * @param extra 额外传递的复杂对象参数（不适合或者不方便放在URL中的数据，比如 Bitmap、自定义数据类实例等）
 */
data class RouteContext(
    val url: String,
    val pathParams: Map<String, String> = emptyMap(),
    val queryParams: Map<String, List<String>> = emptyMap(),
    val extra: Any? = null
)
