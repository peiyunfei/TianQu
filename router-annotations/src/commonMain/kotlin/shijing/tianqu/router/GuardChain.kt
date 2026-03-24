package shijing.tianqu.router

/**
 * 守卫责任链接口
 */
interface GuardChain {
    suspend fun proceed(context: RouteContext): Boolean
}
