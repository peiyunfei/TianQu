package shijing.tianqu.runtime.transition

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import shijing.tianqu.runtime.LocalAnimatedVisibilityScope
import shijing.tianqu.runtime.LocalSharedTransitionScope

/**
 * 便捷的共享元素 Modifier 扩展。
 * 只有当外层存在 SharedTransitionScope 和 AnimatedVisibilityScope 时才会生效。
 *
 * @param key 共享元素的唯一标识符（两个页面的标识符需要一致）
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.routerSharedElement(key: Any): Modifier = composed {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedElement(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    } else {
        this
    }
}

/**
 * 便捷的共享边界 Modifier 扩展。
 * 只有当外层存在 SharedTransitionScope 和 AnimatedVisibilityScope 时才会生效。
 *
 * @param key 共享元素的唯一标识符
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.routerSharedBounds(key: Any): Modifier = composed {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalAnimatedVisibilityScope.current

    if (sharedTransitionScope != null && animatedVisibilityScope != null) {
        with(sharedTransitionScope) {
            Modifier.sharedBounds(
                sharedContentState = rememberSharedContentState(key = key),
                animatedVisibilityScope = animatedVisibilityScope
            )
        }
    } else {
        this
    }
}
