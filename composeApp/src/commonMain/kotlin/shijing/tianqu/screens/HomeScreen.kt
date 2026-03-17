package shijing.tianqu.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import shijing.tianqu.router.RouteContext
import shijing.tianqu.router.RouteTransition
import shijing.tianqu.router.Router
import shijing.tianqu.runtime.LocalNavigator
import shijing.tianqu.runtime.ServiceManager
import shijing.tianqu.services.UserService
import shijing.tianqu.featureb.FeatureBManager

// 定义一个用于演示对象传递的数据类
data class UserProfile(val name: String, val age: Int, val isVip: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Router(
    path = "/home",
    enterTransition = RouteTransition.Slide,
    exitTransition = RouteTransition.Slide
)
@Composable
fun HomeScreen(context: RouteContext) {
    val navigator = LocalNavigator.current

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("首页") })
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
                text = "🏠 首页",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 演示模块间通信：通过接口获取服务实现
            val userService = ServiceManager.getService(UserService::class) as? UserService
            val userName = userService?.getUserName() ?: "未知用户"

            Text(
                text = "欢迎您, $userName (来自 UserService)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "当前 URL: ${context.url}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(onClick = { 
                // 演示传递复杂对象参数：
                // 实例化一个业务对象，并通过 navigator.navigateTo 方法的 extra 参数进行传递。
                // 这种方式不需要将对象序列化进 URL 字符串中，非常适合在同一个应用内传递大数据。
                val userProfile = UserProfile(name = "Kotlin开发者", age = 25, isVip = true)
                navigator.navigateTo("/profile", extra = userProfile) 
            }) {
                Text("前往个人资料页 (带对象参数)")
            }
            Spacer(modifier = Modifier.height(12.dp))

            // 演示从上一个页面获取返回的 result 数据
            val returnedResult = context.result as? String
            if (returnedResult != null) {
                Text(
                    text = "接收到返回结果: $returnedResult",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(onClick = { navigator.navigateTo("/settings") }) {
                Text("前往设置页 (带返回结果演示)")
            }
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { navigator.navigateTo("/not_exist_page") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("跳转未知路由 (测试全局降级)")
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            // 演示参数化路由
            Button(onClick = { navigator.navigateTo("/user/1001") }) {
                Text("前往用户 1001 详情")
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            // 演示查询参数和深层链接格式
            Button(onClick = { navigator.navigateTo("app://shijing.tianqu/user/999?source=home_banner&vip=true") }) {
                Text("打开深层链接 (带查询参数)")
            }
            Spacer(modifier = Modifier.height(12.dp))

            // 演示模块间反向通信：composeApp(上层)依赖feature-b(底层)
            // 点击此按钮，调用 feature-b 中的逻辑，feature-b 再通过接口回调 composeApp 的实现
            Button(onClick = {
                FeatureBManager.doSomethingAndTrack()
            }) {
                Text("触发 Feature B 逻辑 (依赖反转通信)")
            }
        }
    }
}