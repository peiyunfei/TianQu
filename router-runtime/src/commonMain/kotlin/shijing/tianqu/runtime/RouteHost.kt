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
 * @param guards 可选的全局路由守卫列表，用于在导航发生时进行拦截和鉴权
 * @return 返回管理导航栈状态的 Navigator 实例
 */
@Composable
fun rememberNavigator(
    routes: List<RouteNode>,
    startRoute: String,
    guards: List<RouteGuard> = emptyList()
): Navigator {
    val coroutineScope = rememberCoroutineScope()
    val navigator = remember(routes, guards, coroutineScope) {
        Navigator(routes, guards, coroutineScope)
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
    val isPop = navigator.lastAction == NavigationAction.POP || navigator.lastAction == NavigationAction.POP_TO_ROOT

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
