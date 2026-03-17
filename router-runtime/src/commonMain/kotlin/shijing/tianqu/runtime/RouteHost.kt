package shijing.tianqu.runtime

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import shijing.tianqu.router.RouteGuard
import shijing.tianqu.router.RouteTransition

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
 * @param onRouteNotFound 路由降级回调，当未找到匹配路由时触发
 * @param parent 父级 Navigator（用于嵌套路由中的事件向上传递），默认为当前上下文中的 LocalNavigator
 * @return 返回管理导航栈状态的 Navigator 实例
 */
@Composable
fun rememberNavigator(
    routes: List<RouteNode>,
    startRoute: String,
    guards: List<RouteGuard> = emptyList(),
    onRouteNotFound: ((String) -> Unit)? = null,
    parent: Navigator? = null // 默认可以是 null，如果想要自动从 CompositionLocal 取，可以放在调用方或者用 LocalNavigator.current 但要注意它可能未提供
): Navigator {
    // 尝试获取父级 Navigator（如果处于嵌套环境中且没有显式传 parent，由于 LocalNavigator 有默认 error，我们无法安全判断是否提供，因此最好依赖上层显式传参，或捕获异常）
    // 为了安全，默认依赖参数传入 parent
    
    val coroutineScope = rememberCoroutineScope()
    val navigator = remember(routes, guards, onRouteNotFound, parent, coroutineScope) {
        Navigator(
            routeRegistry = routes,
            guards = guards,
            onRouteNotFound = onRouteNotFound,
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
    val isPop = navigator.lastAction == NavigationAction.POP ||
                navigator.lastAction == NavigationAction.POP_TO_ROOT ||
                navigator.lastAction == NavigationAction.POP_UNTIL

    // 自动处理返回键逻辑，允许嵌套路由也能响应返回
    // 只有当栈内有超过1个页面时，才由当前 Navigator 消耗返回事件
    shijing.tianqu.BackHandler(enabled = navigator.backStack.size > 1) {
        navigator.popBackStack()
    }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        Box(modifier = modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {
            // 获取当前栈顶路由实体
            val currentEntry = navigator.backStack.lastOrNull()

            if (currentEntry == null) {
                // 当栈为空时（比如初始异步导航还未完成），显示一个空白占位，避免画面闪烁
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading...")
                }
            } else {
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
                    // 渲染实际页面，传入上下文参数
                    entry.node.composable(entry.context)
                }
            }
        }
    }
}

// 无动画占位符
private fun enterTransitionNone(): EnterTransition = fadeIn(animationSpec = tween(0))
private fun exitTransitionNone(): ExitTransition = fadeOut(animationSpec = tween(0))
