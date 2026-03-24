package shijing.tianqu.runtime.async

/**
 * 异步结果的统一访问接口（基于外观与桥接模式）。
 * 既支持传统的 Callback 回调模式（观察者模式），也支持 Kotlin 的协程挂起模式。
 * 将各种异步行为（页面跳转结果等待、服务异步加载等）收敛到这一统一接口上。
 */
interface AsyncResult<T> {
    /**
     * 协程方式：挂起当前协程直至结果返回
     */
    suspend fun await(): T

    /**
     * 常规回调方式：注册一个监听器，在结果返回时被调用
     * (单次观察者模式)
     */
    fun onResult(callback: (T) -> Unit)
}
