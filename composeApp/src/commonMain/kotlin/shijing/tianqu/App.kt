package shijing.tianqu

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview
import shijing.tianqu.router.generated.RouteRegistry

import shijing.tianqu.runtime.RouterHost

@Composable
@Preview
fun App() {
    MaterialTheme {
        // 使用路由框架，传入 KSP 生成的路由表
        RouterHost(
            routes = RouteRegistry.routers,
            startRoute = "/home"
        )
    }
}
