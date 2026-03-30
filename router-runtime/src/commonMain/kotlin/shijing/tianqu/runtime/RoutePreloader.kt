package shijing.tianqu.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.Deferred
import shijing.tianqu.router.RouterContext

/**
 * 路由数据预加载器接口
 * 接入方可以实现该接口并在 Navigator 中注册，框架会在跳转时自动触发 preload 挂起函数。
 */
interface RoutePreloader<T> {
    suspend fun preload(context: RouterContext): T
}

/**
 * 提供给 Compose 环境，用于获取当前页面的 StackEntry
 */
val LocalStackEntry = compositionLocalOf<StackEntry?> { null }

/**
 * 在页面内获取预加载的数据
 * 如果配置了该路由的 preloader，则在进入页面后自动 await 等待数据。
 * @return 带有状态的数据。如果还在加载或者异常则为 null。
 */
@Composable
inline fun <reified T> rememberPreloadData(): State<T?> {
    val entry = LocalStackEntry.current
    val deferred = entry?.preloaderDeferred as? Deferred<T>
    val state = remember { mutableStateOf<T?>(null) }

    LaunchedEffect(deferred) {
        if (deferred != null) {
            try {
                state.value = deferred.await()
            } catch (e: Exception) {
                // Ignore or log error
                e.printStackTrace()
            }
        }
    }
    return state
}
