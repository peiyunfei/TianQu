package shijing.tianqu

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.jetbrains.compose.ui.tooling.preview.Preview
import shijing.tianqu.runtime.GuardChain
import shijing.tianqu.runtime.RouterContext
import shijing.tianqu.runtime.RouterGuard
import shijing.tianqu.runtime.RouterEvent
import shijing.tianqu.runtime.RouterHost
import shijing.tianqu.screens.DynamicFeatureGuard
import shijing.tianqu.screens.UserDetailPreloader

@Composable
@Preview
fun App() {
    // 1. 定义业务所需的路由拦截器
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

    // 2. 预加载器配置
    val preloaders = remember { mapOf("/demo_preload" to UserDetailPreloader()) }

    // 3. 核心装配：使用 App 层的零样板包装 rememberAppTianQuState。
    // 它会自动注入 GlobalRouteAggregator.routers 和 services，业务只关心自己的策略。
    val navigator = rememberAppTianQuState {
        startRoute = "/main_tab"
        this.guards = guards // 避免与外部 guards 冲突
        this.preloaders = preloaders
        onRouteEvent = { event, navigator ->
            when (event) {
                // 业务决策：拦截到未注册路由时，重定向回 /main_tab
                is RouterEvent.NotFound -> {
                    println("⚠️ [全局事件总线] 拦截到未注册的路由: ${event.url}，重定向回 /main_tab")
                    navigator.navigateTo("/main_tab")
                }
                is RouterEvent.Navigated -> {
                    println("ℹ️ [全局事件总线] 路由跳转成功: ${event.url} [${event.action}]")
                }
            }
        }
    }

    // 4. 渲染 UI
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            // 使用路由框架提供的 Host 组件，承载整个应用的 UI
            RouterHost(
                navigator = navigator
            )
        }
    }
}
