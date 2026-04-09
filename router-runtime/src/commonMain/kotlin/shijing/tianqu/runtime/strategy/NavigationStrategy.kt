package shijing.tianqu.runtime.strategy

import shijing.tianqu.runtime.NavigationAction
import shijing.tianqu.runtime.Navigator
import shijing.tianqu.runtime.StackEntry

/**
 * 导航栈操作策略接口，用于处理不同启动模式和跳转动作下的入栈/出栈逻辑。
 */
interface NavigationStrategy {
    /**
     * 执行栈操作
     * @param navigator 导航器实例
     * @param entry 即将入栈或操作的条目
     * @param action 导航动作
     * @param url 目标路由URL
     * @return true 表示操作成功并已处理完事件发射，false 表示外部仍需处理默认入栈
     */
    suspend fun execute(navigator: Navigator, entry: StackEntry, action: NavigationAction, url: String): Boolean
}