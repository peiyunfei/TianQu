package shijing.tianqu.runtime

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import shijing.tianqu.router.RouterContext
import shijing.tianqu.runtime.async.DeferredAsyncResult

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.Deferred

/**
 * 路由栈实体，实现 ViewModelStoreOwner 接口。
 * 极其重要！实现了这个接口，StackEntry 就拥有了掌管 ViewModel 生杀大权的能力。
 */
data class StackEntry(

    // 包含路由路径、匹配正则和真正的 Composable 函数的静态节点（来自路由表）
    val node: RouterNode,

    // 用户跳转时携带的动态数据（例如 /user/123 解析出来的 123 参数，或者额外传递的 extra 对象）
    // 为了支持 SingleTop/SingleTask 传递新参数时能触发 Compose 重组，将 context 包装为状态
    var _context: RouterContext,

    // 记录是否是因 SingleTop/SingleTask 触发的更新
    var isNewIntent: Boolean = false,

    // 用于处理带有返回值的页面跳转（类似 startActivityForResult）
    val result: DeferredAsyncResult<Any?>? = null,

    // 为每次页面跳转生成一个全局唯一的 ID，用于 SaveableStateHolder 的状态保存
    val id: String = generateEntryId(),

    // 支持页面在做进场动画时，提前并行拉取数据的异步任务
    val preloaderDeferred: Deferred<Any?>? = null

) : ViewModelStoreOwner {

    // 暴露可变的并且支持 Compose 监听的状态属性
    var context: RouterContext by mutableStateOf(_context)

    // 延迟初始化 _viewModelStore。ViewModelStore 是官方提供的容器，本质上是一个 HashMap<String, ViewModel>。
    private var _viewModelStore: ViewModelStore? = null

    // 重写 ViewModelStoreOwner 接口的方法，返回专属的 ViewModel 仓库。
    // 按需创建。如果这个页面非常简单，没有调用 ViewModel，就不占用内存创建 Store。
    override val viewModelStore: ViewModelStore
        get() {
            if (_viewModelStore == null) {
                _viewModelStore = ViewModelStore()
            }
            return _viewModelStore!!
        }

    // 回调列表：允许开发者在页面彻底销毁时，注册一些自定义的清理逻辑。
    private val disposeCallbacks = mutableListOf<() -> Unit>()

    fun onDispose(callback: () -> Unit) {
        disposeCallbacks.add(callback)
    }

    // 页面走向生命终点时的“收尸”操作（由 Navigator 在 popBackStack 时调用）。
    internal fun dispose() {
        // 极其关键！调用 viewModelStore 的 clear() 方法。
        // 这会遍历此页面所有的 ViewModel 实例，并触发它们的 onCleared() 方法，让业务层有机会释放网络、数据库连接。
        _viewModelStore?.clear()

        // 修复：如果用户在数据预加载完成前极速退出页面，应该立即取消后台请求，避免协程泄漏、网络资源浪费。
        preloaderDeferred?.cancel()

        // 依次执行自定义回调，并清空列表。
        disposeCallbacks.forEach { it() }
        disposeCallbacks.clear()
    }

    companion object {
        private var idCounter = 0
        // 全局唯一的 ID 生成器，由计数器和随机数拼接，确保即使用户疯狂进出同一个页面，每次生成的 ID 也绝对不会重复。
        private fun generateEntryId(): String {
            return "entry_${idCounter++}_${kotlin.random.Random.nextInt(10000)}"
        }
    }
}
