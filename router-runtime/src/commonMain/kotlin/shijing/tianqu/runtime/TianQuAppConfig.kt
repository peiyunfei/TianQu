package shijing.tianqu.runtime

import kotlin.reflect.KClass
import shijing.tianqu.router.RouterGuard
import shijing.tianqu.runtime.handler.RouterHandler

/**
 * [TianQuAppConfig] 采用 DSL 风格配置天衢应用的初始化参数。
 */
class TianQuAppConfig {
    /** 全局路由节点表 */
    var routes: List<RouterNode> = emptyList()
    
    /** 应用启动时的默认展示页面路由路径 */
    var startRoute: String = ""
    
    /** 业务层路由拦截器列表 */
    var guards: List<RouterGuard> = emptyList()
    
    /** 需要在路由导航时并发触发的数据预加载器注册表 */
    var preloaders: Map<String, RoutePreloader<*>> = emptyMap()
    
    /** 跨模块服务注册表 */
    var serviceProviders: Map<KClass<*>, () -> Any>? = null
    
    /** 全局未命中路由处理策略扩展 */
    var routerHandler: RouterHandler? = null
    
    /** 回退栈的最大深度限制。-1 表示无限制 */
    var maxStackSize: Int = -1
    
    /** 是否自动启用 BackHandler 拦截物理/手势返回 */
    var enableBackHandler: Boolean = true
    
    /** 路由事件的全局响应回调 */
    var onRouteEvent: suspend (RouterEvent, Navigator) -> Unit = { _, _ -> }
}
