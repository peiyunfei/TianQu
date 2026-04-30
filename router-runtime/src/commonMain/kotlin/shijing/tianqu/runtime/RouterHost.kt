package shijing.tianqu.runtime

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import shijing.tianqu.router.RouterGuard

import kotlinx.coroutines.CoroutineScope
import shijing.tianqu.runtime.handler.RouterHandler
import shijing.tianqu.runtime.renderer.DialogRouterRenderer
import shijing.tianqu.runtime.renderer.ScreenRouterRenderer

/**
 * 共享元素过渡作用域的 CompositionLocal
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/**
 * 动画可见性作用域的 CompositionLocal
 */
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

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
 * @param routes 由 KSP 生成的路由节点列表（通常为 GlobalRouteAggregator.routers）
 * @param startRoute 应用程序的初始路由地址（例如 "/home"）
 * @param guards 可选的路由守卫列表，可以通过重写 guard.matches 来实现局部拦截
 * @param parent 父级 Navigator（用于嵌套路由中的事件向上传递），默认为当前上下文中的 LocalNavigator
 * @return 返回管理导航栈状态的 Navigator 实例
 */
@Composable
fun rememberNavigator(
    routes: List<RouterNode>,
    startRoute: String,
    guards: List<RouterGuard> = emptyList(),
    routerHandler: RouterHandler? = null,
    parent: Navigator? = null,
    preloaders: Map<String, RoutePreloader<*>> = emptyMap(),
    maxStackSize: Int = -1 // -1表示不限制
): Navigator {
    val coroutineScope = rememberCoroutineScope()
    
    val navigator = remember(routes, guards, routerHandler, parent, coroutineScope, preloaders, maxStackSize) {
        Navigator(
            initialRoutes = routes,
            guards = guards,
            routerHandler = routerHandler,
            parent = parent,
            coroutineScope = coroutineScope,
            preloaders = preloaders,
            maxStackSize = maxStackSize
        )
    }

    // 问题：Navigator 的宿主（例如整个 RouterHost 或者嵌套路由）被移出 Compose 视图树时，
    //      Navigator 持有的协程作用域会取消，但它不会自动清空 backStack，导致栈内页面的 ViewModel 和
    //      SaveableStateHolder 发生严重内存泄漏（永远等不到 dispose）。
    // 方案：为 navigator 绑定一个 DisposableEffect。当 Navigator 实例脱离 Compose 树时，
    //      主动调用 disposeAll()，彻底清空回退栈和多 Tab 缓存栈里的资源。
    DisposableEffect(navigator) {
        onDispose {
            navigator.disposeAll()
        }
    }

    if (navigator.backStack.isEmpty()) {
        navigator.navigateTo(startRoute)
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
    // 将 state holder 提升作用域或使用单一实例，确保 tab 切换等场景不被重置
    // 由于 RouterHost 通常在全局只挂载一次，这里的 rememberSaveableStateHolder 是安全的
    val saveableStateHolder = rememberSaveableStateHolder()

    // 动态注册不同类型的路由渲染策略（如果未来有 BottomSheet 等，直接在这里添加即可）
    val routerRenderers = remember {
        listOf(
            // 负责渲染全屏页面和转场动画
            ScreenRouterRenderer(),
            // 负责渲染悬浮弹窗
            DialogRouterRenderer()
        )
    }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        Box(modifier = modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {
            // 使用策略模式遍历并渲染各种类型的页面
            // 这种方式将 RouterHost 与具体的渲染逻辑解耦，符合开闭原则（OCP）
            routerRenderers.forEach { renderer ->
                renderer.Render(navigator = navigator, saveableStateHolder = saveableStateHolder)
            }
        }
    }
}
