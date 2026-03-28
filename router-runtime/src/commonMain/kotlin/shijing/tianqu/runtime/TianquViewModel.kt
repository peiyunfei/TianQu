package shijing.tianqu.runtime

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * 获取与当前页面 (StackEntry) 生命周期绑定的 ViewModel。
 *
 * 它的作用域被限制在当前的路由节点中。当该页面被从导航栈（Navigator.backStack）
 * 以及后台缓存栈中彻底移除时，该 ViewModel 的 onCleared() 将被自动调用，释放相关资源。
 */
@Composable
inline fun <reified T : ViewModel> tianquViewModel(): T {
    // 由于我们在 ScreenRouterRenderer 和 DialogRouterRenderer 中
    // 已经通过 CompositionLocalProvider(LocalViewModelStoreOwner provides entry)
    // 将当前的 StackEntry 提供为了 ViewModelStoreOwner，
    // 这里直接调用官方跨平台的 viewModel() 即可无缝实现生命周期绑定。
    return viewModel<T>()
}
