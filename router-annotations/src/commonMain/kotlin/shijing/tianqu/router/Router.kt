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

/**
 * 路由守卫接口，采用责任链模式
 */
interface RouteGuard {
    /**
     * 决定是否可以进入该路由
     *
     * @param context 目标路由的上下文信息
     * @param chain 责任链，允许异步调用和传递给下一个守卫
     * @return true 表示允许进入，false 表示拦截
     */
    suspend fun canActivate(context: RouteContext, chain: GuardChain): Boolean
}

/**
 * 守卫责任链接口
 */
interface GuardChain {
    suspend fun proceed(context: RouteContext): Boolean
}

/**
 * 页面切换动画枚举
 */
enum class RouteTransition {
    None,       // 无动画
    Fade,       // 渐隐渐现
    Slide,      // 左右平移
    Scale       // 缩放
}

/**
 * 标记一个 Composable 函数为路由目的地
 *
 * @param path 路由路径，支持参数化，例如 "/home", "/user/{id}"
 * @param enterTransition 进入该页面时的动画
 * @param exitTransition 离开该页面时的动画
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Router(
    val path: String,
    val enterTransition: RouteTransition = RouteTransition.Slide,
    val exitTransition: RouteTransition = RouteTransition.Slide
)