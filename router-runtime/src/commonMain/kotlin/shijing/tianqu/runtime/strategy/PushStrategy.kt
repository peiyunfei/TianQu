package shijing.tianqu.runtime.strategy

import kotlinx.coroutines.launch
import shijing.tianqu.runtime.NavigationAction
import shijing.tianqu.runtime.Navigator
import shijing.tianqu.runtime.RouterEvent
import shijing.tianqu.runtime.StackEntry

/**
 * 处理普通的 PUSH 入栈操作策略。
 * 包含了对不同启动模式（SINGLE_TOP, SINGLE_TASK, STANDARD）的逻辑判断。
 */
class PushStrategy : NavigationStrategy {
    override suspend fun execute(
        navigator: Navigator,
        entry: StackEntry,
        action: NavigationAction,
        url: String
    ): Boolean {
        val launchMode = entry.node.launchMode.name
        val backStack = navigator.backStack

        when (launchMode) {
            "SINGLE_TOP" -> {
                // 【条件判断】：栈不能为空，且栈顶页面的路径（path）与即将入栈的页面路径相同。
                // 例如：当前栈顶是 "/user_profile"，即将跳转的也是 "/user_profile"。
                if (backStack.isNotEmpty() && backStack.last().node.path == entry.node.path) {

                    // 获取当前栈顶的页面实例
                    val topEntry = backStack.last()

                    // 【精准拦截】：判断新传入的参数（entry.context）和当前页面的参数（topEntry.context）是否完全一致。
                    // 如果参数完全一样（例如连点按钮），则什么都不做，直接拦截。
                    if (!topEntry.context.isSameParameters(entry.context)) {

                        // 【核心复用逻辑】：参数不同时，将新页面的 context 赋值给栈顶页面。
                        // 因为 topEntry.context 在 StackEntry 中是被 mutableStateOf 包裹的（或者在渲染时被读取），
                        // 这里的赋值操作会触发 Compose 的重组（Recomposition）。
                        // 页面 UI 会使用新的参数重新绘制，但页面实例（topEntry）并没有改变。
                        topEntry.context = entry.context

                        // 标记这是一个新的 Intent（类似于 Android 的 onNewIntent）。
                        // 业务层可以通过监听这个标记来执行特定的刷新逻辑（如果需要的话）。
                        topEntry.isNewIntent = true
                    }

                    // 发送导航成功的事件通知（例如通知全局的日志系统或埋点系统）。
                    navigator.coroutineScope.launch { navigator.emitEventForStrategy(RouterEvent.Navigated(action, url)) }

                    // 返回 true，告诉 Navigator 外部：“我已经处理完毕了，你不需要再执行默认的入栈（backStack.add）操作了”。
                    return true
                }
            }
            "SINGLE_TASK" -> {
                // 【查找目标】：从栈顶向栈底反向查找，找到最后一个路径匹配的页面索引。
                // 假设栈是 [A, B, C, D]，我们要跳转到 B (SINGLE_TASK)。
                // indexOfLast 会找到 B 的索引（1）。
                val existingIndex = backStack.indexOfLast { it.node.path == entry.node.path }
                // 如果找到了（索引不为 -1），说明栈内存在该页面，触发复用逻辑。
                if (existingIndex != -1) {
                    // 计算需要移除的页面数量。
                    // 在 [A, B, C, D] 中，B 的索引是 1，lastIndex 是 3。
                    // removeCount = 3 - 1 = 2。即需要移除 C 和 D 两个页面。
                    val removeCount = backStack.lastIndex - existingIndex
                    // 【动画策略控制】：极其关键的一步！
                    // 将最后一次导航动作设置为 POP_UNTIL（一直出栈直到...）。
                    // 为什么这么做？因为在 ScreenRouterRenderer 中，AnimatedContent 会根据 lastAction 来决定播放什么动画。
                    // 如果不设置，系统可能会认为这是一个普通的 PUSH（入栈）操作，从而播放错误的“新页面从右侧滑入”的动画。
                    // 设置为 POP_UNTIL 后，系统会播放“旧页面向右滑出，底层页面重见天日”的返回动画，符合用户的直觉。
                    navigator.setLastActionForStrategy(NavigationAction.POP_UNTIL)
                    // 【清理上层页面】：循环移除目标页面之上的所有页面。
                    for (i in 0 until removeCount) {
                        // 每次都从栈顶（lastIndex）移除元素。
                        val removed = backStack.removeAt(backStack.lastIndex)
                        // 如果被移除的页面有协程在等待它的返回结果（navigateWithResult），
                        // 这里传入 null 结束等待，防止协程泄漏。
                        removed.result?.complete(null)
                        // 调用 dispose() 触发页面的销毁生命周期回调。
                        removed.dispose()
                    }
                    // 【核心复用逻辑】：获取目标页面（此时它已经成为了新的栈顶）。
                    val targetEntry = backStack[existingIndex]
                    // 同样进行参数比对，如果参数不同，则更新 context 触发重组。
                    if (!targetEntry.context.isSameParameters(entry.context)) {
                        targetEntry.context = entry.context
                        targetEntry.isNewIntent = true
                    }
                    // 发送导航成功事件。
                    navigator.coroutineScope.launch { navigator.emitEventForStrategy(RouterEvent.Navigated(action, url)) }
                    // 返回 true，拦截默认的入栈操作。
                    return true
                }
            }
        }

        // 如果是 STANDARD 或者 SINGLE_TOP/SINGLE_TASK 没有命中复用条件，则直接将新页面入栈
        backStack.add(entry)
        return false // 返回 false 表示还需要外部 Navigator 执行通用的收尾逻辑（如发射事件和检查 maxStackSize）
    }
}