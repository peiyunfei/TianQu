package shijing.tianqu.runtime.handler

import shijing.tianqu.router.RouterContext
import shijing.tianqu.runtime.Navigator

/**
 * 页面不存在，进行降级处理，先调用[handleExternalRoute]让外部路由处理。
 * 外部路由无法处理，调用[handleNotFound]。
 */
interface RouterHandler {

    /**
     * 外部路由处理
     *
     * @param context 路由上下文
     * @param navigator 导航器核心类，负责管理应用内的页面栈
     * @return true 外部路由已经处理了，默认不处理
     */
    suspend fun handleExternalRoute(context: RouterContext, navigator: Navigator): Boolean = false

    /**
     * 页面不存在
     *
     * @param url 页面路径
     * @param navigator 导航器核心类，负责管理应用内的页面栈
     * @return true 处理了该事件，默认不处理
     */
    suspend fun handleNotFound(url: String, navigator: Navigator): Boolean = false

}