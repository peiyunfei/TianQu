package shijing.tianqu.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import shijing.tianqu.router.RouterContext
import shijing.tianqu.router.Router
import shijing.tianqu.runtime.LocalNavigator
import shijing.tianqu.runtime.rememberRouterScope
import shijing.tianqu.runtime.Navigator
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * 设置页面演示。
 * 包含基本的回退操作以及返回根节点 (PopToRoot) 的演示。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Router(
    path = "/settings",
    transition = "Slide",
)
@Composable
fun SettingsScreen(context: RouterContext) {
    // 从上下文中获取全局注入的 Navigator 实例
    val navigator = LocalNavigator.current
    
    // 获取注入了 Navigator Element 的协程作用域
    val routerScope = rememberRouterScope()

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
                .padding(paddingValues),
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
                onClick = { navigator.popBackStack(result = "Settings Saved Successfully!") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("返回并保存设置")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    // 演示直接退回到首页，可用于跳过多级中间页面
                    navigator.popUntil("/home")
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("退回到首页 (popUntil)")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // 演示替换当前栈顶，返回时不会再经过原页面
                    navigator.replace("/user/999")
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("替换为用户页 (replace)")
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // 演示使用 CoroutineContext Element 获取 Navigator 并延迟跳转
                    routerScope.launch {
                        val nav = coroutineContext[Navigator] ?: return@launch
                        delay(1000)
                        nav.popBackStack(result = "Delayed Context Navigation!")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("协程上下文延迟返回 (1秒)")
            }
        }
    }
}