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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import shijing.tianqu.runtime.LocalNavigator
import shijing.tianqu.runtime.RoutePreloader
import shijing.tianqu.runtime.rememberPreloadData
import shijing.tianqu.runtime.tianquViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * 模拟网络数据实体
 */
data class UserDetailData(val name: String, val desc: String)

/**
 * 模拟的 ViewModel，负责处理业务逻辑和状态管理
 */
class DemoPreloadViewModel : ViewModel() {
    // ViewModel 内部维护的 UI 状态
    var uiState by mutableStateOf<UiState>(UiState.Loading)
        private set

    // 接收预加载的数据并更新状态
    fun onPreloadResult(result: Result<UserDetailData>) {
        uiState = if (result.isSuccess) {
            UiState.Success(result.getOrThrow())
        } else {
            UiState.Error(result.exceptionOrNull()?.message ?: "未知错误")
        }
    }

    // 模拟 ViewModel 内部发起的重试请求
    fun retry() {
        uiState = UiState.Loading
        // 实际业务中这里会调用 Repository 发起网络请求
        // 这里简单模拟重试成功
        viewModelScope.launch {
            delay(1000)
            uiState = UiState.Success(UserDetailData(name = "重试成功用户", desc = "这是 ViewModel 重新请求的数据"))
        }
    }

    sealed class UiState {
        object Loading : UiState()
        data class Success(val data: UserDetailData) : UiState()
        data class Error(val message: String) : UiState()
    }
}

/**
 * 实现预加载器：在路由跳转动画执行的同时，它会在后台异步挂起请求数据
 */
class UserDetailPreloader : RoutePreloader<UserDetailData> {
    override suspend fun preload(context: RouterContext): UserDetailData {
        // 模拟网络请求耗时 2 秒
        delay(2000)
        // 模拟抛出网络异常，让业务层 DemoPreloadScreen 展示加载失败的 UI
        throw RuntimeException("网络请求超时，请检查您的网络连接")
        // 加载成功，显示正常数据
        // return UserDetailData(name = "天衢协程预加载", desc = "我是和动画一起并发加载回来的数据！")
    }
}

@Router(path = "/demo_preload")
@Composable
fun DemoPreloadScreen(context: RouterContext) {
    val navigator = LocalNavigator.current
    
    // 1. 获取绑定到当前路由节点生命周期的 ViewModel
    val viewModel = tianquViewModel<DemoPreloadViewModel>()
    
    // 2. 从框架中获取预加载的数据状态
    val preloadedResult by rememberPreloadData<UserDetailData>()

    // 3. 监听预加载结果，一旦有数据就交给 ViewModel 处理
    LaunchedEffect(preloadedResult) {
        preloadedResult?.let { result ->
            // 将预加载的数据喂给 ViewModel，由 ViewModel 统一管理状态
            viewModel.onPreloadResult(result)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // 4. UI 层只负责观察 ViewModel 的状态并渲染
            when (val state = viewModel.uiState) {
                is DemoPreloadViewModel.UiState.Loading -> {
                    // 如果页面动画播完了（比如只有 300ms），但网络请求（2s）还没回来，就会显示这个
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("页面已极速打开，正在等待并发网络数据...")
                }
                is DemoPreloadViewModel.UiState.Success -> {
                    Text("🎉 加载成功！")
                    Text("姓名: ${state.data.name}")
                    Text("描述: ${state.data.desc}")
                }
                is DemoPreloadViewModel.UiState.Error -> {
                    Text("❌ 加载失败！")
                    Text("错误信息: ${state.message}", color = androidx.compose.ui.graphics.Color.Red)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.retry() }) {
                        Text("点击重试 (ViewModel 发起)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { navigator.popBackStack() }) {
                Text("返回")
            }
        }
    }
}
