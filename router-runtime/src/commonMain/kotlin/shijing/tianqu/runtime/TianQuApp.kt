package shijing.tianqu.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import shijing.tianqu.runtime.service.ServiceManager

/**
 * [rememberTianQuApp] 提供了一个更高层次的路由组件装配入口。
 * 相比直接使用 [rememberNavigator]，它内部自动完成了常见的样板代码组装，
 * 让接入方的应用入口 (App.kt) 更清晰，只需关注业务配置。
 *
 * 自动处理的事项包括：
 * 1. 自动调用 [ServiceManager.init] 初始化跨模块服务（如果提供了 [serviceProviders]）
 * 2. 自动给实现了 [NavigatorAwareRouterGuard] 的拦截器实例注入 navigator
 * 3. 自动监听 [Navigator.routeEvents] 并将其路由至更简单的 [onRouteEvent] 回调
 * 4. 自动挂载 BackHandler 处理返回按键
 *
 */
@Composable
fun rememberTianQuApp(
    block: TianQuAppConfig.() -> Unit
): Navigator {
    val config = remember { TianQuAppConfig().apply(block) }

    require(config.routes.isNotEmpty()) { "TianQuApp requires at least one route." }
    require(config.startRoute.isNotEmpty()) { "TianQuApp requires a startRoute." }

    // 1. 自动初始化跨模块服务（在重组时不重复 init，仅在 serviceProviders 变化时重新执行）
    remember(config.serviceProviders) {
        config.serviceProviders?.also(ServiceManager::init)
    }

    // 2. 调用核心的导航器构建
    val navigator = rememberNavigator(
        routes = config.routes,
        startRoute = config.startRoute,
        guards = config.guards,
        preloaders = config.preloaders,
        routerHandler = config.routerHandler,
        parent = null,
        maxStackSize = config.maxStackSize
    )

    // 3. 自动处理事件流监听，交给外部回调
    LaunchedEffect(navigator, config.onRouteEvent) {
        navigator.routeEvents.collect { event ->
            config.onRouteEvent(event, navigator)
        }
    }

    // 4. 自动给依赖 Navigator 的守卫注入实例
    remember(navigator, config.guards) {
        config.guards.filterIsInstance<NavigatorAwareRouterGuard>()
            .forEach { it.onNavigatorReady(navigator) }
    }

    // 5. 自动挂接 BackHandler 拦截后退手势/物理按键
    if (config.enableBackHandler) {
        BackHandler(enabled = navigator.backStack.size > 1) {
            navigator.pop()
        }
    }

    return navigator
}
