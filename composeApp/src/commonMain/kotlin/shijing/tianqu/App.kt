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
    // 示例：创建一个简单的全局路由守卫
    // 可以拦截需要登录才能访问的页面，例如判断 token 是否过期等
    val guards = remember {
        listOf(
            object : RouteGuard {
                override suspend fun canActivate(context: RouteContext, chain: GuardChain): Boolean {
                    // 这里可以添加异步逻辑：比如判断路径是否为 "/settings"，模拟请求网络验证权限等
                    // println("RouteGuard checking: ${context.url}")
                    return chain.proceed(context) // 放行并交给下一个守卫
                }
            }
        )
    }

    // 初始化 ServiceManager (模块间通信)
    LaunchedEffect(Unit) {
        ServiceManager.init(ServiceRegistry.services)
    }

    // 初始化导航器实例，传入 KSP 生成的全局路由表和拦截守卫，并指定首页路径
    val navigator = rememberNavigator(
        routes = RouteRegistry.routers,
        startRoute = "/home",
        guards = guards // 注册路由守卫
    )

    // 处理系统返回键（拦截返回事件，当栈内页面大于1时出栈，否则走系统默认行为退出应用）
    BackHandler(enabled = navigator.backStack.size > 1) {
        navigator.popBackStack()
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
