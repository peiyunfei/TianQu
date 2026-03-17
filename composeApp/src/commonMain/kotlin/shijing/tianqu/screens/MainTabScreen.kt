package shijing.tianqu.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import shijing.tianqu.router.RouteContext
import shijing.tianqu.router.RouteTransition
import shijing.tianqu.router.Router
import shijing.tianqu.router.generated.RouteRegistry
import shijing.tianqu.runtime.LocalNavigator
import shijing.tianqu.runtime.RouterHost
import shijing.tianqu.runtime.rememberNavigator

/**
 * 演示嵌套路由 (Nested Routing) 和底部导航栏。
 */
@Router(
    path = "/main_tab",
    enterTransition = RouteTransition.Fade,
    exitTransition = RouteTransition.Fade
)
@Composable
fun MainTabScreen(context: RouteContext) {
    val parentNavigator = LocalNavigator.current
    
    // 为 Home Tab 创建独立的子导航栈，并将 parent 设为全局 navigator
    val homeNavigator = rememberNavigator(
        routes = RouteRegistry.routers,
        startRoute = "/home",
        parent = parentNavigator
    )
    
    // 为 Profile Tab 创建独立的子导航栈
    val profileNavigator = rememberNavigator(
        routes = RouteRegistry.routers,
        startRoute = "/user/1001", // 这里借用 User 页面作为我的页面
        parent = parentNavigator
    )
    
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "首页") },
                    label = { Text("首页") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "我的") },
                    label = { Text("我的") }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // 使用 Box 和 if/else 来控制显示的 RouterHost
            // 由于 Navigator 是 remember 缓存的，因此即使 RouterHost 移除组合，导航状态(backStack)也不会丢失。
            if (selectedTab == 0) {
                RouterHost(navigator = homeNavigator)
            } else {
                RouterHost(navigator = profileNavigator)
            }
        }
    }
}
