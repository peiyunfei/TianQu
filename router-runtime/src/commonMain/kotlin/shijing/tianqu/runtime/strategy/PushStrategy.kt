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
                // 如果栈顶已经是该页面，复用栈顶，不创建新实例
                if (backStack.isNotEmpty() && backStack.last().node.path == entry.node.path) {
                    val topEntry = backStack.last()
                    // 精准拦截：只有当参数发生实质性改变时，才更新 context 触发重组
                    if (!topEntry.context.isSameParameters(entry.context)) {
                        topEntry.context = entry.context
                        topEntry.isNewIntent = true
                    }
                    navigator.coroutineScope.launch { navigator.emitEventForStrategy(RouterEvent.Navigated(action, url)) }
                    return true // 返回 true 表示已完全处理，外部无需继续入栈
                }
            }
            "SINGLE_TASK" -> {
                val existingIndex = backStack.indexOfLast { it.node.path == entry.node.path }
                if (existingIndex != -1) {
                    // 栈内存在该页面，将其上面的所有页面出栈
                    val removeCount = backStack.lastIndex - existingIndex
                    
                    // 设置 lastAction 为 POP_UNTIL，让底层动画系统（AnimatedContent）知道这是返回操作
                    // 从而避免被识别为新页面入栈，实现原页面状态的完美复用
                    navigator.setLastActionForStrategy(NavigationAction.POP_UNTIL)
                    
                    for (i in 0 until removeCount) {
                        val removed = backStack.removeAt(backStack.lastIndex)
                        removed.result?.complete(null)
                        removed.dispose()
                    }
                    
                    // 更新复用页面的 context，以传递新的参数触发重组
                    val targetEntry = backStack[existingIndex]
                    if (!targetEntry.context.isSameParameters(entry.context)) {
                        targetEntry.context = entry.context
                        targetEntry.isNewIntent = true
                    }
                    navigator.coroutineScope.launch { navigator.emitEventForStrategy(RouterEvent.Navigated(action, url)) }
                    return true
                }
            }
        }

        // 如果是 STANDARD 或者 SINGLE_TOP/SINGLE_TASK 没有命中复用条件，则直接将新页面入栈
        backStack.add(entry)
        return false // 返回 false 表示还需要外部 Navigator 执行通用的收尾逻辑（如发射事件和检查 maxStackSize）
    }
}