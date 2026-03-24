package shijing.tianqu.runtime

import kotlinx.coroutines.awaitCancellation

/**
 * 自定义的一个简易版 SharedFlow，完全基于协程。
 * 不直接依赖 kotlinx.coroutines.flow.SharedFlow，提供一个轻量级的事件总线替代方案。
 */
interface SimpleSharedFlow<T> {
    /**
     * 收集事件，此方法会一直挂起，直到所在的协程作用域被取消。
     */
    suspend fun collect(collector: suspend (T) -> Unit): Nothing
}

class MutableSimpleSharedFlow<T> : SimpleSharedFlow<T> {
    private val collectors = mutableListOf<suspend (T) -> Unit>()

    override suspend fun collect(collector: suspend (T) -> Unit): Nothing {
        collectors.add(collector)
        try {
            // 挂起当前协程，直到作用域被取消时抛出 CancellationException
            awaitCancellation()
        } finally {
            // 协程取消时，自动移除收集器，避免内存泄漏
            collectors.remove(collector)
        }
    }

    suspend fun emit(value: T) {
        // 复制一份快照进行遍历，防止在发射事件的过程中有收集器被移除导致并发修改异常 (ConcurrentModificationException)
        val snapshot = collectors.toList()
        snapshot.forEach { it(value) }
    }
}
