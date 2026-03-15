package shijing.tianqu.runtime

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

/**
 * CompositionLocal 用于在组件树中传递 Navigator 实例
 */
val LocalNavigator = compositionLocalOf<Navigator> {
    error("No Navigator provided. Make sure to wrap your content with KmpRouteHost.")
}

/**
 * 路由宿主容器 Composable
 *
 * @param routes 由 KSP 生成的 RouteRegistry.routes
 * @param startRoute 起始路由路径
 */
@Composable
fun RouterHost(
    routes: Map<String, @Composable () -> Unit>,
    startRoute: String,
    modifier: Modifier = Modifier
) {
    val navigator = remember { 
        Navigator(routes).apply {
            navigateTo(startRoute)
        }
    }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        Box(modifier = modifier.fillMaxSize()) {
            navigator.currentScreen()
        }
    }
}