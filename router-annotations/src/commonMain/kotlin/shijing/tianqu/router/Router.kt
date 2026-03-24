package shijing.tianqu.router

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