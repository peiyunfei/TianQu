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
) {
    // 问题：在动态路由匹配时，如果缓存未命中，之前的写法是 `Regex(node.regexPattern).matches(path)`。
    //      这就导致了每次执行 `find` 遍历路由表时，都会对当前遍历到的每个节点现场 new 一个 Regex 对象。
    //      例如：目标路由排在第 15 位，那么发生 1 次跳转，就会无谓地创建 15 个 Regex 对象，用完即被 GC 回收；
    //      跳转 100 次，就会产生 1500 个瞬时的 Regex 对象，引发主线程 CPU 编译正则的尖峰和内存抖动。
    //
    // 方案：改为使用 `by lazy` 将编译好的 Regex 实例缓存在当前路由节点内部。
    //      这样不管触发多少次跳转查找，针对该节点的 Regex 实例在应用生命周期内最多只会被创建 1 次。
    //      同时搭配 LazyThreadSafetyMode.NONE 去掉多线程同步锁开销，最大化 Compose UI 线程的读取性能。
    val compiledRegex: Regex by lazy(LazyThreadSafetyMode.NONE) { Regex(regexPattern) }
}
