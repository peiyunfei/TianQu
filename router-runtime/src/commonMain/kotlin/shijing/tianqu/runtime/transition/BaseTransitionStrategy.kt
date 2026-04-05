package shijing.tianqu.runtime.transition

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.togetherWith
import shijing.tianqu.runtime.StackEntry

/**
 * 页面切换动画策略基础抽象类。
 * 封装了通用的 zIndex 层级处理，如果某些自定义动画不需要这种层级控制，
 * 可以直接实现 [TransitionStrategy] 接口或重写 [buildTransitionSpec] 方法。
 */
abstract class BaseTransitionStrategy : TransitionStrategy {
    
    override fun buildTransitionSpec(
        scope: AnimatedContentTransitionScope<StackEntry>,
        initial: StackEntry,
        target: StackEntry,
        isPop: Boolean
    ): ContentTransform {
        // 调用子类提供的：新页面的进场动画 (EnterTransition)
        val enterAnim = getEnterTransition(scope, initial, target, isPop)

        // 调用子类提供的：旧页面的退场动画 (ExitTransition)
        val exitAnim = getExitTransition(scope, initial, target, isPop)

        // 使用 togetherWith 将两个动画合并执行。
        // apply 里则是整个框架最核心的图层控制逻辑（Z-Index）
        return (enterAnim togetherWith exitAnim).apply {
            // 给目标页面（新进来的页面）设置层级！
            targetContentZIndex = computeTargetZIndex(isPop)
        }
    }

    /**
     * 计算目标页面的 Z 轴层级。
     * 为什么要有这个方法？这解决了动画的“遮盖”问题！
     *
     * - 当（A -> B）时 (isPop = false)：
     *   B是新页面（target）。新页面应该【盖在】老页面的上面滑入。
     *   所以这里返回 1f，让 target（B）显示在上层。
     *
     * - 当 Pop（B -> A）时 (isPop = true)：
     *   A变成了新页面（target），B成了老页面（initial）。
     *   如果在 Pop 时 A 在上面，那 A 会瞬间盖住 B。
     *   正确的视觉体验应该是：B（老页面）滑出，渐渐露出底下的A。
     *   所以这里返回 -1f，让 target（A）沉到底层去，从而实现经典的抽屉式后退效果！
     */
    protected open fun computeTargetZIndex(isPop: Boolean): Float {
        return if (isPop) -1f else 1f
    }
    
    abstract fun getEnterTransition(
        scope: AnimatedContentTransitionScope<StackEntry>,
        initial: StackEntry,
        target: StackEntry,
        isPop: Boolean
    ): EnterTransition
    
    abstract fun getExitTransition(
        scope: AnimatedContentTransitionScope<StackEntry>,
        initial: StackEntry,
        target: StackEntry,
        isPop: Boolean
    ): ExitTransition
}
