package shijing.tianqu.router

/**
 * 路由节点的类型，区分全屏页面和弹窗等
 */
enum class RouteType {
    SCREEN,     // 全屏页面
    DIALOG,     // 悬浮弹窗
    BOTTOM_SHEET // 底部面板（如果将来需要支持）
}

/**
 * 路由启动模式
 */
enum class LaunchMode {
    STANDARD,       // 标准模式：每次都创建新实例
    SINGLE_TOP,     // 栈顶复用：如果栈顶已经是该页面，则不创建新实例
    SINGLE_TASK     // 栈内复用：如果栈内存在该页面，则将其上面的所有页面出栈
}

/**
 * 标记一个 Composable 函数为路由目的地
 *
 * @param path 路由路径，支持参数化，例如 "/home", "/user/{id}"
 * @param transition 该页面使用的过渡动画策略名称，默认为 "Slide"。
 *                   这里的名称将对应框架内置的动画（如 "Slide", "Fade", "Scale", "None"）
 *                   或通过 `@Transition(name = "xxx")` 注册的自定义动画。
 * @param type 路由页面的类型，默认为 SCREEN（全屏）。可设置为 DIALOG。
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Router(
    val path: String,
    val transition: String = "Slide",
    val type: RouteType = RouteType.SCREEN,
    val launchMode: LaunchMode = LaunchMode.STANDARD,
)