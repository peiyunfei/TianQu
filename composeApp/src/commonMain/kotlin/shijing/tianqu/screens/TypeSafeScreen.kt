package shijing.tianqu.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import shijing.tianqu.router.Router
import shijing.tianqu.router.RouterContext
import shijing.tianqu.runtime.LocalNavigator
import shijing.tianqu.runtime.getTypedArgs

// 1. 定义序列化的 Data Class 参数对象
@Serializable
data class UserDetailArgs(
    val userId: Long,
    val username: String,
    val isVip: Boolean,
    val scores: List<Int>
)

@OptIn(ExperimentalMaterial3Api::class)
@Router(path = "/typesafe_demo")
@Composable
fun TypeSafeScreen(context: RouterContext) {
    val navigator = LocalNavigator.current

    // 2. 从 context 中通过扩展函数直接获取类型安全的 Data Class 对象
    val args = context.getTypedArgs<UserDetailArgs>()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("类型安全传参演示") },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("这是一个使用 kotlinx.serialization 传递 Data Class 的页面")
            
            if (args != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("✅ 解析成功！", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("User ID: ${args.userId}")
                        Text("Username: ${args.username}")
                        Text("Is VIP: ${if (args.isVip) "👑 是" else "否"}")
                        Text("Scores: ${args.scores.joinToString(", ")}")
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        "❌ 缺少参数或解析失败",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}
