package shijing.tianqu.runtime

/**
 * 路由事件密封类，用于事件总线
 */
sealed class RouterEvent {
    data class NotFound(val url: String) : RouterEvent()
    data class Navigated(val action: NavigationAction, val url: String) : RouterEvent()
}
