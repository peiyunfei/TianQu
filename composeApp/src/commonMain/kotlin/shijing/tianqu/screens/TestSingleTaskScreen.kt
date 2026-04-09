package shijing.tianqu.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import shijing.tianqu.router.LaunchMode
import shijing.tianqu.router.Router
import shijing.tianqu.router.RouterContext
import shijing.tianqu.runtime.LocalNavigator
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Router(
    path = "/test_single_task",
    launchMode = LaunchMode.SINGLE_TASK
)
@Composable
fun TestSingleTaskScreen(context: RouterContext) {
    val navigator = LocalNavigator.current
    
    // 用于验证页面是否被重建。如果是复用，这个值会保持不变。
    var initTimestamp by rememberSaveable { mutableStateOf(Random.nextInt(1000000).toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SingleTask 测试") },
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
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
                text = "SingleTask 模式测试",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "页面创建时间: $initTimestamp",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "(如果发生了复用，这个时间戳应该保持不变)",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "当前接收到的参数:",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // 每次 context 更新，这里都会重组并展示最新的 url
            Text(
                text = context.url,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    // 跳转到自身，传递新的参数
                    navigator.navigateTo("/test_single_task?param=${Random.nextInt(100)}")
                }
            ) {
                Text("跳转到自己 (传递新参数)")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    // 跳转到一个普通的中间页，使其不在栈顶
                    navigator.navigateTo("/test_single_task_middle")
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)

            ) {
                Text("跳转到中间页 (使本页不在栈顶)")
            }
        }
    }
}
