package shijing.tianqu.runtime.renderer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.SaveableStateHolder
import shijing.tianqu.router.RouteType
import shijing.tianqu.runtime.Navigator

/**
 * 路由渲染策略接口。
 * 使用策略模式将不同类型页面的渲染逻辑从 RouterHost 中解耦。
 * 如果未来需要支持新的页面类型（如 BottomSheet），只需实现该接口并在 RouterHost 中注册即可。
 */
interface RouterRenderer {
    /**
     * 当前策略支持的路由类型
     */
    val supportedType: RouteType

    /**
     * 渲染组件的核心方法
     * @param navigator 导航器实例，提供当前的返回栈和状态信息
     * @param saveableStateHolder 用于保存和恢复页面状态的容器
     */
    @Composable
    fun Render(navigator: Navigator, saveableStateHolder: SaveableStateHolder)
}
