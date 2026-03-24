package shijing.tianqu.runtime.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import shijing.tianqu.runtime.service.CoroutineServiceManager

/**
 * 带有协程挂起功能的 Compose 扩展，
 * 用于在 UI 中优雅地获取可能需要异步初始化的 Service。
 */
@Composable
inline fun <reified T : Any> rememberService(): T? {
    var service by remember { mutableStateOf<T?>(null) }
    
    LaunchedEffect(Unit) {
        // 使用协程异步挂起等待 Service 初始化完成
        service = CoroutineServiceManager.awaitService<T>()
    }
    
    return service
}
