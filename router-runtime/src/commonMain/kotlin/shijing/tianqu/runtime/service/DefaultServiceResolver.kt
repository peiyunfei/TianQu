package shijing.tianqu.runtime.service

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import shijing.tianqu.runtime.async.AsyncResult
import shijing.tianqu.runtime.async.DeferredAsyncResult
import kotlin.reflect.KClass

/**
 * 默认的服务解析器实现（服务定位器模式核心）。
 *
 * 负责维护：
 * 1. 实例缓存（同步缓存与协程 Deferred 缓存）
 * 2. 多线程安全的初始化（Mutex）
 * 3. 后台异步创建逻辑（CoroutineScope.async）
 */
class DefaultServiceResolver : ServiceResolver {

    // 缓存已经实例化的 Service 对象的 Deferred（用于协程等待）
    private val deferredInstances = mutableMapOf<KClass<*>, Deferred<Any?>>()
    
    // 缓存已经实例化完成的 Service 对象（用于同步快速获取）
    private val syncInstances = mutableMapOf<KClass<*>, Any>()

    // 存储接口与其实现类工厂方法的映射
    private val providers = mutableMapOf<KClass<*>, () -> Any>()
    
    // 互斥锁，保证在并发获取同一个 Service 时，工厂只被调用一次
    private val mutex = Mutex()

    // 用于后台异步初始化服务的协程作用域
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun init(registryProviders: Map<KClass<*>, () -> Any>) {
        providers.putAll(registryProviders)
    }

    override fun getService(clazz: KClass<*>): Any? {
        if (syncInstances.containsKey(clazz)) {
            // 先从缓存中查找，如果已经实例化过直接返回
            return syncInstances[clazz]
        }
        val provider = providers[clazz]
        if (provider != null) {
            // 这里的 provider 是一个无参 Lambda 函数 `() -> Any`，相当于对象工厂。
            // KSP 生成的代码形如： AnalyticsService::class to { AnalyticsServiceImpl() }
            // 调用 provider() 就会执行大括号里的代码，真正去 new 这个对象。
            // 这样做有两个核心优势：
            // 1. 延迟加载 (Lazy Loading)：只有在第一次被 getService 请求时才会创建对象，优化 App 启动速度和内存。
            // 2. 无反射 (Zero Reflection)：这是纯静态的普通方法调用
            val instance = provider()
            syncInstances[clazz] = instance
            // 同步创建完成后，也要把 Deferred 状态同步，以防有协程在等待
            deferredInstances[clazz] = CompletableDeferred(instance)
            return instance
        }

        return null
    }

    override fun <T : Any> getServiceAsync(clazz: KClass<T>): AsyncResult<T?> {
        val result = DeferredAsyncResult<T?>()
        
        scope.launch {
            val deferred = mutex.withLock {
                deferredInstances.getOrPut(clazz) {
                    if (syncInstances.containsKey(clazz)) {
                        CompletableDeferred(syncInstances[clazz])
                    } else {
                        val provider = providers[clazz]
                        if (provider != null) {
                            scope.async {
                                val instance = provider()
                                syncInstances[clazz] = instance
                                instance
                            }
                        } else {
                            CompletableDeferred<Any?>().apply { complete(null) }
                        }
                    }
                }
            }
            
            try {
                @Suppress("UNCHECKED_CAST")
                val resolvedInstance = deferred.await() as? T
                result.complete(resolvedInstance)
            } catch (e: Exception) {
                result.complete(null)
            }
        }
        return result
    }
}
