package shijing.tianqu.featureb

import shijing.tianqu.runtime.service.ServiceManager

/**
 * 模拟 feature-b 中的某个业务逻辑，它需要调用上层模块实现的服务。
 * 因为依赖关系是 composeApp -> feature-b，feature-b 无法直接使用 composeApp 中的类。
 * 通过 ServiceManager 就可以实现反向调用。
 */
object FeatureBManager {

    fun doSomethingAndTrack() {
        // feature-b 直接从 ServiceManager 获取服务实例并调用
        val service = ServiceManager.getService<AnalyticsService>()
        if (service != null) {
            service.trackEvent("feature_b_action_triggered")
        } else {
            println("FeatureBManager: AnalyticsService not found!")
        }
    }
}
