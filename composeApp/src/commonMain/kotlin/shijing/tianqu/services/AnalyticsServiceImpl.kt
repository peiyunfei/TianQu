package shijing.tianqu.services

import shijing.tianqu.router.Service
import shijing.tianqu.featureb.AnalyticsService

/**
 * 位于上层模块 (composeApp) 的服务实现。
 * 它实现了底层模块 (feature-b) 定义的接口。
 * 由于 composeApp 依赖于 feature-b，这里能访问到 AnalyticsService 接口。
 * KSP 会扫描到 @Service，并把它注册给 AnalyticsService。
 */
@Service
class AnalyticsServiceImpl : AnalyticsService {
    override fun trackEvent(eventName: String) {
        println("📊 [Analytics] 收到来自底层模块(feature-b)的埋点事件: $eventName")
    }
}
