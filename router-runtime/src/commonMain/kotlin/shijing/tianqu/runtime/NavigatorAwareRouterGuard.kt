package shijing.tianqu.runtime

/**
 * 允许 [shijing.tianqu.router.RouterGuard] 在初始化时获取所在 [Navigator] 实例的可选接口。
 *
 * 如果你的路由守卫在执行拦截或放行逻辑时，需要主动触发其他的导航操作
 * （例如在动态模块下载完成后主动将新路由节点注册进框架，或主动重定向），
 * 你可以实现此接口。框架（例如 [rememberTianQuApp]）会在 [Navigator] 创建完成后自动调用它。
 */
interface NavigatorAwareRouterGuard {

    /**
     * 当 [Navigator] 实例就绪时被框架回调
     *
     * @param navigator 当前所处的导航器实例
     */
    fun onNavigatorReady(navigator: Navigator)
}
