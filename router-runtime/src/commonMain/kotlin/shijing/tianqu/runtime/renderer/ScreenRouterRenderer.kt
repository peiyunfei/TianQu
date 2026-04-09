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
import shijing.tianqu.runtime.LocalStackEntry
import shijing.tianqu.runtime.NavigationAction
import shijing.tianqu.runtime.Navigator

/**
 * 屏幕类型（SCREEN）页面的渲染策略。
 * 负责处理栈顶页面的显示、退出与进入动画（AnimatedContent）以及共享元素（SharedTransitionLayout）的过渡。
 */
class ScreenRouterRenderer : RouterRenderer {

    // 声明当前支持处理哪种类型的路由。
    // 策略模式的基础。RouterHost 遍历所有 renderer，只有匹配 SCREEN 的节点才会进入这里的逻辑。
    override val supportedType: RouteType = RouteType.SCREEN

    @OptIn(ExperimentalSharedTransitionApi::class)
    @Composable
    override fun Render(navigator: Navigator, saveableStateHolder: SaveableStateHolder) {
        // 从 Navigator 的回退栈（backStack）中，找到最后一个（也就是栈顶）类型为 SCREEN 的页面。
        // 为什么只找“最后一个”？因为对于全屏页面，用户只能看到最上面的一层。
        // 底下的页面不需要渲染，Compose 会自动将它们从组合树中移除以节省极大的内存。
        val currentScreenEntry = navigator.backStack.lastOrNull { it.node.type == supportedType }

        // 如果栈里有全屏页面，才开始下面的渲染。
        if (currentScreenEntry != null) {

            // SharedTransitionLayout：这是 Compose 最新提供的共享元素动画容器。
            // 必须在最外层包裹这个 Layout，里面所有的页面跳转才能在彼此之间传递需要共享的 UI 元素（比如从列表页点击图片，图片平滑放大到详情页）。
            SharedTransitionLayout {

                // CompositionLocalProvider 依赖注入：将当前的共享元素作用域（this@SharedTransitionLayout）注入到上下文中。
                // 因为子页面（如详情页）的 Composable 函数并不知道自己外面套了 SharedTransitionLayout。
                // 通过 LocalSharedTransitionScope，子页面可以通过 `Modifier.sharedElement()` 随处调用共享动画，实现了容器和页面的极致解耦。
                CompositionLocalProvider(LocalSharedTransitionScope provides this@SharedTransitionLayout) {

                    // transitionSpec 的职责是告诉框架：从 A 变到 B，用什么动画？
                    // 注意：这个花括号里的代码运行在 AnimatedContentTransitionScope 作用域中。
                    // Compose 官方框架会【自动】为你提供两个极其关键的上下文变量：
                    // 1. initialState: 代表动画【开始前】的页面状态（即旧页面，马上要被干掉或盖住）
                    // 2. targetState: 代表动画【结束后】的页面状态（即新页面，也是你刚刚传进来的 currentScreenEntry）
                    AnimatedContent(
                        targetState = currentScreenEntry, // 监听栈顶页面的变化
                        modifier = Modifier.fillMaxSize(), // 占满全屏

                        // transitionSpec：极其关键的一步！动态决定使用什么动画来切换。
                        transitionSpec = {
                            // 判断当前导航操作是不是“出栈”（返回上一页）操作。
                            val isPopAnim = navigator.lastAction == NavigationAction.POP ||
                                    navigator.lastAction == NavigationAction.POP_TO_ROOT ||
                                    navigator.lastAction == NavigationAction.POP_UNTIL

                            // 动画主导权的控制
                            // Compose 自动赋值了 initialState 和 targetState，我们来分配主导权：
                            //
                            // 假如我们执行入栈操作：A -> Push -> B
                            // 此时 initialState = A, targetState = B。
                            // 进入动画，是由【新弹出的页面 B】的注解（@Router(transition=...)）决定的。
                            // 所以取 targetState.node.transition！
                            //
                            // 假如我们执行退栈操作：B -> Pop -> A
                            // 此时发生了反转！initialState = B (原本在上面，现在要走), targetState = A (底下的页面重见天日)。
                            // 退出动画，必须【原路返回】。这不应该由底下A的动画决定，而应该由【正在被销毁的页面B】决定。
                            // 所以在 Pop 时，策略主导权交给了即将离场的页面，也就是 initialState.node.transition！
                            val strategy = if (isPopAnim) initialState.node.transition else targetState.node.transition

                            // 将动画的具体实现，委托给开发者配置的具体策略实现类。
                            strategy.buildTransitionSpec(
                                scope = this,          // 动画作用域
                                initial = initialState,// 即将退场的旧页面
                                target = targetState,  // 即将登场的新页面
                                isPop = isPopAnim      // 告诉策略类当前是前进还是后退
                            )
                        },
                        label = "route_transition"
                    ) { entry -> // 这个 entry 是 AnimatedContent 提供给你的当前正在执行动画的节点（可能有新旧两个节点同时在渲染）

                        // 将 AnimatedVisibilityScope 注入。这是很多精细化进出场动画（如 Modifier.animateEnterExit）必须要用到的环境。
                        CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@AnimatedContent) {

                            // SaveableStateProvider：恢复与保存 Compose 页面状态的核心（非常关键！）。
                            // 根据 entry.id 为这个页面开辟一块专属的“状态保留区”。
                            // 由于使用了 AnimatedContent，当 B 压入栈顶时，A 页面会被从 UI 树中移除。
                            // 这里提供了一个包裹层，如果页面被移除，它的状态（输入框文字、列表位置）会被存入全局的 saveableStateHolder，等下次返回 A 时原样恢复。
                            saveableStateHolder.SaveableStateProvider(entry.id) {

                                // DisposableEffect 监听节点的上屏与下屏生命周期。
                                DisposableEffect(entry) {
                                    onDispose {
                                        // onDispose：当这个 Compose 节点从屏幕上彻底被移除（并且切换动画也播放完了）时触发。
                                        // isEntryAlive 会检查这个页面是不是还存在于导航栈中。
                                        // 如果是因为页面真的被 POP 销毁了，那就彻底清理掉它缓存在全局的 UI 状态（removeState），防止内存泄漏。
                                        // 假如是因为压入了新页面导致的 A 页面被“暂时”移除，isEntryAlive 会返回 true，此时不会清空状态，等待未来恢复。
                                        if (!navigator.isEntryAlive(entry)) {
                                            saveableStateHolder.removeState(entry.id)
                                        }
                                    }
                                }

                                // 注入 ViewModel 和 StackEntry 上下文
                                // 为什么需要 LocalViewModelStoreOwner？因为 Compose 里的 `viewModel()` 函数底层会去这个 Owner 里面拿 ViewModel。
                                // 我们把 StackEntry（它内部实现了 ViewModelStoreOwner 接口）提供出去。
                                // 实现了“页面级 ViewModel”的存活域！哪怕页面组件经历了重组、哪怕因为入栈被暂时移除屏幕，只要它还在后退栈（Navigator.backStack）中，它的 ViewModel 实例就不会被销毁！
                                CompositionLocalProvider(
                                    LocalViewModelStoreOwner provides entry,
                                    LocalStackEntry provides entry
                                ) {
                                    // 真正执行由开发者编写的 Compose 页面代码。
                                    // 每次重组时，如果 isNewIntent 为 true，说明参数更新了，这里会传入最新的 context
                                    entry.node.composable(entry.context)
                                    
                                    // 消费掉 isNewIntent 标志，避免重复触发（可选，取决于具体业务是否需要严格的消费机制）
                                    // 但由于 Compose 是声明式的，只要 context 变了，composable 就会用新的 context 重新执行
                                    // 所以这里其实不需要手动重置 isNewIntent，只要 context 是 mutableState 或者在重组范围内即可
                                    // 为了确保 context 变化能触发重组，StackEntry 中的 context 应该被包裹在 mutableStateOf 中，或者在 Renderer 中被读取
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}