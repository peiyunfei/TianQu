package shijing.tianqu.runtime

import shijing.tianqu.router.GuardChain
import shijing.tianqu.router.RouterContext
import shijing.tianqu.router.RouterGuard

/**
 * 路由守卫处理器，负责筛选和执行责任链
 */
object GuardProcessor {
    /**
     * 执行路由守卫责任链
     */
    suspend fun processGuards(context: RouterContext, guards: List<RouterGuard>): Boolean {
        // 只筛选出匹配当前路由的守卫
        val matchedGuards = guards.filter { it.matches(context) }
        if (matchedGuards.isEmpty()) return true

        var index = 0
        var isAllowed = true

        // 定义责任链内部实现
        val chain = object : GuardChain {
            override suspend fun proceed(ctx: RouterContext): Boolean {
                if (index < matchedGuards.size) {
                    val guard = matchedGuards[index++]
                    return guard.canActivate(ctx, this)
                }
                return true
            }
        }

        // 开始执行第一层守卫
        isAllowed = chain.proceed(context)
        return isAllowed
    }
}
