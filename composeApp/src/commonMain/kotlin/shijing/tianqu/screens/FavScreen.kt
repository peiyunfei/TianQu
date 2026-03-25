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

/**
 * 用户详情页面演示。
 * 展示了如何从 RouteContext 中提取由正则匹配出来的路径参数（Path Params）
 * 以及如何获取 URL 后面的查询参数（Query Params）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Router(
    path = "/fav",
    transition = "Slide",
)
@Composable
fun FavScreen(context: RouterContext) {
    val navigator = LocalNavigator.current
    
    // 从上下文中提取由正则匹配出来的路径参数
    val userId = context.pathParams["id"] ?: "未知"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("收藏") },
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
                text = "👤 用户 ID: $userId",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "完整的导航 URL:",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = context.url,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            if (context.queryParams.isNotEmpty()) {
                Text(
                    text = "查询参数 (Query):",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                context.queryParams.forEach { (key, values) ->
                    Text(
                        text = "$key = ${values.joinToString(", ")}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                Text(
                    text = "没有携带任何查询参数。",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { navigator.popBackStack() }) {
                Text("返回上一页")
            }
        }
    }
}