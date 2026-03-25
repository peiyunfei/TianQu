package shijing.tianqu.runtime.transition

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import shijing.tianqu.runtime.StackEntry

/**
 * 无动画过渡策略
 */
class NoneTransitionStrategy : BaseTransitionStrategy() {
    override fun getEnterTransition(
        scope: AnimatedContentTransitionScope<StackEntry>,
        initial: StackEntry,
        target: StackEntry,
        isPop: Boolean
    ): EnterTransition {
        return fadeIn(animationSpec = tween(0))
    }

    override fun getExitTransition(
        scope: AnimatedContentTransitionScope<StackEntry>,
        initial: StackEntry,
        target: StackEntry,
        isPop: Boolean
    ): ExitTransition {
        return fadeOut(animationSpec = tween(0))
    }
    
    override fun computeTargetZIndex(isPop: Boolean): Float {
        // 无动画时，层级不重要
        return 0f
    }
}
