package shijing.tianqu.runtime.transition

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import shijing.tianqu.runtime.StackEntry

/**
 * 缩放过渡策略
 */
class ScaleTransitionStrategy : BaseTransitionStrategy() {
    override fun getEnterTransition(
        scope: AnimatedContentTransitionScope<StackEntry>,
        initial: StackEntry,
        target: StackEntry,
        isPop: Boolean
    ): EnterTransition {
        return scaleIn(initialScale = 0.8f, animationSpec = tween(300)) + fadeIn()
    }

    override fun getExitTransition(
        scope: AnimatedContentTransitionScope<StackEntry>,
        initial: StackEntry,
        target: StackEntry,
        isPop: Boolean
    ): ExitTransition {
        return scaleOut(targetScale = 1.2f, animationSpec = tween(300)) + fadeOut()
    }
}
