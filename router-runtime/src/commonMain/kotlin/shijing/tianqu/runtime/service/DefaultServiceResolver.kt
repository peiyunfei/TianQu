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

    // 存储接口与其实现类的"工厂方法"映射，来源于 KSP 自动生成
    private val providers = mutableMapOf<KClass<*>, () -> Any>()
    
    // 互斥锁，保证在并发获取同一个 Service 时，工厂只被调用一次
    private val mutex = Mutex()

    // 专属的协程作用域，运行在默认后台线程池 (Dispatchers.Default)，SupervisorJob 确保一个子协程失败不影响其他协程
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
            // 使用 Mutex 互斥锁包围核心的"获取或创建"逻辑，保证绝对的线程安全
            val deferred = mutex.withLock {
                // 如果 deferredInstances 里没有该类，就执行大括号里的逻辑创建一个新的 Deferred 放入
                deferredInstances.getOrPut(clazz) {
                    // 二次检查同步缓存 (Double Check Locking 思想)
                    if (syncInstances.containsKey(clazz)) {
                        CompletableDeferred(syncInstances[clazz])
                    } else {
                        val provider = providers[clazz]
                        if (provider != null) {
                            // 启动一个新的后台异步任务(async)去实例化对象
                            scope.async {
                                val instance = provider()
                                syncInstances[clazz] = instance
                                instance // 返回结果
                            }
                        } else {
                            // 如果找不到服务，返回一个直接完结的 Deferred(null)
                            CompletableDeferred<Any?>().apply { complete(null) }
                        }
                    }
                }
            }
            
            try {
                @Suppress("UNCHECKED_CAST")
                // 等待对象创建完成（如果已经在创建中，这里会挂起等待；如果已经创建完，立刻继续）
                val resolvedInstance = deferred.await() as? T
                // 将真实实例通过 complete 推给调用方
                result.complete(resolvedInstance)
            } catch (e: Exception) {
                result.complete(null)
            }
        }
        return result
    }
}
