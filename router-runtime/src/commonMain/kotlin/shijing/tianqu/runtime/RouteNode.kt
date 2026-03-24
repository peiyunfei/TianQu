package shijing.tianqu.runtime

import androidx.compose.runtime.Composable
import shijing.tianqu.router.RouteContext
import shijing.tianqu.router.RouteTransition

/**
 * 路由节点数据模型，由 KSP 处理器生成
 */
data class RouteNode(
    val path: String,
    val regexPattern: String,
    val enterTransition: RouteTransition = RouteTransition.Slide,
    val exitTransition: RouteTransition = RouteTransition.Slide,
    val composable: @Composable (RouteContext) -> Unit
)
