package shijing.tianqu.runtime.service

import kotlin.reflect.KClass

/**
 * 协程服务管理器兼容层。
 *
 * 该对象作为 Facade，统一转发到 [ServiceManager]，
 * 保持已有调用方不需要改动。
 */
object CoroutineServiceManager {

    /**
     * 协程挂起获取服务。
     */
    suspend inline fun <reified T : Any> awaitService(): T? {
        return ServiceManager.awaitService<T>()
    }

    /**
     * 协程挂起获取服务（非内联版本）。
     */
    suspend fun <T : Any> awaitService(clazz: KClass<T>): T? {
        return ServiceManager.awaitService(clazz)
    }
}