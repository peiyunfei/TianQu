package shijing.tianqu.runtime

import kotlin.reflect.KClass

/**
 * 模块间通信核心管理类。
 * 用于实现模块间的依赖解耦（Service Provider 模式）。
 *
 * 框架在编译期会扫描所有被 `@Service` 注解标记的类，并将它们与其实现的接口关联起来生成 `ServiceRegistry`。
 * 运行时通过调用 `ServiceManager.init(ServiceRegistry.services)` 初始化。
 *
 * 初始化之后，任意模块都可以通过 `ServiceManager.getService<YourInterface>()` 获取到该接口的实现实例。
 */
object ServiceManager {
    // 缓存已经实例化的 Service 对象，保证单例
    private val instances = mutableMapOf<KClass<*>, Any>()
    
    // 存储接口与其实现类工厂方法的映射
    private val providers = mutableMapOf<KClass<*>, () -> Any>()

    /**
     * 初始化 ServiceManager，注入 KSP 生成的提供者映射表。
     *
     * @param registryProviders 接口与其实现工厂的映射关系
     */
    fun init(registryProviders: Map<KClass<*>, () -> Any>) {
        providers.putAll(registryProviders)
    }

    /**
     * 获取指定接口的服务实现实例。
     *
     * @param T 需要获取的服务接口类型
     * @return 如果找到了对应的实现类并实例化成功，则返回该实例；否则返回 null
     */
    inline fun <reified T : Any> getService(): T? {
        return getService(T::class) as? T
    }

    /**
     * 获取指定接口的服务实现实例（非内联版本，供内部或其他语言平台使用）。
     *
     * @param clazz 服务的接口类型 [KClass]
     * @return 实例化后的服务对象，如果没有注册则返回 null
     */
    fun getService(clazz: KClass<*>): Any? {
        // 先从缓存中查找，如果已经实例化过直接返回
        if (instances.containsKey(clazz)) {
            return instances[clazz]
        }
        
        // 如果未实例化，查找其对应的工厂方法
        val provider = providers[clazz]
        if (provider != null) {
            // 这里的 provider 是一个无参 Lambda 函数 `() -> Any`，相当于对象工厂。
            // KSP 生成的代码形如： AnalyticsService::class to { AnalyticsServiceImpl() }
            // 调用 provider() 就会执行大括号里的代码，真正去 new 这个对象。
            // 这样做有两个核心优势：
            // 1. 延迟加载 (Lazy Loading)：只有在第一次被 getService 请求时才会创建对象，优化 App 启动速度和内存。
            // 2. 无反射 (Zero Reflection)：这是纯静态的普通方法调用，执行速度极快，且对代码混淆 (R8/ProGuard) 绝对安全。
            val instance = provider()
            
            // 将创建好的实例缓存起来，保证单例
            instances[clazz] = instance
            return instance
        }
        
        return null
    }
}
