package shijing.tianqu.runtime.renderer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import shijing.tianqu.router.RouteType
import shijing.tianqu.runtime.LocalAnimatedVisibilityScope
import shijing.tianqu.runtime.LocalSharedTransitionScope
import shijing.tianqu.runtime.NavigationAction
import shijing.tianqu.runtime.Navigator

/**
 * 屏幕类型（SCREEN）页面的渲染策略。
 * 负责处理栈顶页面的显示、退出与进入动画（AnimatedContent）以及共享元素（SharedTransitionLayout）的过渡。
 */
class ScreenRouterRenderer : RouterRenderer {
    override val supportedType: RouteType = RouteType.SCREEN

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    override fun Render(navigator: Navigator, saveableStateHolder: SaveableStateHolder) {
        // 获取当前栈顶类型为 SCREEN 的路由实体
        val currentScreenEntry = navigator.backStack.lastOrNull { it.node.type == supportedType }

        if (currentScreenEntry != null) {
            SharedTransitionLayout {
                // 将 SharedTransitionScope 暴露出去，供页面内使用 modifier 共享元素
                CompositionLocalProvider(LocalSharedTransitionScope provides this@SharedTransitionLayout) {
                    // 使用 AnimatedContent 实现路由切换动画
                    AnimatedContent(
                        targetState = currentScreenEntry,
                        modifier = Modifier.fillMaxSize(),
                        transitionSpec = {
                            val isPopAnim = navigator.lastAction == NavigationAction.POP ||
                                            navigator.lastAction == NavigationAction.POP_TO_ROOT ||
                                            navigator.lastAction == NavigationAction.POP_UNTIL
                            
                            // Push 时使用目标页面的动画策略；Pop 时使用即将退出页面的动画策略（这样才能执行对应页面的反向出场动画）
                            val strategy = if (isPopAnim) initialState.node.transition else targetState.node.transition

                            strategy.buildTransitionSpec(
                                scope = this,
                                initial = initialState,
                                target = targetState,
                                isPop = isPopAnim
                            )
                        },
                        label = "route_transition"
                    ) { entry ->
                        // 将 AnimatedVisibilityScope 暴露出去，支持更精细的动画控制
                        CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@AnimatedContent) {
                            // 统一使用 SaveableStateProvider，不管是否在栈内，保证在退出动画期间页面层级不被破坏
                            saveableStateHolder.SaveableStateProvider(entry.id) {
                                // 提供独立协程作用域并挂载到 entry
                                val entryScope = rememberCoroutineScope()
                                DisposableEffect(entry) {
                                    entry.scope = entryScope
                                    onDispose {
                                        // 当页面从屏幕上完全移除（动画结束）时，如果它不在返回栈内，也不在被缓存的后台 Tab 栈内，则彻底清理状态
                                        if (!navigator.isEntryAlive(entry)) {
                                            saveableStateHolder.removeState(entry.id)
                                        }
                                    }
                                }

                                // 渲染实际页面
                                // 将当前页面的 StackEntry (它实现了 ViewModelStoreOwner) 注入给 Compose 上下文
                                // 这样在该页面内调用 viewModel() 时，拿到的就会是与该页面生命周期绑定的 ViewModel
                                CompositionLocalProvider(
                                    LocalViewModelStoreOwner provides entry
                                ) {
                                    entry.node.composable(entry.context)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
