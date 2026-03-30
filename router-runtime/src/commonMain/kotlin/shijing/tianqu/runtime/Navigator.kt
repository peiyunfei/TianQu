package shijing.tianqu.runtime

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import shijing.tianqu.router.RouterContext
import shijing.tianqu.router.RouterGuard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.launch
import shijing.tianqu.runtime.async.AsyncResult
import shijing.tianqu.runtime.async.DeferredAsyncResult
import shijing.tianqu.runtime.handler.RouterHandler
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

/**
 * 导航器核心类，负责管理应用内的页面栈。
 * 它持有由 KSP 生成的路由节点注册表，并在导航过程中协调解析器、拦截器和栈管理。
 *
 * @param routeRegistry 包含所有通过 @Router 注解生成的路由节点列表
 * @param guards 注册在当前导航器中的路由守卫列表，用于在导航跳转前进行拦截验证
 * @param routerHandler 页面不存在，进行降级处理
 * @param parent 父导航器，用于嵌套路由时向上传递返回事件
 * @param coroutineScope 用于执行异步路由守卫的协程作用域
 */
class Navigator(
    private val routeRegistry: List<RouterNode>,
    private val guards: List<RouterGuard> = emptyList(),
    private val routerHandler: RouterHandler? = null,
    val parent: Navigator? = null,
    val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    preloaders: Map<String, RoutePreloader<*>> = emptyMap()
) : AbstractCoroutineContextElement(Navigator) {

    /**
     * 实现 CoroutineContext.Element，允许在协程上下文中直接获取 Navigator
     * 类似 Compose 中的 Recomposer.kt 或者 AndroidUiDispatcher
     */
    companion object Key : CoroutineContext.Key<Navigator>

    /**
     * 当前应用的导航回退栈，保存了已访问页面的节点信息和对应的上下文数据
     */
    val backStack = mutableStateListOf<StackEntry>()
    
    /**
     * 用于保存被切走的后备栈（支持多返回栈 / Tab 切换）
     */
    private val savedStateStacks = mutableMapOf<String, List<StackEntry>>()
    
    // 记录最后一次导航动作，以便在过渡动画中准确判断是进入还是退出
    var lastAction by mutableStateOf(NavigationAction.IDLE)
        private set
        
    // 路由数据预加载器注册表
    private val preloaders = mutableMapOf<String, RoutePreloader<*>>().apply {
        putAll(preloaders)
    }

    /**
     * 为指定路由注册一个预加载器。在导航到该路由时，框架会在后台自动并发调用预加载器。
     * @param path 路由路径，例如 "/user/{id}"
     * @param preloader 预加载器实现
     */
    fun <T> registerPreloader(path: String, preloader: RoutePreloader<T>) {
        preloaders[path] = preloader
    }

    fun <T> registerPreloader(map: MutableMap<String, RoutePreloader<T>>) {
        preloaders.putAll(map)
    }

    // 基于协程挂起和恢复特性的自定义简易 SharedFlow 事件总线
    private val _routeEvents = MutableSimpleSharedFlow<RouterEvent>()

    /**
     * 路由事件流，外部通过 collect 订阅事件。
     * 使用自定义的 SimpleSharedFlow 替代 kotlinx SharedFlow，
     * 核心原理是 awaitCancellation 挂起 + CancellationException 自动移除订阅。
     */
    val routeEvents: SimpleSharedFlow<RouterEvent> = _routeEvents
    
    /**
     * 判断指定的页面实例是否仍然存活在导航器中（包括当前的返回栈以及保存在后台的多 Tab 栈）。
     * 用于在 Compose 节点销毁时判断是否彻底清理 SaveableState。
     */
    fun isEntryAlive(entry: StackEntry): Boolean {
        if (backStack.contains(entry)) return true
        for (stack in savedStateStacks.values) {
            if (stack.contains(entry)) return true
        }
        return false
    }

    private suspend fun emitEvent(event: RouterEvent) {
        _routeEvents.emit(event)
    }

    /**
     * 准备路由上下文并匹配路由节点
     */
    private fun prepareContext(url: String, extra: Any? = null): Pair<RouterNode?, RouterContext> {
        val (pathPart, queryPart) = UrlParser.parseUrl(url)
        val matchedNode = findMatchedNode(pathPart)
        val pathParams = matchedNode?.let { UrlParser.extractPathParams(it.path, it.regexPattern, pathPart) } ?: emptyMap()
        val queryParams = UrlParser.parseQuery(queryPart)
        
        val context = RouterContext(
            url = url,
            pathParams = pathParams,
            queryParams = queryParams,
            extra = extra
        )
        return Pair(matchedNode, context)
    }

    /**
     * 查找匹配的路由节点
     */
    private fun findMatchedNode(path: String): RouterNode? {
        return routeRegistry.find { node ->
            Regex(node.regexPattern).matches(path)
        }
    }

    /**
     * 核心导航逻辑：处理拦截器并入栈
     */
    private suspend fun processNavigation(
        url: String,
        extra: Any?,
        action: NavigationAction,
        result: DeferredAsyncResult<Any?>? = null,
        saveState: Boolean = false,
        restoreState: Boolean = false
    ): Boolean {
        val (matchedNode, context) = prepareContext(url, extra)

        if (matchedNode == null) {
            // 如果未匹配到 Compose 路由节点，尝试交给外部路由处理
            if (routerHandler?.handleExternalRoute(context, this) == true || parent?.routerHandler?.handleExternalRoute(context, this) == true) {
                // 外部路由处理了该事件，发送导航成功事件 (External)
                coroutineScope.launch { emitEvent(RouterEvent.Navigated(action, url)) }
                return true
            }
            val handler = routerHandler?.handleNotFound(url, this) ?: parent?.routerHandler?.handleNotFound(url, this) ?: false
            if (handler) {
                // 已经处理了
                return true
            }
            // 直接发送页面不存在事件出去
            coroutineScope.launch { emitEvent(RouterEvent.NotFound(url)) }
            return false
        }

        if (!GuardProcessor.processGuards(context, guards)) {
            return false
        }

        lastAction = action

        // 处理多返回栈的切换状态
        if (saveState && backStack.isNotEmpty()) {
            val topId = backStack.first().node.path // 以当前栈的根节点作为标识保存
            savedStateStacks[topId] = backStack.toList()
            backStack.clear() // 这里的 clear 不会 dispose 内部的 Entry，以便之后恢复
        }

        if (restoreState) {
            val stateId = matchedNode.path
            val restored = savedStateStacks.remove(stateId)
            if (!restored.isNullOrEmpty()) {
                backStack.clear()
                backStack.addAll(restored)
                coroutineScope.launch { emitEvent(RouterEvent.Navigated(action, url)) }
                return true
            }
        }

        // 检查并触发预加载 (非阻塞式)
        var preloaderDeferred: Deferred<Any?>? = null
        val preloader = preloaders[matchedNode.path]
        if (preloader != null) {
            preloaderDeferred = coroutineScope.async {
                runCatching {
                    preloader.preload(context)
                }.getOrNull()
            }
        }

        val entry = StackEntry(matchedNode, context, result, preloaderDeferred = preloaderDeferred)

        when (action) {
            NavigationAction.PUSH -> backStack.add(entry)
            NavigationAction.REPLACE -> {
                if (backStack.isNotEmpty()) {
                    val removed = backStack.removeAt(backStack.lastIndex)
                    removed.dispose()
                }
                backStack.add(entry)
            }
            NavigationAction.CLEAR_AND_PUSH -> {
                backStack.forEach { it.dispose() }
                backStack.clear()
                backStack.add(entry)
            }
            else -> {}
        }
        
        // 发送导航成功事件
        coroutineScope.launch {
            emitEvent(RouterEvent.Navigated(action, url))
        }
        
        return true
    }

    /**
     * 导航到指定 URL（支持深层链接，例如 /user/123?ref=home）
     * 不能直接传递 Map 参数，强制基于 URL
     *
     * @param url 目标路由URL
     * @param extra 额外传递的复杂对象参数（不适合放在URL中的数据）
     * @param saveState 是否保存当前栈的状态（适用于多 Tab 场景）
     * @param restoreState 是否尝试恢复目标栈的状态
     */
    fun navigateTo(url: String, extra: Any? = null, saveState: Boolean = false, restoreState: Boolean = false) {
        coroutineScope.launch {
            processNavigation(url, extra, NavigationAction.PUSH, null, saveState, restoreState)
        }
    }
    
    /**
     * 导航到指定 URL，支持以统一的异步接口形式返回结果。
     * 可以使用挂起函数 await() 或回调 onResult(callback) 获取结果。
     */
    fun navigateWithResult(url: String, extra: Any? = null): AsyncResult<Any?> {
        val result = DeferredAsyncResult<Any?>()
        coroutineScope.launch {
            val success = processNavigation(url, extra, NavigationAction.PUSH, result)
            if (!success) {
                // 如果导航失败，立即完成结果为 null
                result.complete(null)
            }
        }
        return result
    }

    /**
     * 导航到指定 URL，并挂起等待返回结果（类似 Compose 的 awaitFirstDown 等协程 API）。
     * 完全避免了在 State 中轮询或回调嵌套。
     *
     * @param url 目标路由URL
     * @param extra 额外参数
     * @return 目标页面通过 popBackStack 返回的数据，如果未返回或被拦截则返回 null
     */
    suspend fun awaitNavigateForResult(url: String, extra: Any? = null): Any? {
        return navigateWithResult(url, extra).await()
    }

    /**
     * 替换当前页面：弹出栈顶页面，并把新页面压入栈顶，不产生新的历史记录
     */
    fun replace(url: String, extra: Any? = null) {
        coroutineScope.launch {
            processNavigation(url, extra, NavigationAction.REPLACE, null)
        }
    }

    /**
     * 清空所有路由栈并跳转到一个新页面（常用于退出登录后跳转到登录页）
     */
    fun clearAndPush(url: String, extra: Any? = null) {
        coroutineScope.launch {
            processNavigation(url, extra, NavigationAction.CLEAR_AND_PUSH, null)
        }
    }

    fun pop() {
        popBackStack(null)
    }

    /**
     * 出栈，返回上一级页面
     *
     * @param result 返回给上一级页面的结果数据。被协程挂起等待的地方会收到该返回值。
     */
    fun popBackStack(result: Any? = null) {
        if (backStack.size > 1) {
            lastAction = NavigationAction.POP
            val entry = backStack.removeAt(backStack.lastIndex)
            entry.dispose() // 触发销毁回调
            
            // 恢复等待结果的协程，将结果返回
            entry.result?.complete(result)
        } else {
            // 如果当前导航器已经到底了，则将 pop 动作交给父导航器（支持嵌套路由返回）
            parent?.popBackStack(result)
        }
    }
    
    fun popToRoot() {
        if (backStack.size > 1) {
            lastAction = NavigationAction.POP_TO_ROOT
            
            // 如果出栈的页面有等待结果的，使用 null 结束等待，避免协程泄漏
            for (i in backStack.lastIndex downTo 1) {
                backStack[i].result?.complete(null)
                backStack[i].dispose()
            }
            
            val root = backStack.first()
            backStack.clear()
            backStack.add(root)
        }
    }

    /**
     * 一直出栈，直到遇到满足条件的页面
     * @param predicate 判断条件，返回 true 表示停止出栈
     */
    fun popUntil(predicate: (StackEntry) -> Boolean) {
        if (backStack.size <= 1) return // 至少保留一个根页面
        
        lastAction = NavigationAction.POP_UNTIL
        var foundIndex = -1
        for (i in backStack.lastIndex downTo 0) {
            if (predicate(backStack[i])) {
                foundIndex = i
                break
            }
        }
        
        if (foundIndex != -1 && foundIndex < backStack.lastIndex) {
            // 删除 foundIndex 之后的所有页面
            val removeCount = backStack.lastIndex - foundIndex
            for (i in 0 until removeCount) {
                val removed = backStack.removeAt(backStack.lastIndex)
                removed.result?.complete(null)
                removed.dispose()
            }
        } else if (foundIndex == -1) {
            // 如果没找到，默认弹到根节点
            popToRoot()
        }
    }

    /**
     * 一直出栈，直到遇到指定路径的页面
     * @param path 路由的 path，例如 "/home"
     */
    fun popUntil(path: String) {
        popUntil { it.node.path == path }
    }
}
