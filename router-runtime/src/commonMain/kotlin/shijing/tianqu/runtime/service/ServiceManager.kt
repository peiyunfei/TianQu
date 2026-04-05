package shijing.tianqu.runtime.service

import kotlin.reflect.KClass

/**
 * 服务管理器（Facade 门面）。
 *
 * 对外提供统一的静态入口，内部将所有请求委托给 [ServiceResolver]（默认为 [DefaultServiceResolver]）。
 * 既支持同步获取 [getService]，也支持协程异步获取 [awaitService]。
 *
 * 用法：
 * - 同步获取：`ServiceManager.getService<YourInterface>()`
 * - 异步获取：`ServiceManager.awaitService<YourInterface>()`
 *
 * 可通过 [setResolver] 替换底层实现（例如测试时注入 mock）。
 */
object ServiceManager {

    // 底层真实解析器，默认为 DefaultServiceResolver
    private var resolver: ServiceResolver = DefaultServiceResolver()

    /**
     * 替换底层服务解析器（方便测试或定制扩展）。
     */
    fun setResolver(customResolver: ServiceResolver) {
        resolver = customResolver
    }

    /**
     * 初始化服务提供者映射表。
     */
    fun init(registryProviders: Map<KClass<*>, () -> Any>) {
        resolver.init(registryProviders)
    }

    /**
     * 同步获取指定接口的服务实现实例（内联版本）。
     * 开发者调用时不需要传 T::class，直接写 ServiceManager.getService<UserService>()
     */
    inline fun <reified T : Any> getService(): T? {
        return getService(T::class) as? T
    }

    /**
     * 同步获取指定接口的服务实现实例。
     */
    fun getService(clazz: KClass<*>): Any? {
        return resolver.getService(clazz)
    }

    /**
     * 协程挂起获取指定接口的服务实现实例（内联版本）。
     */
    suspend inline fun <reified T : Any> awaitService(): T? {
        return awaitService(T::class)
    }

    /**
     * 协程挂起获取指定接口的服务实现实例。
     */
    suspend fun <T : Any> awaitService(clazz: KClass<T>): T? {
        return resolver.awaitService(clazz)
    }
}
