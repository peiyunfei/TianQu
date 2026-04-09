package shijing.tianqu.runtime.strategy

import shijing.tianqu.runtime.NavigationAction
import shijing.tianqu.runtime.Navigator
import shijing.tianqu.runtime.StackEntry

/**
 * 处理 REPLACE 操作策略。
 * 弹出当前栈顶页面，并把新页面压入栈顶，不产生新的历史记录。
 */
class ReplaceStrategy : NavigationStrategy {
    override suspend fun execute(
        navigator: Navigator,
        entry: StackEntry,
        action: NavigationAction,
        url: String
    ): Boolean {
        val backStack = navigator.backStack
        
        // 如果栈不为空，将当前显示的页面弹出并触发其资源释放操作
        if (backStack.isNotEmpty()) {
            val removed = backStack.removeAt(backStack.lastIndex)
            removed.dispose()
        }
        
        // 将目标页面压入栈顶
        backStack.add(entry)
        
        return false // 交给外部触发通用导航事件
    }
}