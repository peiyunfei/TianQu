package shijing.tianqu.router

/**
 * 标记一个 Composable 函数为路由目的地
 *
 * @param path 路由路径，例如 "/home", "/user/{id}"
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Router(val path: String)