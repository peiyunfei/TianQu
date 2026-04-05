# 天衢 路由框架 `RouterHost` 源码解析与设计思考

在 天衢 路由框架中，如果说 `Navigator` 是负责管理页面栈和跳转逻辑的“大脑”，那么 `RouterHost` 就是负责将这些页面真正渲染到屏幕上的“躯体”。

本文将逐行剖析 `router-runtime/src/commonMain/kotlin/shijing/tianqu/runtime/RouterHost.kt` 文件，探讨其背后的架构设计原理。

---

## 核心源码逐行解析

### 1. 共享元素与动画作用域的 CompositionLocal

```kotlin
/**
 * 共享元素过渡作用域的 CompositionLocal
 */
@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/**
 * 动画可见性作用域的 CompositionLocal
 */
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }
```

*   **这是什么？**：定义了两个 `CompositionLocal`，用于在 Compose 树中隐式传递共享元素动画（Shared Element Transition）和可见性动画（Animated Visibility）的上下文作用域。
*   **为什么这么设计？**：
    在 Compose 中，要实现类似 Android 原生 `ActivityOptions.makeSceneTransitionAnimation` 的共享元素动画，子组件必须能够访问到父级的 `SharedTransitionScope` 和 `AnimatedVisibilityScope`。如果通过函数参数一层层往下传（Prop Drilling），代码会变得极其臃肿。通过 `CompositionLocal`，底层的 UI 组件（如图片、卡片）可以直接通过 `LocalSharedTransitionScope.current` 获取作用域并绑定动画修饰符，实现了动画逻辑与业务 UI 的完美解耦。

---

### 2. 协程作用域与 Navigator 的融合

```kotlin
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
```

*   **什么是 `rememberCoroutineScope()`？为什么不用 `LaunchedEffect`？**
    *   在 Compose 中启动协程，我们最常听到的两个 API 是 `LaunchedEffect` 和 `rememberCoroutineScope()`，它们的设计目的截然不同：
        *   **`LaunchedEffect(key)`（声明式副作用，自动执行）**：它本身是一个 `@Composable` 函数，它的内部闭包只能是被它自己控制调用的挂起函数。当它进入组件树时会**自动运行**内部的协程，当 `key` 变化时会自动取消并重启，当它离开组件树时自动取消。它通常用于**页面一加载就自动执行**的任务（比如初始化拉取数据）。**致命限制**：它内部封装了协程的自动触发，你无法将一个 `LaunchedEffect` 放在 `onClick { ... }` 事件回调里去让用户手动触发。
        *   **`rememberCoroutineScope()`（命令式副作用，手动执行）**：它本身也是一个 `@Composable` 函数，**必须**在 Composable 作用域内被调用（就像代码中那样）。但区别在于：它执行后会**返回一个被记忆的（remembered）、与当前组合生命周期绑定的普通的 `CoroutineScope` 对象**。
            **核心价值**：拿到这个对象后，你就可以把它带出 Composable 的世界，带入到普通的 Kotlin 回调函数中（比如按钮的 `onClick { scope.launch { ... } }`）。这样既拥有了在点击事件中命令式启动协程的能力，又享受了 Compose 组合生命周期带来的自动销毁（`cancel`）特性，**完美避免内存泄漏**。
    *   对于路由导航来说，绝大多数的跳转都是由用户点击按钮触发的，因此我们需要一个能在点击事件中使用的 `CoroutineScope`，这就是为什么这里使用的是 `rememberCoroutineScope()`。

    **【代码示例对比】**
    假设我们要实现一个“点击按钮，延迟 1 秒后跳转”的功能：

    **❌ 错误做法（使用 LaunchedEffect）：**
    ```kotlin
    @Composable
    fun LoginScreen() {
        Button(onClick = {
            // 编译报错！@Composable invocations can only happen from the context of a @Composable function
            // 因为 onClick 是一个普通的 () -> Unit 回调，不是 @Composable 环境
            LaunchedEffect(Unit) {
                delay(1000)
                navigator.push("/home")
            }
        }) { Text("登录") }
    }
    ```

    **✅ 正确做法（使用 rememberCoroutineScope）：**
    ```kotlin
    @Composable
    fun LoginScreen() {
        // 1. 在 @Composable 环境中调用，拿到一把“尚方宝剑”（作用域对象）
        val scope = rememberCoroutineScope()
        
        Button(onClick = {
            // 2. 把宝剑带入普通的 onClick 回调中，手动、命令式地启动协程
            scope.launch {
                delay(1000)
                navigator.push("/home")
            }
        }) { Text("登录") }
    }
    ```
    **为什么安全？** 如果用户点击了“登录”按钮，但在 1 秒的延迟期间，用户按了返回键退出了 `LoginScreen`。此时 `LoginScreen` 从组件树中移除，Compose 会自动 `cancel` 掉 `scope`。于是 `delay(1000)` 会抛出取消异常，后续的 `navigator.push("/home")` 根本不会执行，完美避免了“页面已销毁却还在跳转”的崩溃和内存泄漏。

*   **那 `rememberRouterScope()` 又是干什么的？为什么这么设计（黑科技）？**
    在日常开发中，我们经常需要在业务逻辑（例如一个挂起函数 `suspend fun login()`）中完成网络请求后，自动跳转到下一个页面。
    传统做法是把 `Navigator` 当作参数一路传进 `login(navigator: Navigator)`，这叫“参数传递地狱”（Prop Drilling），非常难看。

    而天衢路由的 `Navigator` 实现了一个非常黑科技的接口：`AbstractCoroutineContextElement`。这意味着，**Navigator 自己本身就是一种协程上下文（CoroutineContext）**，就跟 `Dispatchers.IO` 或者 `Job` 是一模一样的性质！

    在 `rememberRouterScope` 中：
    1. 先获取了 Compose 原生的生命周期绑定作用域 `baseScope = rememberCoroutineScope()`。
    2. 然后执行了 `baseScope.coroutineContext + navigator`。在 Kotlin 协程中，`+` 号代表将两个上下文合并。这里就是把“导航器实例”偷偷塞进（合并进）了这个协程的上下文中。
    3. **最终效果**：在通过 `rememberRouterScope()` 启动的任意深层嵌套的挂起函数中，开发者完全不需要传 `navigator` 参数，只需要写一句：
       `val nav = coroutineContext[Navigator]`
       就像从口袋里拿东西一样，神奇地就把路由实例掏出来了！这是一种极其高级的隐式依赖注入（DI）技巧。

---

### 3. 导航器的全局注入

```kotlin
/**
 * CompositionLocal 用于在组件树中向下传递 Navigator 实例，
 * 方便任意子组件通过 LocalNavigator.current 获取导航器并进行页面跳转。
 */
val LocalNavigator = compositionLocalOf<Navigator> {
    error("No Navigator provided. Make sure to wrap your content with RouterHost.")
}
```

*   **作用**：定义了传递 `Navigator` 的通道。如果开发者忘记在最外层包裹 `RouterHost` 就尝试获取导航器，会抛出明确的错误提示，这是一种良好的防御性编程（Defensive Programming）实践。

---

### 4. 导航器的初始化与记忆

```kotlin
@Composable
fun rememberNavigator(
    routes: List<RouterNode>,
    startRoute: String,
    guards: List<RouterGuard> = emptyList(),
    routerHandler: RouterHandler? = null,
    parent: Navigator? = null,
    preloaders: Map<String, RoutePreloader<*>> = emptyMap()
): Navigator {
    val coroutineScope = rememberCoroutineScope()
    
    val navigator = remember(routes, guards, routerHandler, parent, coroutineScope, preloaders) {
        Navigator(
            initialRoutes = routes,
            guards = guards,
            routerHandler = routerHandler,
            parent = parent,
            coroutineScope = coroutineScope,
            preloaders = preloaders
        )
    }
    
    // 如果栈是空的，自动跳转到起始页
    if (navigator.backStack.isEmpty()) {
        navigator.navigateTo(startRoute)
    }
    return navigator
}
```

*   **这是什么？**：负责实例化 `Navigator` 并将其与 Compose 的生命周期绑定。
*   **设计思考**：
    *   **`remember` 的使用**：确保在 Compose 发生重组（Recomposition）时，`Navigator` 实例不会被重复创建，从而保证路由栈状态的持久性。
    *   **自动起航**：当检测到 `backStack` 为空时，自动触发 `navigateTo(startRoute)`，这是整个 App 路由引擎启动的“第一脚油门”。

---

### 5. 核心渲染容器：RouterHost

```kotlin
@Composable
fun RouterHost(
    navigator: Navigator,
    modifier: Modifier = Modifier
) {
    // 1. 状态保存器
    val saveableStateHolder = rememberSaveableStateHolder()

    // 2. 渲染策略注册
    val routerRenderers = remember {
        listOf(
            ScreenRouterRenderer(),
            DialogRouterRenderer()
        )
    }

    // 3. 上下文注入与渲染
    CompositionLocalProvider(LocalNavigator provides navigator) {
        Box(modifier = modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {
            // 使用策略模式遍历并渲染各种类型的页面
            routerRenderers.forEach { renderer ->
                renderer.Render(navigator = navigator, saveableStateHolder = saveableStateHolder)
            }
        }
    }
}
```

*   **这是什么？**：这是整个路由框架在 UI 层的唯一入口，通常挂载在 `App.kt` 的最顶层。
*   **为什么这么设计（核心亮点）？**：
    1.  **`rememberSaveableStateHolder()`**：
        这是 Compose 提供的状态保存机制。B 页面覆盖 A 页面，Compose 会移除 A 页面，安卓 View 体系却不会移除 A 页面。
        *   **为什么 Compose 要移除 A 页面？Android View 体系不怕内存爆吗？**
            这涉及到两种不同 UI 架构的核心哲学差异：
            *   **传统 Android View 体系**：从 Activity A 跳转到 Activity B，A 的整棵 View 树确实依然驻留在内存中（只是处于 `onStop` 状态不可见）。它**当然也怕内存爆掉**！Android 系统的解法是：由操作系统（OS）在后台监控内存。如果内存吃紧，系统会无情地杀掉整个处于后台的 Activity A 进程（Low Memory Kill）。为了防止用户返回时懵逼，系统提供了 `onSaveInstanceState` 机制，让开发者手动把核心数据（如 `id=123`）存到一个 Bundle 里。等用户按返回键时，系统重新创建一个全新的 Activity A，再把 Bundle 塞给你去恢复界面。
            *   **Compose 声明式 UI 体系**：Compose 认为把一堆不可见的巨大 UI 树堆在内存里是非常浪费且不优雅的（尤其是 Compose 还要跨平台运行在 iOS、Web 等设备上）。所以，Compose 选择了更主动、更极端的优化策略：**不用的 UI 直接删掉**。当路由栈发生变化（栈顶变成 B），Compose 会进行重组（Recomposition）。它发现 A 页面的 Composable 函数不再被调用（不在屏幕上），就会立刻将其从内存的“组合树（Composition Tree）”中彻底移除（Dispose）。在 Compose 眼里，UI 只是状态（State）的映射，只要状态还在，UI 随时可以极速重建，根本不需要把不可见的 View 树缓存在内存里。
        *   **移除带来的副作用与解决方案**：
            既然 A 页面被彻底移除了，那么 A 页面里用户输入了一半的文本（`TextField`）、列表滑动到的位置（`LazyColumn` 的 `ScrollState`）等所有通过 `remember` 保存的局部状态，都会随着组件的销毁而灰飞烟灭。当用户从 B 返回 A 时，A 会被重新创建，一切从零开始，体验极差。
            `rememberSaveableStateHolder()` 就是为了解决这个痛点而生的。它就像一个**全局的保险箱**。在 A 页面被移除前，框架会把 A 页面的关键状态打包存进这个保险箱；当用户返回 A 页面，A 重新进入组合树时，框架再从保险箱里把状态取出来还给 A。
            将它放在最顶层的 `RouterHost` 中，确保了这个“保险箱”的生命周期与整个 App 一样长，从而完美支持了多 Tab 切换、深层路由返回等复杂场景下的状态恢复。
    2.  **策略模式（Strategy Pattern）与开闭原则（OCP）**：
        注意 `routerRenderers` 这个列表。框架并没有写死 `if (type == SCREEN) { ... } else if (type == DIALOG) { ... }` 这种丑陋的代码。
        相反，它将渲染逻辑抽象成了 `RouterRenderer` 接口，并注册了 `ScreenRouterRenderer`（负责渲染全屏页面和转场动画）和 `DialogRouterRenderer`（负责渲染悬浮弹窗）。
        **好处**：如果未来框架需要支持 `BottomSheet`（底部面板）或者 `Window`（桌面端独立窗口），只需要新建一个 Renderer 实现类并添加到这个列表中即可，完全不需要修改 `RouterHost` 的核心代码。这极大地提升了框架的扩展性和可维护性。
    3.  **`CompositionLocalProvider`**：
        在这里，将 `navigator` 实例正式注入到 `LocalNavigator` 中，从此，包裹在 `Box` 内部的所有业务页面，都可以随时随地获取到导航器了。

## 总结

`RouterHost.kt` 完美地扮演了“承上启下”的角色：
*   **向上**：它接管了 Compose 的生命周期、状态保存机制（SaveableState）和协程作用域。
*   **向下**：它通过 `CompositionLocal` 为业务组件提供了无缝的导航能力，并通过**策略模式**优雅地处理了不同形态页面（全屏、弹窗）的渲染逻辑。