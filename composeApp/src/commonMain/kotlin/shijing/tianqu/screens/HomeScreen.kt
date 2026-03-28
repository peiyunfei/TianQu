package shijing.tianqu.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import shijing.tianqu.router.RouterContext
import shijing.tianqu.router.Router
import shijing.tianqu.runtime.LocalNavigator
import shijing.tianqu.runtime.utils.rememberService
import shijing.tianqu.services.UserService
import shijing.tianqu.featureb.FeatureBManager
import shijing.tianqu.runtime.navigateArgs

import shijing.tianqu.runtime.transition.routerSharedBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size

// 定义一个用于演示对象传递的数据类
data class UserProfile(val name: String, val age: Int, val isVip: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Router(
    path = "/home"
)
@Composable
fun HomeScreen(context: RouterContext) {
    val navigator = LocalNavigator.current
    
    // 增加一个测试状态，用于验证切换回来时状态是否丢失
    var testCounter by rememberSaveable { mutableStateOf(0) }
    // 用于保存从设置页返回的结果（必须使用 rememberSaveable，否则从 Settings 返回后页面重组会导致状态重置为 null）
    var returnedResult by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("首页") })
        }
    ) { paddingValues ->
        // 为页面添加垂直滚动状态
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🏠 首页",
                style = MaterialTheme.typography.headlineLarge
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 演示模块间通信：使用最新支持协程的 rememberService() 自动挂起获取服务
            val userService = rememberService<UserService>()
            val userName = userService?.getUserName() ?: "正在异步加载服务..."

            Text(
                text = "欢迎您, $userName (来自 Coroutine Service)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "当前 URL: ${context.url}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 测试状态保持的交互按钮
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("状态保持测试(计数值应保持不变): $testCounter", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { testCounter++ }) {
                    Text("+1")
                }
            }
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

            // 演示通过协程获取返回的 result 数据（类似 Compose awaitFirstDown）
            if (returnedResult != null) {
                Text(
                    text = "接收到返回结果: $returnedResult",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(onClick = {
                // 使用 navigator 自带的长生命周期作用域，防止页面因离开组合树而取消协程
                navigator.coroutineScope.launch {
                    // 调用挂起函数，一直等待直到设置页 popBackStack 返回结果
                    val result = navigator.awaitNavigateForResult("/settings")
                    if (result is String) {
                        returnedResult = "协程获取: $result"
                    }
                }
            }) {
                Text("前往设置页 (协程等待结果)")
            }
            Spacer(modifier = Modifier.height(12.dp))

            Button(onClick = {
                // 使用回调方式获取返回结果，适用于非协程环境 (如 Swift)
                navigator.navigateWithResult("/settings").onResult { result ->
                    if (result is String) {
                        returnedResult = "回调获取: $result"
                    }
                }
            }) {
                Text("前往设置页 (回调获取结果)")
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

            // 测试跨模块路由导航
            Button(
                onClick = { navigator.navigateTo("/featureb/home") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("跨模块导航 (Feature B)")
            }

            // 演示自定义动画
            Button(onClick = { navigator.navigateTo("/demo_anim") }) {
                Text("测试自定义动画 (RotateScale)")
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            // 演示查询参数和深层链接格式
            Button(onClick = { navigator.navigateTo("app://shijing.tianqu/user/999?source=home_banner&vip=true") }) {
                Text("打开深层链接 (带查询参数)")
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            // 演示弹窗路由
            Button(
                onClick = { navigator.navigateTo("/demo_dialog") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("测试弹窗路由 (Dialog)")
            }
            Spacer(modifier = Modifier.height(12.dp))

            // 测试类型安全传参
            Button(
                onClick = {
                    navigator.navigateArgs(
                        path = "/typesafe_demo",
                        args = UserDetailArgs(
                            userId = 999L,
                            username = "天衢路由",
                            isVip = true,
                            scores = listOf(100, 98, 95)
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("测试类型安全传参 (Data Class)")
            }
            Spacer(modifier = Modifier.height(12.dp))

            // 测试共享元素过渡动画
            Box(
                modifier = Modifier
                    .routerSharedBounds(key = "shared_box_1")
                    .size(100.dp)
                    .background(Color.Red)
                    .clickable { navigator.navigateTo("/shared_element_demo") },
                contentAlignment = Alignment.Center
            ) {
                Text("小红块", color = Color.White)
            }
            Spacer(modifier = Modifier.height(12.dp))

            // 测试专属作用域 ViewModel
            Button(
                onClick = { navigator.navigateTo("/demo_viewmodel") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
            ) {
                Text("测试 ViewModel 生命周期绑定")
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