package shijing.tianqu.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import shijing.tianqu.router.RouteContext
import shijing.tianqu.router.RouteTransition
import shijing.tianqu.router.Router
import shijing.tianqu.runtime.LocalNavigator

/**
 * 设置页面演示。
 * 包含基本的回退操作以及返回根节点 (PopToRoot) 的演示。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Router(
    path = "/settings",
    enterTransition = RouteTransition.Slide,
    exitTransition = RouteTransition.Slide
)
@Composable
fun SettingsScreen(context: RouteContext) {
    // 从上下文中获取全局注入的 Navigator 实例
    val navigator = LocalNavigator.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "⚙️ 设置页",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "URL: ${context.url}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { navigator.popBackStack() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("返回上一页")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    // 演示直接回到根节点
                    navigator.popToRoot()
                }
            ) {
                Text("回到首页 (PopToRoot)")
            }
        }
    }
}