package shijing.tianqu

import androidx.compose.runtime.Composable
import shijing.tianqu.router.generated.GlobalRouteAggregator
import shijing.tianqu.runtime.Navigator
import shijing.tianqu.runtime.TianQuAppConfig
import shijing.tianqu.runtime.rememberTianQuApp

/**
 * App 层的零样板包装。
 * 自动注入 GlobalRouteAggregator，让业务层的配置做到极致精简。
 */
@Composable
fun rememberAppTianQuState(
    block: TianQuAppConfig.() -> Unit
): Navigator {
    return rememberTianQuApp {
        // 自动注入 KSP 生成的产物
        routes = GlobalRouteAggregator.routers
        serviceProviders = GlobalRouteAggregator.services

        // 业务自定义配置覆盖
        this.block()
    }
}
