package shijing.tianqu

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview
import shijing.tianqu.router.RouteContext
import shijing.tianqu.router.RouteGuard
import shijing.tianqu.router.GuardChain
import shijing.tianqu.router.generated.RouteRegistry

import shijing.tianqu.runtime.RouterHost
import shijing.tianqu.runtime.rememberNavigator
import shijing.tianqu.runtime.ServiceManager
import shijing.tianqu.router.generated.ServiceRegistry

import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect

import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier

@Composable
@Preview
fun App() {
    // 示例：创建一个简单的局部路由守卫（只拦截特定路由）
    val guards = remember {
        listOf(
            object : RouteGuard {
                // 重写 matches 方法，实现局部拦截
                override fun matches(context: RouteContext): Boolean {
                    // 仅当跳转到带有 /user 的路径时才触发此守卫
                    return context.url.startsWith("/user")
                }

                override suspend fun canActivate(context: RouteContext, chain: GuardChain): Boolean {
                    println("🚀 [局部拦截器] 发现正在尝试进入 User 模块，URL: ${context.url}")
                    return chain.proceed(context) // 放行并交给下一个守卫
                }
            }
        )
    }

    // 初始化 ServiceManager (模块间通信)
    LaunchedEffect(Unit) {
        ServiceManager.init(ServiceRegistry.services)
    }

    // 存储由于降级产生的信息
    var notFoundRoute by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    // 初始化导航器实例，传入 KSP 生成的全局路由表和拦截守卫，并指定首页路径为嵌套路由容器 /main_tab
    val navigator = rememberNavigator(
        routes = RouteRegistry.routers,
        startRoute = "/main_tab",
        guards = guards,
        onRouteNotFound = { url ->
            println("⚠️ [全局降级] 找不到路由: $url")
            notFoundRoute = url
        }
    )

    // 监听 notFoundRoute 的变化，通过 navigator 跳转到预设的通用错误页或 H5
    LaunchedEffect(notFoundRoute) {
        notFoundRoute?.let { url ->
            // 这里为了演示，我们先跳转回首页，实际业务中可以跳转到 /404 页面
            navigator.navigateTo("/main_tab")
            notFoundRoute = null
        }
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
