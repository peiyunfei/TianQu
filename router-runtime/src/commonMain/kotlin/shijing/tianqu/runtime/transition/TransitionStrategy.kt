package shijing.tianqu.runtime.transition

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import shijing.tianqu.runtime.StackEntry

/**
 * 页面切换动画策略接口
 */
interface TransitionStrategy {
    fun buildTransitionSpec(
        scope: AnimatedContentTransitionScope<StackEntry>,
        initial: StackEntry,
        target: StackEntry,
        isPop: Boolean
    ): ContentTransform
}
