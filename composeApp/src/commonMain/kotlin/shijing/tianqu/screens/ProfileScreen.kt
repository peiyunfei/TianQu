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

@OptIn(ExperimentalMaterial3Api::class)
@Router(
    path = "/profile",
    enterTransition = RouteTransition.Slide,
    exitTransition = RouteTransition.Slide
)
@Composable
fun ProfileScreen(context: RouteContext) {
    val navigator = LocalNavigator.current
    
    // 尝试提取从上一页通过 Navigator.navigateTo(..., extra) 传递过来的复杂对象参数
    // 因为 extra 是 Any? 类型，我们需要将其安全转换为期望的业务模型 UserProfile
    val profileData = context.extra as? UserProfile

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("个人资料") },
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
                text = "👤 个人资料",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "URL: ${context.url}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            // 如果成功接收到传递过来的对象数据，就在界面上展示出来
            if (profileData != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("接收到的对象数据:", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("姓名: ${profileData.name}")
                        Text("年龄: ${profileData.age}")
                        Text("VIP用户: ${if (profileData.isVip) "是" else "否"}")
                    }
                }
            } else {
                Text(
                    text = "没有收到对象参数",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = { navigator.navigateTo("/settings") }) {
                Text("从这里去设置页")
            }
        }
    }
}