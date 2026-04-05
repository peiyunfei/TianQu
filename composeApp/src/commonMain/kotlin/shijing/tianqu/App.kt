package shijing.tianqu

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview
import shijing.tianqu.router.RouterContext
import shijing.tianqu.router.RouterGuard
import shijing.tianqu.router.GuardChain
import shijing.tianqu.router.generated.GlobalRouteAggregator

import shijing.tianqu.runtime.RouterHost
import shijing.tianqu.runtime.rememberNavigator
import shijing.tianqu.runtime.service.ServiceManager

import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import shijing.tianqu.screens.DynamicFeatureGuard
import shijing.tianqu.screens.UserDetailPreloader

@Composable
@Preview
fun App() {
    // 示例：创建一个简单的局部路由守卫（只拦截特定路由）
    val dynamicGuard = remember { DynamicFeatureGuard() }
    val guards = remember {
        listOf(
            dynamicGuard,
            object : RouterGuard {
                // 重写 matches 方法，实现局部拦截
                override fun matches(context: RouterContext): Boolean {
                    // 仅当跳转到带有 /user 的路径时才触发此守卫
                    return context.url.startsWith("/user")
                }

                override suspend fun canActivate(context: RouterContext, chain: GuardChain): Boolean {
                    println("🚀 [局部拦截器] 发现正在尝试进入 User 模块，URL: ${context.url}")
                    return chain.proceed(context) // 放行并交给下一个守卫
                }
            }
        )
    }

    // 初始化 ServiceManager (模块间通信)
    ServiceManager.init(GlobalRouteAggregator.services)

    val preloaders = remember { mapOf("/demo_preload" to UserDetailPreloader()) }
    // 初始化导航器实例，传入 KSP 生成的全局路由表和拦截守卫，并指定首页路径为嵌套路由容器 /main_tab
    val navigator = rememberNavigator(
        routes = GlobalRouteAggregator.routers,
        startRoute = "/main_tab",
        guards = guards,
        preloaders = preloaders
    )
    dynamicGuard.navigator = navigator

    // 监听 Navigator 路由事件总线
    LaunchedEffect(navigator) {
        navigator.routeEvents.collect { event ->
            when (event) {
                is shijing.tianqu.runtime.RouterEvent.NotFound -> {
                    println("⚠️ [全局事件总线] 拦截到未注册的路由: ${event.url}，重定向回 /main_tab")
                    navigator.navigateTo("/main_tab")
                }
                is shijing.tianqu.runtime.RouterEvent.Navigated -> {
                    println("ℹ️ [全局事件总线] 路由跳转成功: ${event.url} [${event.action}]")
                }
            }
        }
    }

    // 支持物理返回键 / 手势返回（当导航栈中多于1个页面时启用拦截并出栈）
    BackHandler(enabled = navigator.backStack.size > 1) {
        navigator.pop()
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            // 使用路由框架提供的 Host 组件，承载整个应用的 UI
            RouterHost(
                navigator = navigator
            )
        }
    }
}
