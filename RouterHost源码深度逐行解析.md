# RouterHost 源码深度逐行解析

`RouterHost` 是整个 TianQu 路由框架在 Compose 侧的渲染核心引擎。它不仅仅负责“根据 URL 显示 Composable”，更关键的是解决了在声明式 UI 框架中常见的**状态保存、协程作用域管理、过渡动画方向控制以及内存泄漏问题**。

下面将对 `router-runtime/src/commonMain/kotlin/shijing/tianqu/runtime/RouteHost.kt` 中的 `RouterHost` 函数进行逐行拆解与深入分析。

---

## 1. 函数声明与基础准备

```kotlin
@Composable
fun RouterHost(
    navigator: Navigator,          // 注入的导航控制器，包含页面栈、路由事件等核心状态
    modifier: Modifier = Modifier  // 允许外部传入 Modifier 调整宿主的布局（如 padding, weight）
) {
```
这里使用了 `@Composable` 注解，声明这是一个 UI 组件。它必须接受一个 `Navigator` 实例，因为 `RouterHost` 的所有 UI 变化都由 `navigator.backStack` 驱动。

```kotlin
    // isPop 是一个布尔值，用于判断当前的导航动作是否是“回退”性质的操作。
    // 这在后续计算转场动画方向时非常关键。如果单纯进栈和出栈用同一种动画方向，视觉上会非常违和。
    val isPop = navigator.lastAction == NavigationAction.POP ||
                navigator.lastAction == NavigationAction.POP_TO_ROOT ||
                navigator.lastAction == NavigationAction.POP_UNTIL
```
此段代码监控 `navigator` 的 `lastAction` 状态。只要是 POP（出栈）相关的动作，`isPop` 就会被置为 `true`。

---

## 2. 状态保存容器与生命周期记录

```kotlin
    // rememberSaveableStateHolder 是 Compose 官方提供的状态保存神器。
    // 它可以在内部元素被移除出组件树（Composition）时，将其状态（如列表的滚动位置、TextField 的输入内容）
    // 序列化缓存在内存中。当具有相同 key (即 entry.id) 的元素再次被挂载时，状态能够无缝恢复。
    // 由于 RouterHost 通常在全局只挂载一次，这个 stateHolder 会伴随应用存活。
    val saveableStateHolder = rememberSaveableStateHolder()
```

```kotlin
    // 维护一个记录所有出现过的 entry.id 的集合。
    // 这是一个普通的 MutableSet，配合 remember 保证在 RouterHost 重组（Recomposition）时不丢失数据。
    // 它的作用是用来和当前的栈进行对比，从而知道哪些页面已经被彻底销毁。
    val savedEntryIds = remember { mutableSetOf<String>() }
```

---

## 3. 内存泄露防范：动态清理废弃状态

这是整个框架中非常巧妙且至关重要的一段代码，用于解决 `SaveableStateHolder` 导致的内存泄漏问题。

```kotlin
    // LaunchedEffect 会在 navigator.backStack 发生变化时重新执行。
    LaunchedEffect(navigator.backStack) {
        // 1. 将当前存在于导航返回栈中所有的页面 id 收集起来，存入 Set。
        val currentEntryIds = navigator.backStack.map { it.id }.toSet()

        // 2. 集合减法操作 (A - B)。
        // 找出所有存在于历史记录 (savedEntryIds) 中，但已经不在当前栈 (currentEntryIds) 中的 key。
        // 这意味着对应的页面刚刚被出栈 (Pop) 销毁了。
        val poppedIds = savedEntryIds - currentEntryIds

        // 3. 遍历这些被销毁的页面 id
        poppedIds.forEach { id ->
            // 从 saveableStateHolder 中彻底移除该页面的缓存状态！
            // 【极其重要】：如果不执行这一步，当用户下次重新 push 进入相同路径的页面时，
            // 会错误地恢复上一次退出时的废弃状态（比如上次填了一半的表单、上次滑动的列表位置），
            // 这会导致严重的业务 Bug 和内存泄漏。
            saveableStateHolder.removeState(id)
            
            // 从历史记录集合中抹除这个 id
            savedEntryIds.remove(id)
        }

        // 4. 将当前栈里的所有 id 补充到记录集合中，为下一次对比做准备
        savedEntryIds.addAll(currentEntryIds)
    }
```

---

## 4. 依赖注入环境与基础布局

```kotlin
    // CompositionLocalProvider 允许我们将 navigator 实例注入到当前树的上下文中。
    // 这样 RouterHost 内部包裹的所有子 Composable 页面，
    // 都可以直接通过 `val nav = LocalNavigator.current` 获取导航器，而不需要通过参数层层传递。
    CompositionLocalProvider(LocalNavigator provides navigator) {
        
        // 创建一个 Box 容器铺满全屏，并设置了背景色（基于 MaterialTheme）。
        // 设置背景色是为了在页面转场动画期间，底色不会是黑屏或透明，避免视觉闪烁。
        Box(modifier = modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)) {
```

---

## 5. 获取栈顶并配置动画引擎 (`AnimatedContent`)

```kotlin
            // 拿到栈顶的页面实体，如果栈是空的则为 null
            val currentEntry = navigator.backStack.lastOrNull()

            if (currentEntry != null) {
                // AnimatedContent 是 Compose 中专门用于处理内容替换动画的高级 API。
                // 当 targetState (即 currentEntry) 发生变化时，它会自动在两个状态之间执行动画。
                AnimatedContent(
                    targetState = currentEntry,
                    modifier = Modifier.fillMaxSize(),
```

### 5.1 核心：转场动画计算闭包

```kotlin
                    transitionSpec = {
                        // 这里的 this 指向 AnimatedContentTransitionScope。
                        // targetState 代表即将进入屏幕的新页面；initialState 代表即将离开屏幕的旧页面。
                        
                        // 从注解生成的路由节点中读取用户配置的入场和出场动画枚举。
                        val enter = targetState.node.enterTransition
                        val exit = initialState.node.exitTransition

                        // 根据配置生成对应的进入动画 (enterAnim)
                        val enterAnim = when (enter) {
                            RouteTransition.None -> enterTransitionNone()
                            RouteTransition.Fade -> fadeIn(animationSpec = tween(300))
                            
                            // 这里体现了 isPop 的价值：控制 Slide (滑动) 动画的方向
                            RouteTransition.Slide -> if (isPop) {
                                // 如果是 Pop(后退)：对于底下的新页面 (A页面)，它不需要再从边上滑入，
                                // 它只需要在原地执行 fadeIn() 渐渐显现即可。
                                fadeIn(animationSpec = tween(300))
                            } else {
                                // 如果是 Push(前进)：新进入的页面 (B页面) 需要从屏幕右侧滑入
                                slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300))
                            }
                            RouteTransition.Scale -> scaleIn(initialScale = 0.8f, animationSpec = tween(300)) + fadeIn()
                        }
```

```kotlin
                        // 根据配置生成对应的退出动画 (exitAnim)
                        val exitAnim = when (exit) {
                            RouteTransition.None -> exitTransitionNone()
                            RouteTransition.Fade -> fadeOut(animationSpec = tween(300))
                            
                            RouteTransition.Slide -> if (isPop) {
                                // 如果是 Pop(后退)：顶部的旧页面 (B页面) 需要向屏幕右侧滑出，露出下面的 A页面
                                slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))
                            } else {
                                // 如果是 Push(前进)：底下的旧页面 (A页面) 不需要滑走，只需要在原地渐渐隐去，
                                // 等待被新滑入的 B页面 覆盖即可。
                                fadeOut(animationSpec = tween(300))
                            }
                            RouteTransition.Scale -> scaleOut(targetScale = 1.2f, animationSpec = tween(300)) + fadeOut()
                        }
```

```kotlin
                        // 将进场动画和出场动画组合在一起 (togetherWith)。
                        (enterAnim togetherWith exitAnim).apply {
                            // targetContentZIndex 控制动画期间页面的上下层叠关系 (Z轴)。
                            // 默认情况下，新进入的页面层级高。
                            // 但如果是 Pop 退栈操作，正在离场的旧页面（B页面）必须盖在渐渐显现的新页面（A页面）之上，
                            // 否则会出现 A 突然跳到最前面遮挡住 B 滑出动画的现象。
                            // 所以这里如果是 isPop，就将新页面 (targetState) 的 ZIndex 降为 -1，保证它沉在底下。
                            targetContentZIndex = if (isPop) -1f else 1f
                        }
                    },
                    label = "route_transition" // 用于 Compose 调试工具中识别该动画
                ) { entry ->
```

---

## 6. 最终的页面渲染与生命周期挂载

```kotlin
                    // 此时进入到 AnimatedContent 的具体渲染闭包中。
                    // 为什么这里要做 backStack.contains 的判断？
                    // 因为在执行 Pop 动画的那 300 毫秒内，B页面 已经从 backStack 移除了，
                    // 但 AnimatedContent 为了播放退出动画，依然会让 B页面 存活 300 毫秒并执行这段渲染代码。
                    if (navigator.backStack.contains(entry)) {
                        
                        // 只有真正还在栈内的活跃页面，才需要被包裹在 SaveableStateProvider 中。
                        // Provider 通过 entry.id 将页面的所有内部状态 (如 TextField 的值) 与这个唯一 ID 绑定起来。
                        saveableStateHolder.SaveableStateProvider(entry.id) {
                            
                            // 利用 rememberCoroutineScope 为当前页面创建一个独立的协程作用域。
                            val entryScope = rememberCoroutineScope()
                            
                            // DisposableEffect 保证在页面挂载时执行大括号内容，在页面被销毁时执行 onDispose。
                            DisposableEffect(entry) {
                                // 将生成的独立协程作用域挂载到 StackEntry 实体上。
                                // 业务代码中所有的挂起任务，只要运行在这个 scope 下，
                                // 当 entry 被 pop 销毁时，这个 scope 会在 StackEntry.dispose() 中被 cancel()，
                                // 从而自动取消所有正在进行的网络请求，这是防止协程泄露的核心机制。
                                entry.scope = entryScope 
                                
                                onDispose {
                                    // 当前提供了一个回调占位符，如果以后有需要在这个 Compose 节点真正销毁时
                                    // 做的底层清理工作，可以加在这里。
                                }
                            }

                            // 最关键的一步：执行路由表中配置的闭包，即实际运行开发者编写的 @Router Composable 函数。
                            // 并把解析好的参数上下文 context 传给它。
                            entry.node.composable(entry.context)
                        }
                    } else {
                        // 【离场保护逻辑】
                        // 如果 entry 已经不在栈内了（说明它正在播放退栈滑动动画），
                        // 我们坚决不能再使用 saveableStateHolder.SaveableStateProvider 包裹它！
                        // 否则刚刚在上面 LaunchedEffect 中费尽心机 removeState 删掉的状态记录，
                        // 会立刻被 Provider 重新注册回来，导致彻底的内存和状态泄露！
                        // 此时只需"裸渲"页面，保证它能撑过这最后 300 毫秒的滑出动画即可。
                        entry.node.composable(entry.context)
                    }
                }
            }
        }
    }
}
```

---

## 总结
通过以上逐行分析，我们可以看到 `RouterHost` 的精巧设计：
1. 它用一套集合过滤逻辑彻底阻断了 `SaveableStateHolder` 的状态泄露。
2. 它针对导航栈的前进（Push）与后退（Pop）动态计算了符合人类直觉的滑动层级（ZIndex）与方向。
3. 它在极其复杂的重组（Recomposition）生命周期内，准确切分了"存活页"与"离场孤儿页"的渲染策略（`contains` 检查），完美结合了独立协程域（`entryScope`）的管理。

---

## 7. 真实业务场景推演：进出详情页到底发生了什么？

为了让上述复杂的逻辑更容易被理解，我们用一个**真实的业务场景**来演示这几行关键代码在底层到底发生了什么。

假设你的 App 正在运行，这里有两个页面：
- **`HomeScreen`** (主页，里面有一个可以滚动的很长的列表)
- **`DetailScreen`** (详情页)

### 场景一：从主页前进到详情页 (PUSH)

**用户的操作**：
1. 用户在 `HomeScreen` 把列表向下滑动了 500 个像素。
2. 用户点击了列表里的一项，触发跳转代码：`navigator.navigateTo("/detail")`。

**底层代码的反应**：
```kotlin
// 此时 navigator.lastAction 被设置为 NavigationAction.PUSH
val isPop = false // 因为不是后退，所以 isPop 为 false

// backStack 里本来只有 [HomeScreen]，现在变成了 [HomeScreen, DetailScreen]
// currentEntry 拿到了栈顶元素：DetailScreen
val currentEntry = navigator.backStack.lastOrNull()
```

此时 `AnimatedContent` 开始工作，因为页面要切换了：
```kotlin
transitionSpec = {
    // 发现是进场（PUSH），isPop 为 false
    val enterAnim = slideInHorizontally(新页面从右边滑入)
    val exitAnim = fadeOut(旧页面在原地不动，慢慢变暗)
    
    // ZIndex 设置为 1f，新页面 (DetailScreen) 会盖在旧页面 (HomeScreen) 上面滑进来
    targetContentZIndex = 1f
}
```

重点来了，渲染层发生了什么？
```kotlin
{ entry -> // 这里的 entry 是指 AnimatedContent 正在处理的页面（HomeScreen 和 DetailScreen 都会走一遍这个逻辑）
    
    // 此时两个页面都还在 backStack 里
    if (navigator.backStack.contains(entry)) {
        
        // 当渲染 HomeScreen 时，saveableStateHolder 发现 HomeScreen 不在屏幕最前面了，
        // 于是它把 HomeScreen "列表滑动了 500 像素" 这个状态给打包缓存到了内存里！
        // 它的唯一凭证就是 entry.id
        saveableStateHolder.SaveableStateProvider(entry.id) {
            entry.node.composable(entry.context)
        }
    }
}
```
**结果**：DetailScreen 顺利显示，HomeScreen 隐藏到了后台，但它滑动的 500 像素被 `SaveableStateHolder` 记住了。

---

### 场景二：从详情页返回主页 (POP)

**用户的操作**：
用户在 `DetailScreen` 看完了，按了手机的返回键，触发了：`navigator.popBackStack()`。

**底层代码的反应**：
```kotlin
// 此时 navigator.lastAction 被设置为 NavigationAction.POP
val isPop = true // 因为是返回操作！

// backStack 里原本是 [HomeScreen, DetailScreen]，执行 pop 后变成了只有 [HomeScreen]
// currentEntry 拿到了栈顶元素：HomeScreen
val currentEntry = navigator.backStack.lastOrNull()
```

此时 `AnimatedContent` 再次开始工作，它发现你要显示 `HomeScreen` 了：
```kotlin
transitionSpec = {
    // 这次是退场（POP），isPop 为 true，动画方向完全反转！
    val enterAnim = fadeIn(你要显示的 HomeScreen 在底层原地慢慢显现)
    val exitAnim = slideOutHorizontally(要离开的 DetailScreen 向右侧滑出屏幕)
    
    // ZIndex 设置为 -1f！为什么要降级？
    // 因为如果要离开的 DetailScreen 滑出时，新页面层级太高，DetailScreen 会瞬间被遮住，动画就看不到了。
    // 所以把要显示的 HomeScreen 压在下面。
    targetContentZIndex = -1f
}
```

最复杂的内存处理开始：
```kotlin
// 这个 LaunchedEffect 监听到了 backStack 发生了变化！
LaunchedEffect(navigator.backStack) {
    // 当前栈里只有 [HomeScreen] 了
    val currentEntryIds = ["home_id"]
    
    // 历史记录里有 ["home_id", "detail_id"]
    // 找差集：poppedIds = ["detail_id"] （系统发现了：哦，原来是详情页被关掉了）
    val poppedIds = savedEntryIds - currentEntryIds

    poppedIds.forEach { id ->
        // 【关键】从内存中彻底删除 DetailScreen 之前存过的所有 UI 状态（比如它内部的输入框文字）。
        // 否则你下次看另外一篇文章的详情时，还会带着这次输入的废旧文字。
        saveableStateHolder.removeState(id)
    }
}
```

这时候渲染层在做最后的挣扎：
```kotlin
{ entry ->
    // 此时 AnimatedContent 正在同时渲染 HomeScreen（进）和 DetailScreen（退）
    
    if (navigator.backStack.contains(entry)) {
        // 对于 HomeScreen 来说，它在栈里。
        // 它向 saveableStateHolder 要状态："嘿，我有 home_id，把我之前存的数据还给我！"
        // 于是，它的列表瞬间自动恢复到了之前向下滑动了 500 像素的位置。完全没有重新刷新的感觉！
        saveableStateHolder.SaveableStateProvider(entry.id) {
            entry.node.composable(entry.context)
        }
    } else {
        // 对于 DetailScreen 来说，它已经被弹出了，它已经不在栈里了。
        // 但是它现在正在播放向右滑出的动画（需要大概 300 毫秒）。
        // 这 300 毫秒内它不能死，必须画出来。
        // 【离场保护】：我们只画它，但不给它套 SaveableStateProvider，
        // 否则会把它刚才被 LaunchedEffect 删掉的垃圾状态又保存回去，导致严重 Bug。
        entry.node.composable(entry.context)
    }
}
```

### 业务价值结论
如果没有 `RouterHost` 里这几百行复杂的代码，用最普通的写法去写 Compose 页面切换，用户看到的灾难将是：
1. 从详情返回主页时，主页重新从服务器加载了一次，列表瞬间跳回了最顶部。
2. 动画很生硬，因为 Compose 默认渲染时不能感知你是"往前走"还是"往后退"。
3. 反复进出详情页，内存不断增加，直到崩溃。

这就是 `RouterHost` 的核心价值：**让声明式的 Compose 组件，拥有了堪比原生 Android 系统的页面栈生命周期和视觉体验。**