package shijing.tianqu.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import shijing.tianqu.router.Router
import shijing.tianqu.router.RouterContext
import shijing.tianqu.runtime.LocalNavigator
import shijing.tianqu.runtime.RoutePreloader
import shijing.tianqu.runtime.rememberPreloadData

/**
 * 模拟网络数据实体
 */
data class UserDetailData(val name: String, val desc: String)

/**
 * 实现预加载器：在路由跳转动画执行的同时，它会在后台异步挂起请求数据
 */
class UserDetailPreloader : RoutePreloader<UserDetailData> {
    override suspend fun preload(context: RouterContext): UserDetailData {
        // 模拟网络请求耗时 2 秒
        delay(2000)
        return UserDetailData(name = "天衢协程预加载", desc = "我是和动画一起并发加载回来的数据！")
    }
}

@Router(path = "/demo_preload")
@Composable
fun DemoPreloadScreen(context: RouterContext) {
    val navigator = LocalNavigator.current
    // 从框架中获取预加载的数据状态
    val preloadedData by rememberPreloadData<UserDetailData>()

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (preloadedData == null) {
                // 如果页面动画播完了（比如只有 300ms），但网络请求（2s）还没回来，就会显示这个
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("页面已极速打开，正在等待并发网络数据...")
            } else {
                Text("🎉 加载成功！")
                Text("姓名: ${preloadedData?.name}")
                Text("描述: ${preloadedData?.desc}")
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { navigator.popBackStack() }) {
                Text("返回")
            }
        }
    }
}
