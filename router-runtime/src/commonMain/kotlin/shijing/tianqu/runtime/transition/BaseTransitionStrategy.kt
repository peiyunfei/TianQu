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
        val enterAnim = getEnterTransition(scope, initial, target, isPop)
        val exitAnim = getExitTransition(scope, initial, target, isPop)
        
        return (enterAnim togetherWith exitAnim).apply {
            targetContentZIndex = computeTargetZIndex(isPop)
        }
    }
    
    /**
     * 计算目标页面的 Z 轴层级。
     * 默认规则：进栈时新页面在上(1f)，出栈时退出页面在上(新页面在下 -1f)。
     * 子类可以重写此方法以自定义层级逻辑。
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
