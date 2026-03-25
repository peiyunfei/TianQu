package shijing.tianqu.runtime

import shijing.tianqu.router.RouterContext
import shijing.tianqu.runtime.async.DeferredAsyncResult

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

/**
 * 路由栈实体
 */
data class StackEntry(
    val node: RouterNode,
    val context: RouterContext,
    val result: DeferredAsyncResult<Any?>? = null,
    val id: String = generateEntryId()
) {
    // 为每个页面绑定独立的协程作用域，出栈时取消
    var scope: CoroutineScope? = null
        internal set

    // 页面销毁时的清理回调
    private val disposeCallbacks = mutableListOf<() -> Unit>()

    fun onDispose(callback: () -> Unit) {
        disposeCallbacks.add(callback)
    }

    internal fun dispose() {
        scope?.cancel()
        disposeCallbacks.forEach { it() }
        disposeCallbacks.clear()
    }

    companion object {
        private var idCounter = 0
        private fun generateEntryId(): String {
            return "entry_${idCounter++}_${kotlin.random.Random.nextInt(10000)}"
        }
    }
}
