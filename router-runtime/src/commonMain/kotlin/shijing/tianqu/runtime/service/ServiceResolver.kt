package shijing.tianqu.runtime.service

import shijing.tianqu.runtime.async.AsyncResult
import kotlin.reflect.KClass

/**
 * 服务解析器接口。
 * 定义了统一的服务注册和获取标准。
 */
interface ServiceResolver {
    
    /**
     * 初始化服务提供者映射表。
     */
    fun init(registryProviders: Map<KClass<*>, () -> Any>)

    /**
     * 同步获取指定接口的服务实现实例。
     */
    fun getService(clazz: KClass<*>): Any?

    /**
     * 异步获取指定接口的服务实现实例。
     * @return 返回支持回调和挂起的 AsyncResult 适配器接口。
     */
    fun <T : Any> getServiceAsync(clazz: KClass<T>): AsyncResult<T?>

    /**
     * 异步挂起获取指定接口的服务实现实例。
     */
    suspend fun <T : Any> awaitService(clazz: KClass<T>): T? = getServiceAsync(clazz).await()
}
