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

    // ==== 进场动画 (目标页面要怎么出现) ====
    override fun getEnterTransition(
        scope: AnimatedContentTransitionScope<StackEntry>,
        initial: StackEntry,
        target: StackEntry,
        isPop: Boolean
    ): EnterTransition {
        return if (isPop) {
            // 【场景：用户点击返回键 (B -> A)】
            // 此时 target 是底部的 A 页面。
            // A 页面当初在进栈时，往左微微滑出了 30% 藏起来了。
            // 现在要回来了，所以它从 -30% 的位置（左侧），滑回原位 (0)，并伴随淡入(fadeIn)。
            slideInHorizontally(
                initialOffsetX = { -(it * 0.3f).toInt() },
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))
        } else {
            // 【场景：用户点击跳转 (A -> B)】
            // 此时 target 是新弹出的 B 页面。
            // B 页面需要从屏幕的最右侧边缘（100%宽度），完整地滑入屏幕中心。
            slideInHorizontally(
                initialOffsetX = { it }, // it 代表屏幕全宽
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        }
    }

    // ==== 退场动画 (老页面要怎么消失) ====
    override fun getExitTransition(
        scope: AnimatedContentTransitionScope<StackEntry>,
        initial: StackEntry,
        target: StackEntry,
        isPop: Boolean
    ): ExitTransition {
        return if (isPop) {
            // 【场景：用户点击返回键 (B -> A)】
            // 此时 initial 是即将被干掉的 B 页面。
            // B 页面要退出，按照常理，它应该向右滑动离开屏幕。
            slideOutHorizontally(
                targetOffsetX = { it }, // 滑动到屏幕右侧外 (100% 宽度)
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
        } else {
            // 【场景：用户点击跳转 (A -> B)】
            // 此时 initial 是底部的 A 页面。
            // B 马上要盖在 A 头上了！为了视觉差，A 不能傻站着不动，
            // A 需要向左侧微微退后 30%，并在 B 遮住它的时候伴随渐隐 (fadeOut)
            slideOutHorizontally(
                targetOffsetX = { -(it * 0.3f).toInt() }, // 向左滑动自身宽度的 30%
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))
        }
    }
}