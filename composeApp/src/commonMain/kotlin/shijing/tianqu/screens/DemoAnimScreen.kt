package shijing.tianqu.screens

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import shijing.tianqu.router.RouterContext
import shijing.tianqu.router.Router
import shijing.tianqu.router.Transition
import shijing.tianqu.runtime.StackEntry
import shijing.tianqu.runtime.transition.BaseTransitionStrategy

/**
 * 演示：自定义的旋转缩放动画
 */
@Transition(name = "RotateScale")
class RotateScaleTransitionStrategy : BaseTransitionStrategy() {
    override fun getEnterTransition(
        scope: AnimatedContentTransitionScope<StackEntry>,
        initial: StackEntry,
        target: StackEntry,
        isPop: Boolean
    ): EnterTransition {
        return scaleIn(initialScale = 0.5f, animationSpec = tween(500)) + fadeIn(animationSpec = tween(500))
    }

    override fun getExitTransition(
        scope: AnimatedContentTransitionScope<StackEntry>,
        initial: StackEntry,
        target: StackEntry,
        isPop: Boolean
    ): ExitTransition {
        return scaleOut(targetScale = 1.5f, animationSpec = tween(500)) + fadeOut(animationSpec = tween(500))
    }
}

/**
 * 演示使用自定义动画策略的页面
 */
@Router(path = "/demo_anim", transition = "RotateScale")
@Composable
fun DemoAnimScreen(context: RouterContext) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Magenta),
        contentAlignment = Alignment.Center
    ) {
        Text("自定义动画展示页面")
    }
}
