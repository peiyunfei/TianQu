package shijing.tianqu.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import shijing.tianqu.router.Router
import shijing.tianqu.runtime.LocalNavigator

@Router(path = "/home")
@Composable
fun HomeScreen() {
    val navigator = LocalNavigator.current

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🏠 首页",
            style = MaterialTheme.typography.headlineLarge
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "欢迎使用 KMP 路由框架！",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = { navigator.navigateTo("/profile") }) {
            Text("前往个人资料页")
        }
        Spacer(modifier = Modifier.height(12.dp))

        Button(onClick = { navigator.navigateTo("/settings") }) {
            Text("前往设置页")
        }
    }
}