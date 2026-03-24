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

## 7. 真实业务场景推演：基于日志验证的执行轨迹

为了彻底弄懂 `RouterHost` 的运转逻辑，我们结合真实的日志输出来一行行分析代码的执行情况。这也是 Compose 响应式重组（Recomposition）机制最真实的体现。

我们在 `RouteHost.kt` 中关键位置打上日志：
- 在 `RouterHost` 函数第一行打上 `println("yunfei RouterHost")`
- 在 `CompositionLocalProvider` 之前打上 `println("yunfei CompositionLocalProvider")`
- 在 `CompositionLocalProvider` 里面，`LaunchedEffect` 之后打上 `println("yunfei -------------------------------------------------------")`
- 在 `LaunchedEffect(navigator.backStack)` 内部打上 `println("yunfei LaunchedEffect")`

### 阶段一：第一次启动，进入首页 (Start)

**日志输出**：
```text
2026-03-24 23:10:48.776 I yunfei RouterHost
2026-03-24 23:10:48.778 I yunfei CompositionLocalProvider
2026-03-24 23:10:48.778 I yunfei -------------------------------------------------------
2026-03-24 23:10:48.850 I yunfei LaunchedEffect
2026-03-24 23:10:48.861 I yunfei RouterHost
2026-03-24 23:10:48.861 I yunfei CompositionLocalProvider
2026-03-24 23:10:48.861 I yunfei -------------------------------------------------------
```

**逐行代码执行分析与原因**：

1. **第一次组件上树 (Initial Composition)**
   ```kotlin
   @Composable
   fun RouterHost(navigator: Navigator, modifier: Modifier = Modifier) {
       println("yunfei RouterHost") // 执行：首次调用 Composable 函数
       // ...
   ```
   
   这里有一个极其关键的细节！为什么第一次启动时 `yunfei RouterHost` 和 `CompositionLocalProvider` 会被打印**两次**，而 `LaunchedEffect` 只打印了**一次**？
   我们来看 `App.kt` 和 `RouteHost.kt` 的初始化代码：
   
   ```kotlin
   // App.kt
   val navigator = rememberNavigator(startRoute = "/main_tab", ...)
   
   // RouteHost.kt (rememberNavigator内部)
   LaunchedEffect(navigator, startRoute) {
       if (navigator.backStack.isEmpty()) {
           navigator.navigateTo(startRoute)
       }
   }
   ```
   
   **第一帧渲染 (Initial Composition)：**
   因为 `LaunchedEffect` 是异步执行的（等待协程调度），所以当 `RouterHost` **第一次**被调用并渲染时，`navigator.backStack` 确实还是**空**的！
   此时 `RouterHost` 内部执行到了这里：
   ```kotlin
   println("yunfei RouterHost") // 第 1 次打印
   // ...
   val currentEntry = navigator.backStack.lastOrNull() // 此时是 null
   // ...
   LaunchedEffect(navigator.backStack) { ... } // 注册 Effect，等待执行
   
   CompositionLocalProvider(LocalNavigator provides navigator) {
       println("yunfei CompositionLocalProvider") // 第 1 次打印
       println("yunfei -------------------------------------------------------") // 第 1 次打印
       // ...
       if (currentEntry != null) {
           // 此时 currentEntry 为 null，不满足条件，整个 AnimatedContent 被跳过，屏幕上什么都不画。
       }
   }
   ```
   
   **异步回调与重组 (Recomposition)：**
   紧接着，有两件异步的事情发生了：
   1. `RouterHost` 内部的 `LaunchedEffect(navigator.backStack)` 被调度执行了，打印出：
      ```kotlin
      println("yunfei LaunchedEffect") // 第 1 次打印。此时 backStack 还是空的。
      ```
   2. `rememberNavigator` 里的 `LaunchedEffect` 协程启动了，执行了 `navigator.navigateTo("/main_tab")`！首页被 push 进栈，`navigator.backStack` 这个 `StateList` 发生了改变！这引发了 `RouterHost` 依赖 `backStack` 地方的**重组 (Recomposition)**。
   
   **第二帧重组分析：**
   当 `backStack` 从空变成 `[HomeEntry]` 时，为什么又打印了一次 `RouterHost` 和 `CompositionLocalProvider`？
   因为在 Compose 中，`navigator.backStack` 是一个 `SnapshotStateList`，而我们在 `RouterHost` 函数体中有一行代码读取了它：
   ```kotlin
   val currentEntry = navigator.backStack.lastOrNull()
   ```
   这一行直接暴露在 `RouterHost` 的最外层作用域中！当 `backStack` 改变时，Compose 追踪到了最外层的依赖，因此整个 `RouterHost` 函数被重新执行了一遍！
   
   ```kotlin
   println("yunfei RouterHost") // 第 2 次打印，因为外层发生了重组
   // ...
   val currentEntry = navigator.backStack.lastOrNull() // 这次拿到了 HomeEntry！
   // ...
   
   // 🚨 为什么没有第二次打印 LaunchedEffect？
   // 因为 LaunchedEffect(navigator.backStack) 中的 key 是 navigator.backStack 这个 MutableList 对象。
   // 它的引用地址没有变（只是里面加了一个元素），因此 Compose 认为 Key 没有改变，不会重新执行 Effect 内部闭包！
   
   CompositionLocalProvider(LocalNavigator provides navigator) {
       println("yunfei CompositionLocalProvider") // 第 2 次打印
       println("yunfei -------------------------------------------------------") // 第 2 次打印
       
       if (currentEntry != null) { // 此时不为空了！
           AnimatedContent(...) { // 开始正式渲染主页的 UI
               // ...
           }
       }
   }
   ```

3. **异步执行 LaunchedEffect**
   ```kotlin
   LaunchedEffect(navigator.backStack) {
       println("yunfei LaunchedEffect") // 执行：Compose 在重组完成后，会在指定的协程中执行 LaunchedEffect 内部的代码。
       
       val currentEntryIds = navigator.backStack.map { it.id } // 此时只有 [Home_ID]
       val poppedIds = savedEntryIds - currentEntryIds // poppedIds 为空，因为 savedEntryIds 也是空的
       // ... (循环不执行)
   }
   ```

---

### 阶段二：从首页进入详情页 (PUSH)

**日志输出**：
```text
2026-03-24 23:11:38.686 I yunfei CompositionLocalProvider
2026-03-24 23:11:38.686 I yunfei -------------------------------------------------------
```
*(注意：这里没有打印 `yunfei RouterHost` 和 `yunfei LaunchedEffect`)*

**逐行代码执行分析与原因**：

当用户触发 `navigator.navigateTo("/detail")` 时，`navigator.backStack` 增加了 `DetailEntry`。

**为什么没打印 `yunfei RouterHost`？**
因为 Compose 编译器进行了非常极致的**局部重组优化（Smart Recomposition）**。
在 `RouterHost` 函数体中，最顶层的代码（比如 `println("yunfei RouterHost")`）读取的参数 `navigator` 实例本身并没有变（变的只是它里面的 `backStack` 列表里的元素）。
Compose 发现：只有 `CompositionLocalProvider` 内部读取了 `navigator.backStack` （传递给了 `LaunchedEffect` 和隐式参与了内部逻辑）。所以 Compose **直接跳过了函数的外层，从内部开始重组**！

```kotlin
// 外面的代码被跳过了！Compose 知道它们没必要重新执行

println("yunfei CompositionLocalProvider") // 执行：从这里开始发生局部重组
CompositionLocalProvider(
    LocalNavigator provides navigator
) {
    // LaunchedEffect(navigator.backStack) {...}  -> 这里代码不执行！
    // 为什么不打印 yunfei LaunchedEffect？
    // 因为虽然 backStack 里面的元素增加了，但 LaunchedEffect 监听的 `navigator.backStack` 对象引用并没有改变（它是一个 SnapshotStateList，增加元素不改变本身地址）。
    // 在 Compose 中，如果是 mutableStateListOf，只要它的元素发生改变（增删），读取它的作用域就会重组。
    // 但是，LaunchedEffect(key1) 中的 key1 需要发生对象级别的 equals() 不等才会重新触发内部闭包。
    // 由于 navigator.backStack 这个 MutableList 对象的引用始终是同一个，LaunchedEffect 认为 "Key 没变"，所以内部代码不执行！
    // 这是一个常见的 Compose 行为。如果我们希望 LaunchedEffect 每次增加页面都执行，它的 key 应该传 navigator.backStack.toList()。
    // 幸好，在 PUSH 操作时，我们本来就不需要清理状态（poppedIds 是空的），所以它没执行也没有引入 Bug。
    
    println("yunfei -------------------------------------------------------") // 执行
    
    AnimatedContent(...) { entry ->
        // 执行：这里会调用两遍！
        // 一遍给处于栈底的 HomeEntry（开始渐隐动画，保存状态）
        // 一遍给新压入栈顶的 DetailEntry（开始滑入动画，创建新状态容器）
        if (navigator.backStack.contains(entry)) {
             saveableStateHolder.SaveableStateProvider(entry.id) { ... }
        }
    }
}
```

---

### 阶段三：退出详情页，返回首页 (POP)

**日志输出**：
```text
2026-03-24 23:12:16.334 I yunfei RouterHost
2026-03-24 23:12:16.335 I yunfei CompositionLocalProvider
2026-03-24 23:12:16.335 I yunfei -------------------------------------------------------
```
*(注意：这里打印了 `yunfei RouterHost`，但依然没打印 `yunfei LaunchedEffect`)*

**逐行代码执行分析与原因**：

当用户触发 `navigator.popBackStack()` 时，`navigator.lastAction` 被设置成了 `NavigationAction.POP`，随后 `backStack` 移除了 `DetailEntry`。

```kotlin
@Composable
fun RouterHost(navigator: Navigator, modifier: Modifier = Modifier) {
    println("yunfei RouterHost") // 为什么这次执行了？
    // 因为这一次除了 backStack 变了，最关键的是 `navigator.lastAction` 变了！
    // 之前一直是在做 PUSH，这一次变成了 POP。这个状态的改变导致 `val isPop = ...` 这一行的计算结果发生了改变，
    // 触发了外层函数体的整体重组。
    
    val saveableStateHolder = rememberSaveableStateHolder() // 执行：因为 remember，直接返回之前缓存的实例
    val isPop = navigator.lastAction == NavigationAction.POP // 执行：isPop 变成了 true！
    val currentEntry = navigator.backStack.lastOrNull() // 执行：重新拿到 HomeEntry
    
    if (currentEntry == null) return // 不满足条件，继续
    
    println("yunfei CompositionLocalProvider") // 执行
    CompositionLocalProvider(
        LocalNavigator provides navigator
    ) {
        // LaunchedEffect(navigator.backStack) {...}
        // 为什么依然没有打印 yunfei LaunchedEffect？
        // 原因同上！navigator.backStack 对象的引用依然没变。所以 `LaunchedEffect` 认为 Key 没有改变，内部闭包没有被再次触发。
        //
        // 🚨【严重警示】🚨：
        // 通过这里的日志分析，我们发现了一个代码中的潜藏逻辑漏洞：
        // 在 POP 页面时，我们期望 LaunchedEffect 执行 `saveableStateHolder.removeState(id)` 来清理废弃内存。
        // 但由于 `LaunchedEffect(navigator.backStack)` 的 Key 是一个固定引用的 MutableList，它实际上并没有再次触发执行！
        // 这意味着：被 POP 出去的详情页的状态并没有被及时清理，发生了轻微的内存（状态）泄漏！
        // （修复方案：应该改为 `LaunchedEffect(navigator.backStack.size)` 或者 `LaunchedEffect(currentEntry)` 才能在发生增删时准确触发）。
        
        println("yunfei -------------------------------------------------------") // 执行
        
        AnimatedContent(...) { entry ->
            // 执行：再次调用两遍！
            // 一遍给重回视野的 HomeEntry：
            if (navigator.backStack.contains(entry)) { // 此时是 true
                // 向 saveableStateHolder 索要之前缓存的状态，页面完美恢复原状
                saveableStateHolder.SaveableStateProvider(entry.id) { ... }
            }
            
            // 一遍给正在被踢出局、播放离场动画的孤儿 DetailEntry：
            if (navigator.backStack.contains(entry)) { // 此时是 false！因为已被 pop 出栈
                // 不执行
            } else {
                // 执行！(离场保护)
                // 裸渲染这个残影，绝不能给它包裹 SaveableStateProvider
                entry.node.composable(entry.context)
            }
        }
    }
}
```

### 总结日志带给我们的深刻启示：
1. **第一次启动打印了两次 `RouterHost`**：证明 Compose 在处理异步初始状态时会发生快速的重组纠错（从 `currentEntry == null` 直接 `return`，到马上收到 `HomeEntry` 再次重组并开始渲染）。
2. **PUSH 操作跳过了 `RouterHost` 顶层**：虽然我们在 `RouterHost` 最外层写了 `val currentEntry = navigator.backStack.lastOrNull()`，但在 PUSH 详情页时（尤其是如果这个 PUSH 发生在当前页面的点击事件中，而不是根 `RouterHost` 重组触发），如果 Compose 优化判断顶层没有重新读取会导致 UI 变化的参数，可能直接在内部的 `AnimatedContent` 进行局部重组。（注：这里如果在某些 Compose 版本中依然发生了外层重组但没打印日志，是因为外层重组被优化跳过了，这也证明了 Compose 编译器的强大）。
3. **POP 操作恢复了 `RouterHost` 顶层执行**：因为 `navigator.lastAction` 从 `PUSH` 变成了 `POP`，影响了顶层代码 `val isPop = ...` 的值，所以触发了整体重组。
4. **意料之外的 `LaunchedEffect` 不执行**：这也是日志分析带来的最大价值。由于将 `SnapshotStateList` 直接作为 `LaunchedEffect` 的 Key，导致内存清理逻辑并未如预期般在每次 `POP` 时执行。

---

### 总结：为什么必须要这样执行？

1. **为什么需要 SaveableStateHolder？** Compose 的底层逻辑是纯函数渲染，UI 不在屏幕上（出组件树）它的局部状态（如输入框文字、列表滑动位置）就会全部丢失。借助它，页面才能表现得像原生的 Activity/Fragment 栈。
2. **为什么需要动态调整 ZIndex？** 如果固定新页面在上，当你 `POP` 退栈时，底下的旧页面会突然跳到最顶层，直接盖住退栈页面的滑出动画，视觉上会闪烁断层。
3. **为什么需要 backStack.contains 的离场保护？** `AnimatedContent` 的动画生命周期长于实际逻辑栈的生命周期。被踢出栈的页面在 UI 上变成了“孤魂野鬼”（还要活 300 毫秒），必须防范这 300 毫秒内由于组件状态变更而导致的幽灵状态存储。