package shijing.tianqu.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import shijing.tianqu.router.Router
import shijing.tianqu.runtime.LocalNavigator
import shijing.tianqu.runtime.tianquViewModel

class CounterViewModel : ViewModel() {
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    init {
        println("CounterViewModel initialized")
    }

    fun increment() {
        _count.value++
    }

    override fun onCleared() {
        super.onCleared()
        println("CounterViewModel onCleared - 这个页面已被销毁！")
    }
}

@Router(path = "/demo_viewmodel", transition = "Slide")
@Composable
fun DemoViewModelScreen() {
    val navigator = LocalNavigator.current
    
    // 使用 tianquViewModel() 获取与当前路由页面生命周期绑定的 ViewModel
    // 当这个页面被弹栈(pop)时，CounterViewModel 的 onCleared() 将会被自动调用！
    val viewModel = tianquViewModel<CounterViewModel>()
    
    val count by viewModel.count.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("当前计数: $count")
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = { viewModel.increment() }) {
            Text("增加计数")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(onClick = { navigator.popBackStack() }) {
            Text("返回上一页（并销毁 ViewModel）")
        }
    }
}
