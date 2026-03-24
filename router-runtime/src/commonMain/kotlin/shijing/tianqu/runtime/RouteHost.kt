package shijing.tianqu.runtime

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import shijing.tianqu.router.RouteGuard
import shijing.tianqu.router.RouteTransition

import kotlinx.coroutines.CoroutineScope
import shijing.tianqu.runtime.handler.RouterHandler

/**
 * 获取包含当前 Navigator 的协程作用域。
 * 允许在任何挂起函数中通过 coroutineContext[Navigator] 拿到当前的导航器，
 */
@Composable
fun rememberRouterScope(): CoroutineScope {
    val navigator = LocalNavigator.current
    val baseScope = rememberCoroutineScope()
    return remember(baseScope, navigator) {
        CoroutineScope(baseScope.coroutineContext + navigator)
    }
}

/**
 * CompositionLocal 用于在组件树中向下传递 Navigator 实例，
 * 方便任意子组件通过 LocalNavigator.current 获取导航器并进行页面跳转。
 */
val LocalNavigator = compositionLocalOf<Navigator> {
    error("No Navigator provided. Make sure to wrap your content with RouterHost.")
}

/**
 * 记住并创建一个 Navigator 实例。
 * 它会在第一次组合时使用提供的路由表和守卫列表进行初始化，并自动导航到指定的起始路由。
 *
 * @param routes 由 KSP 生成的路由节点列表（通常为 RouteRegistry.routers）
 * @param startRoute 应用程序的初始路由地址（例如 "/home"）
 * @param guards 可选的路由守卫列表，可以通过重写 guard.matches 来实现局部拦截
 * @param parent 父级 Navigator（用于嵌套路由中的事件向上传递），默认为当前上下文中的 LocalNavigator
 * @return 返回管理导航栈状态的 Navigator 实例
 */
@Composable
fun rememberNavigator(
    routes: List<RouteNode>,
    startRoute: String,
    guards: List<RouteGuard> = emptyList(),
    routerHandler: RouterHandler? = null,
    parent: Navigator? = null
): Navigator {
    val coroutineScope = rememberCoroutineScope()
    
    // 改为 rememberSaveable 并自定义 Saver 以在配置更改时保持 Navigator 状态
    val navigator = remember(routes, guards, routerHandler, parent, coroutineScope) {
        Navigator(
            routeRegistry = routes,
            guards = guards,
            routerHandler = routerHandler,
            parent = parent,
            coroutineScope = coroutineScope
        )
    }
    
    LaunchedEffect(navigator, startRoute) {
        if (navigator.backStack.isEmpty()) {
            navigator.navigateTo(startRoute)
        }
    }
    return navigator
}

/**
 * 路由宿主容器 Composable
 *
 * @param navigator 导航器实例
 * @param modifier 修饰符
 */
@Composable
fun RouterHost(
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    println("yunfei RouterHost")
    val isPop = navigator.lastAction == NavigationAction.POP ||
                navigator.lastAction == NavigationAction.POP_TO_ROOT ||
                navigator.lastAction == NavigationAction.POP_UNTIL

    // 将 state holder 提升作用域或使用单一实例，确保 tab 切换等场景不被重置
    // 由于 RouterHost 通常在全局只挂载一次，这里的 rememberSaveableStateHolder 是安全的
    val saveableStateHolder = rememberSaveableStateHolder()
    
    // 维护一个记录所有出现过的 entry.id 的集合，用于比较哪些需要被清理
    val savedEntryIds = remember { mutableSetOf<String>() }

    // 监听返回栈的变化，清理已经被移出栈的页面保存的状态
    // 如果某个 entry 的 id 不在当前的 backStack 中了，说明它已被 pop 出去了，此时彻底清理它的状态缓存
    LaunchedEffect(navigator.backStack) {
        println("yunfei LaunchedEffect")
        val currentEntryIds = navigator.backStack.map { it.id }.toSet()
        
        // 找出所有在 savedEntryIds 中存在，但不再存在于 currentEntryIds 中的 key（被 pop 掉的页面）
        val poppedIds = savedEntryIds - currentEntryIds
        
        // 移除它们保存的状态，这样下次再 push 同一个路径时，就是全新的状态而不是恢复之前的脏状态
        poppedIds.forEach { id ->
            saveableStateHolder.removeState(id)
            savedEntryIds.remove(id)
        }
        
        // 将当前栈里的新 id 添加到记录中
        savedEntryIds.addAll(currentEntryIds)
    }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        println("yunfei CompositionLocalProvider")
        println("yunfei -------------------------------------------------------")
        Box(modifier = modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {
            // 获取当前栈顶路由实体
            val currentEntry = navigator.backStack.lastOrNull()

            if (currentEntry != null) {
                // 使用 AnimatedContent 实现路由切换动画
                AnimatedContent(
                    targetState = currentEntry,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        val enter = targetState.node.enterTransition
                        val exit = initialState.node.exitTransition

                        // 根据配置生成对应的进入动画
                        val enterAnim = when (enter) {
                            RouteTransition.None -> enterTransitionNone()
                            RouteTransition.Fade -> fadeIn(animationSpec = tween(300))
                            RouteTransition.Slide -> if (isPop) {
                                // Pop 时，A页面（旧页面，此时为 targetState）只需渐显，不进行位移，保持在原地被 B 离开后露出
                                fadeIn(animationSpec = tween(300))
                            } else {
                                // Push 时，B页面（新页面，此时为 targetState）从右侧完整滑入，覆盖在 A页面 之上
                                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300))
                            }
                            RouteTransition.Scale -> scaleIn(initialScale = 0.8f, animationSpec = tween(300)) + fadeIn()
                        }

                        // 根据配置生成对应的退出动画
                        val exitAnim = when (exit) {
                            RouteTransition.None -> exitTransitionNone()
                            RouteTransition.Fade -> fadeOut(animationSpec = tween(300))
                            RouteTransition.Slide -> if (isPop) {
                                // Pop 时，B页面（当前页，此时为 initialState）向右侧滑出，露出下面的 A页面
                                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))
                            } else {
                                // Push 时，A页面（当前页，此时为 initialState）保持不动并渐隐，等待被 B页面 覆盖
                                fadeOut(animationSpec = tween(300))
                            }
                            RouteTransition.Scale -> scaleOut(targetScale = 1.2f, animationSpec = tween(300)) + fadeOut()
                        }

                        // zIndex 控制层级：Push 时新页面在上，Pop 时退出页面在上（此时 targetZIndex 需要低于 initial）
                        (enterAnim togetherWith exitAnim).apply {
                            targetContentZIndex = if (isPop) -1f else 1f
                        }
                    },
                    label = "route_transition"
                ) { entry ->
                    // 仅当 entry 没有被标记为已销毁时渲染其状态
                    if (navigator.backStack.contains(entry)) {
                        // 为当前页面提供基于 SaveableStateHolder 的状态保留
                        saveableStateHolder.SaveableStateProvider(entry.id) {
                            // 提供独立协程作用域并挂载到 entry
                            val entryScope = rememberCoroutineScope()
                            DisposableEffect(entry) {
                                entry.scope = entryScope
                                onDispose {
                                    // 节点移除清理
                                }
                            }

                            // 渲染实际页面
                            entry.node.composable(entry.context)
                        }
                    } else {
                        // 如果 entry 已经不再栈内（比如 pop 动画仍在执行），我们不使用 StateProvider，
                        // 以免在清理后再次注册状态导致异常或内存泄漏
                        entry.node.composable(entry.context)
                    }
                }
            }
        }
    }
}

// 无动画占位符
private fun enterTransitionNone(): EnterTransition = fadeIn(animationSpec = tween(0))
private fun exitTransitionNone(): ExitTransition = fadeOut(animationSpec = tween(0))
