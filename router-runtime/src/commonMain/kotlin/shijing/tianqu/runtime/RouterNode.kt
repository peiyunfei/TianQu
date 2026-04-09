package shijing.tianqu.runtime

import androidx.compose.runtime.Composable
import shijing.tianqu.router.RouterContext
import shijing.tianqu.router.RouteType
import shijing.tianqu.runtime.transition.TransitionStrategy

/**
 * 路由节点数据模型，由 KSP 处理器生成
 */
import shijing.tianqu.router.LaunchMode

/**
 * 路由节点数据模型，由 KSP 处理器生成
 */
data class RouterNode(
    val path: String,
    val regexPattern: String,
    val transition: TransitionStrategy,
    val type: RouteType = RouteType.SCREEN,
    val launchMode: LaunchMode = LaunchMode.STANDARD,
    val composable: @Composable (RouterContext) -> Unit
)
