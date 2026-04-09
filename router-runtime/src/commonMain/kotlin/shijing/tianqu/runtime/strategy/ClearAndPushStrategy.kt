package shijing.tianqu.runtime.strategy

import shijing.tianqu.runtime.NavigationAction
import shijing.tianqu.runtime.Navigator
import shijing.tianqu.runtime.StackEntry

/**
 * 处理 CLEAR_AND_PUSH 策略。
 * 常用于用户登出或者强制跳转到首页的场景，会清空整个回退栈并压入新页面。
 */
class ClearAndPushStrategy : NavigationStrategy {
    override suspend fun execute(
        navigator: Navigator,
        entry: StackEntry,
        action: NavigationAction,
        url: String
    ): Boolean {
        val backStack = navigator.backStack
        
        // 遍历整个路由栈，触发每个页面的 dispose，防止内存泄漏（包括对应的 ViewModel 和 离线任务清理）
        backStack.forEach { it.dispose() }
        
        // 清空回退栈
        backStack.clear()
        
        // 将全新的目标页面压入空栈中作为新的根页面
        backStack.add(entry)
        
        return false // 返回 false 交由 Navigator 处理后续统一事件逻辑
    }
}