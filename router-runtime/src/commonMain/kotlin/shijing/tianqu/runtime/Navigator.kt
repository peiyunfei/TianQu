package shijing.tianqu.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import shijing.tianqu.router.GuardChain
import shijing.tianqu.router.RouteContext
import shijing.tianqu.router.RouteGuard
import shijing.tianqu.router.RouteTransition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 导航动作类型
 */
enum class NavigationAction {
    IDLE, PUSH, POP, POP_TO_ROOT, POP_UNTIL, REPLACE, CLEAR_AND_PUSH
}

/**
 * 路由节点数据模型，由 KSP 处理器生成
 */
data class RouteNode(
    val path: String,
    val regexPattern: String,
    val enterTransition: RouteTransition,
    val exitTransition: RouteTransition,
    val composable: @Composable (RouteContext) -> Unit
)

/**
 * 导航记录栈实体
 */
data class StackEntry(
    val node: RouteNode,
    val context: RouteContext
)

/**
 * 导航器核心类，负责管理应用内的页面栈、URL解析和路由分发。
 * 它持有由 KSP 生成的路由节点注册表，并在导航过程中执行路由守卫的校验。
 *
 * @param routeRegistry 包含所有通过 @Router 注解生成的路由节点列表
 * @param guards 注册在当前导航器中的路由守卫列表，用于在导航跳转前进行拦截验证
 * @param onRouteNotFound 当路由未找到时的降级回调
 * @param parent 父导航器，用于嵌套路由时向上传递返回事件
 * @param coroutineScope 用于执行异步路由守卫的协程作用域
 */
class Navigator(
    private val routeRegistry: List<RouteNode>,
    private val guards: List<RouteGuard> = emptyList(),
    private val onRouteNotFound: ((String) -> Unit)? = null,
    val parent: Navigator? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    /**
     * 当前应用的导航回退栈，保存了已访问页面的节点信息和对应的上下文数据
     */
    val backStack = mutableStateListOf<StackEntry>()
    
    // 记录最后一次导航动作，以便在过渡动画中准确判断是进入还是退出
    var lastAction by mutableStateOf(NavigationAction.IDLE)
        private set

    /**
     * 准备路由上下文并匹配路由节点
     */
    private fun prepareContext(url: String, extra: Any? = null): Pair<RouteNode, RouteContext>? {
        val (pathPart, queryPart) = parseUrl(url)
        
        val matchedNode = findMatchedNode(pathPart)
        if (matchedNode == null) {
            println("Route not found for url: $url")
            onRouteNotFound?.invoke(url)
            return null
        }

        val pathParams = extractPathParams(matchedNode.path, matchedNode.regexPattern, pathPart)
        val queryParams = parseQuery(queryPart)
        
        val context = RouteContext(
            url = url,
            pathParams = pathParams,
            queryParams = queryParams,
            extra = extra
        )
        return Pair(matchedNode, context)
    }

    /**
     * 导航到指定 URL（支持深层链接，例如 /user/123?ref=home）
     * 不能直接传递 Map 参数，强制基于 URL
     *
     * @param url 目标路由URL
     * @param extra 额外传递的复杂对象参数（不适合放在URL中的数据）
     */
    fun navigateTo(url: String, extra: Any? = null) {
        val match = prepareContext(url, extra) ?: return
        val (matchedNode, context) = match

        coroutineScope.launch {
            if (!processGuards(context)) {
                println("Navigation blocked by RouteGuard for url: $url")
                return@launch
            }
            lastAction = NavigationAction.PUSH
            backStack.add(StackEntry(matchedNode, context))
        }
    }

    /**
     * 替换当前页面：弹出栈顶页面，并把新页面压入栈顶，不产生新的历史记录
     */
    fun replace(url: String, extra: Any? = null) {
        val match = prepareContext(url, extra) ?: return
        val (matchedNode, context) = match

        coroutineScope.launch {
            if (!processGuards(context)) {
                println("Navigation blocked by RouteGuard for url: $url")
                return@launch
            }
            lastAction = NavigationAction.REPLACE
            if (backStack.isNotEmpty()) {
                backStack.removeAt(backStack.lastIndex)
            }
            backStack.add(StackEntry(matchedNode, context))
        }
    }

    /**
     * 清空所有路由栈并跳转到一个新页面（常用于退出登录后跳转到登录页）
     */
    fun clearAndPush(url: String, extra: Any? = null) {
        val match = prepareContext(url, extra) ?: return
        val (matchedNode, context) = match

        coroutineScope.launch {
            if (!processGuards(context)) {
                println("Navigation blocked by RouteGuard for url: $url")
                return@launch
            }
            lastAction = NavigationAction.CLEAR_AND_PUSH
            backStack.clear()
            backStack.add(StackEntry(matchedNode, context))
        }
    }

    /**
     * 执行路由守卫责任链
     */
    private suspend fun processGuards(context: RouteContext): Boolean {
        // 只筛选出匹配当前路由的守卫
        val matchedGuards = guards.filter { it.matches(context) }
        if (matchedGuards.isEmpty()) return true

        var index = 0
        var isAllowed = true

        // 定义责任链内部实现
        val chain = object : GuardChain {
            override suspend fun proceed(ctx: RouteContext): Boolean {
                if (index < matchedGuards.size) {
                    val guard = matchedGuards[index++]
                    return guard.canActivate(ctx, this)
                }
                return true
            }
        }

        // 开始执行第一层守卫
        isAllowed = chain.proceed(context)
        return isAllowed
    }

    /**
     * 出栈，返回上一级页面
     *
     * @param result 返回给上一级页面的结果数据。上一级页面可以从 RouteContext.result 中读取该值。
     */
    fun popBackStack(result: Any? = null) {
        if (backStack.size > 1) {
            lastAction = NavigationAction.POP
            backStack.removeAt(backStack.lastIndex)
            
            // 如果传递了 result 且栈中还有页面，将 result 塞给新的栈顶页面
            if (result != null && backStack.isNotEmpty()) {
                val previousEntry = backStack.last()
                val updatedContext = previousEntry.context.copy(result = result)
                backStack[backStack.lastIndex] = previousEntry.copy(context = updatedContext)
            }
        } else {
            // 如果当前导航器已经到底了，则将 pop 动作交给父导航器（支持嵌套路由返回）
            parent?.popBackStack(result)
        }
    }
    
    fun popToRoot() {
        if (backStack.size > 1) {
            lastAction = NavigationAction.POP_TO_ROOT
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
                backStack.removeAt(backStack.lastIndex)
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

    /**
     * URL 解析工具
     * @return Pair(path, query)
     */
    private fun parseUrl(url: String): Pair<String, String> {
        // 处理深层链接（去掉 scheme 和 host，例如 app://example.com/user/123 -> /user/123）
        var processedUrl = url
        if (processedUrl.contains("://")) {
            val uriWithoutScheme = processedUrl.substringAfter("://")
            processedUrl = "/" + uriWithoutScheme.substringAfter("/")
        }

        val parts = processedUrl.split("?", limit = 2)
        val path = parts[0]
        val query = if (parts.size > 1) parts[1] else ""
        return Pair(path, query)
    }

    /**
     * 解析 Query 字符串
     */
    private fun parseQuery(query: String): Map<String, List<String>> {
        if (query.isEmpty()) return emptyMap()
        val result = mutableMapOf<String, MutableList<String>>()
        query.split("&").forEach {
            val kv = it.split("=", limit = 2)
            if (kv.isNotEmpty()) {
                val key = kv[0]
                val value = if (kv.size > 1) kv[1] else ""
                result.getOrPut(key) { mutableListOf() }.add(value)
            }
        }
        return result
    }

    /**
     * 查找匹配的路由节点
     */
    private fun findMatchedNode(path: String): RouteNode? {
        return routeRegistry.find { node ->
            Regex(node.regexPattern).matches(path)
        }
    }

    /**
     * 提取路径参数
     */
    private fun extractPathParams(template: String, regexPattern: String, actualPath: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        
        // 提取模板中的参数名 (例如 "/user/{id}" -> ["id"])
        val paramNames = Regex("\\{([^/]+)\\}").findAll(template).map { it.groupValues[1] }.toList()
        if (paramNames.isEmpty()) return result
        
        // 使用正则提取实际值
        val matchResult = Regex(regexPattern).matchEntire(actualPath)
        if (matchResult != null) {
            // groupValues 的第 0 个元素是完整匹配，从第 1 个开始是捕获组
            val values = matchResult.groupValues.drop(1)
            paramNames.forEachIndexed { index, name ->
                if (index < values.size) {
                    result[name] = values[index]
                }
            }
        }
        
        return result
    }
}