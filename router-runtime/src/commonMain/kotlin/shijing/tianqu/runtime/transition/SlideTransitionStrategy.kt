package shijing.tianqu.runtime.transition

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import shijing.tianqu.runtime.StackEntry

/**
 * 仿原生安卓的滑动过渡策略
 */
class SlideTransitionStrategy : BaseTransitionStrategy() {
    override fun getEnterTransition(
        scope: AnimatedContentTransitionScope<StackEntry>,
        initial: StackEntry,
        target: StackEntry,
        isPop: Boolean
    ): EnterTransition {
        return if (isPop) {
            // Pop 时，A页面（旧页面，此时为 targetState）从左侧微弱滑入，恢复到原位
            slideInHorizontally(
                initialOffsetX = { -(it * 0.3f).toInt() },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))
        } else {
            // Push 时，B页面（新页面，此时为 targetState）从右侧完整滑入
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        }
    }

    override fun getExitTransition(
        scope: AnimatedContentTransitionScope<StackEntry>,
        initial: StackEntry,
        target: StackEntry,
        isPop: Boolean
    ): ExitTransition {
        return if (isPop) {
            // Pop 时，B页面（当前页，此时为 initialState）向右侧滑出
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        } else {
            // Push 时，A页面（当前页，此时为 initialState）向左侧微滑，等待被 B页面 覆盖
            slideOutHorizontally(
                targetOffsetX = { -(it * 0.3f).toInt() },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))
        }
    }
}
