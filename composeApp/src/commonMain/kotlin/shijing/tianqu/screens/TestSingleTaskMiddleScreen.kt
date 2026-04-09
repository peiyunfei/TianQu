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
    path = "/test_single_task_middle",
    launchMode = LaunchMode.STANDARD
)
@Composable
fun TestSingleTaskMiddleScreen(context: RouterContext) {
    val navigator = LocalNavigator.current
    
    var initTimestamp by rememberSaveable { mutableStateOf(Random.nextInt(1000000).toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SingleTask 中间页") },
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
                text = "这是用来垫在 SingleTask 上面的中间页",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "页面创建时间: $initTimestamp",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    // 跳转回 SingleTask 页，触发其清理其上的栈逻辑
                    navigator.navigateTo("/test_single_task?param=from_middle_page_${Random.nextInt(100)}")
                }
            ) {
                Text("返回 SingleTask 页 (清理我吧！)")
            }
        }
    }
}
