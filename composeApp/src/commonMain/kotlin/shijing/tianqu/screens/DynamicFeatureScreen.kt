package shijing.tianqu.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import shijing.tianqu.runtime.LocalNavigator
import shijing.tianqu.runtime.LocalStackEntry

/**
 * 模拟一个动态下载的模块页面。
 * 注意：故意不使用 @Router 注解，使得它在编译期不被收集，
 * 只有在运行时通过 DynamicFeatureGuard 按需手动注入
 */
@Composable
fun DynamicFeatureScreen() {
    val nav = LocalNavigator.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8F5E9)), // 浅绿色背景区分
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("✨ 动态加载模块", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("这个页面在启动时不在内存和路由表中！\n完全通过协程挂起并在后台\"下载\"后动态注册进来的。")
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { nav.popBackStack() }) {
            Text("返回上页")
        }
    }
}
