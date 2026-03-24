package shijing.tianqu.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import shijing.tianqu.router.RouteContext
import shijing.tianqu.router.RouteTransition
import shijing.tianqu.router.Router
import shijing.tianqu.router.generated.RouteRegistry

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

    // 将 Tab 选中状态改为 rememberSaveable，防止后台被销毁时重置回首页
    var selectedTab by rememberSaveable { mutableStateOf(0) }

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
        // 核心：不使用多个 Navigator，而是用 SaveableStateHolder 在同一个页面下保持两个 Tab 的组合节点状态。
        // 这是很多 Compose 底层导航库实现多 Tab 的标准轻量级方案：单宿主 + 多状态挂载点
        val saveableStateHolder = rememberSaveableStateHolder()

        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // 通过 SaveableStateProvider 为每个 Tab 提供独立的 State 保存空间
            // 当 selectedTab 变化时，前一个 tab 的组件会离开组合树，但其状态（由 key "tab_home" 保存）会被持久化
            if (selectedTab == 0) {
                saveableStateHolder.SaveableStateProvider("tab_home") {
                    // 动态获取对应的节点并执行渲染
                    val homeNode = RouteRegistry.routers.find { it.path == "/home" }
                    homeNode?.composable?.invoke(RouteContext("/home"))
                }
            } else {
                saveableStateHolder.SaveableStateProvider("tab_profile") {
                    val profileNode = RouteRegistry.routers.find { it.path == "/user/{id}" }
                    // 注意：这只是为了演示。在完整的路由框架里，Tab 的切换通常不直接用 /user/{id}，而是专门的 TabRoot 页面
                    profileNode?.composable?.invoke(RouteContext("/user/1001", mapOf("id" to "1001")))
                }
            }
        }
    }
}
