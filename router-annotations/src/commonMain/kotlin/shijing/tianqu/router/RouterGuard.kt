package shijing.tianqu.router

/**
 * 路由守卫接口，采用责任链模式
 */
interface RouterGuard {
    /**
     * 匹配当前路由是否需要执行此拦截器
     * @param context 目标路由的上下文信息
     * @return 默认返回 true，表示全局拦截。若只想拦截部分路由，可重写此方法进行判断（局部拦截）。
     */
    fun matches(context: RouterContext): Boolean = true

    /**
     * 决定是否可以进入该路由
     *
     * @param context 目标路由的上下文信息
     * @param chain 责任链，允许异步调用和传递给下一个守卫
     * @return true 表示允许进入，false 表示拦截
     */
    suspend fun canActivate(context: RouterContext, chain: GuardChain): Boolean
}
