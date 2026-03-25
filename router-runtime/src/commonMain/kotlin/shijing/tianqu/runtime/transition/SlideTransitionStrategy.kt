package shijing.tianqu.runtime.transition

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import shijing.tianqu.runtime.StackEntry

/**
 * 滑动过渡策略
 */
class SlideTransitionStrategy : BaseTransitionStrategy() {
    override fun getEnterTransition(
        scope: AnimatedContentTransitionScope<StackEntry>,
        initial: StackEntry,
        target: StackEntry,
        isPop: Boolean
    ): EnterTransition {
        return if (isPop) {
            // Pop 时，A页面（旧页面，此时为 targetState）只需渐显，不进行位移，保持在原地被 B 离开后露出
            fadeIn(animationSpec = tween(300))
        } else {
            // Push 时，B页面（新页面，此时为 targetState）从右侧完整滑入，覆盖在 A页面 之上
            slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300))
        }
    }

    override fun getExitTransition(
        scope: AnimatedContentTransitionScope<StackEntry>,
        initial: StackEntry,
        target: StackEntry,
        isPop: Boolean
    ): ExitTransition {
        return if (isPop) {
            // Pop 时，B页面（当前页，此时为 initialState）向右侧滑出，露出下面的 A页面
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300))
        } else {
            // Push 时，A页面（当前页，此时为 initialState）保持不动并渐隐，等待被 B页面 覆盖
            fadeOut(animationSpec = tween(300))
        }
    }
}
