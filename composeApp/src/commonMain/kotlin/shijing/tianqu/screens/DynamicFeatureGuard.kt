package shijing.tianqu.screens

import kotlinx.coroutines.delay
import shijing.tianqu.router.GuardChain
import shijing.tianqu.router.RouterContext
import shijing.tianqu.router.RouterGuard
import shijing.tianqu.runtime.Navigator
import shijing.tianqu.runtime.RouterNode
import shijing.tianqu.runtime.transition.FadeTransitionStrategy

class DynamicFeatureGuard : RouterGuard {
    
    var navigator: Navigator? = null
    
    // 模拟尚未加载的远程模块列表
    private val uninitializedModules = mutableListOf("/dynamic_feature")

    override fun matches(context: RouterContext): Boolean {
        // 如果目标路由在未初始化的模块列表中，则拦截
        println("📡 [DynamicFeatureGuard] matches")
        return uninitializedModules.any { context.url.startsWith(it) }
    }

    override suspend fun canActivate(context: RouterContext, chain: GuardChain): Boolean {
        val targetModule = uninitializedModules.firstOrNull { context.url.startsWith(it) } ?: return chain.proceed(context)
        
        println("📡 [DynamicFeatureGuard] 检测到未加载模块: $targetModule，准备挂起当前协程开始下载...")
        
        // 1. 模拟网络下载/模块初始化的耗时 (挂起 2 秒)
        delay(2000)
        
        // 2. 模拟下载完成，使用手动构造将新路由节点注册进框架
        val dynamicNode = RouterNode(
            path = targetModule,
            regexPattern = "^$targetModule$",
            transition = FadeTransitionStrategy(),
            composable = { DynamicFeatureScreen() }
        )
        navigator?.registerDynamicRoutes(listOf(dynamicNode))
        
        println("✅ [DynamicFeatureGuard] 模块下载并注册完成: $targetModule")
        
        // 3. 从待加载列表中移除，下次不再拦截
        uninitializedModules.remove(targetModule)
        
        // 4. 恢复挂起，放行路由跳转，继续后续的跳转流程
        return chain.proceed(context)
    }
}
