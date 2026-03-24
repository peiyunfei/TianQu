package shijing.tianqu.runtime.async

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * 基于 CompletableDeferred 的异步结果适配器（适配器模式 Adapter Pattern）。
 * 内部将协程的 deferred 机制适配转换为更通用的 [AsyncResult] 协议。
 */
class DeferredAsyncResult<T> : AsyncResult<T> {
    private val deferred = CompletableDeferred<T>()
    private var callback: ((T) -> Unit)? = null

    /**
     * 在内部逻辑（如服务初始化完毕、或者目标页面 popBackStack）完成时调用。
     */
    fun complete(value: T) {
        deferred.complete(value)
        callback?.invoke(value)
    }

    override suspend fun await(): T {
        return try {
            deferred.await()
        } catch (e: kotlinx.coroutines.CancellationException) {
            // 协程被取消（如页面销毁），这里可以按需处理，为了不中断上层协程传播，重新抛出
            throw e
        }
    }

    override fun onResult(callback: (T) -> Unit) {
        this.callback = callback
        if (deferred.isCompleted) {
            // 已经拿到结果了，直接给出回调，避免事件丢失
            callback.invoke(deferred.getCompleted())
        }
    }
}
