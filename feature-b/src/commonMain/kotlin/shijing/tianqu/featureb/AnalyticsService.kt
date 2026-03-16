package shijing.tianqu.featureb

/**
 * 这是定义在 feature-b（底层模块）中的接口。
 * feature-b 不知道具体是谁来实现它，但它需要这个能力。
 */
interface AnalyticsService {
    fun trackEvent(eventName: String)
}

/**
 * 模拟 feature-b 中的某个业务逻辑，它需要调用上层模块实现的服务。
 * 因为依赖关系是 composeApp -> feature-b，feature-b 无法直接使用 composeApp 中的类。
 * 通过 ServiceManager 就可以实现反向调用。
 */
object FeatureBManager {
    fun doSomethingAndTrack() {
        // feature-b 直接从 ServiceManager 获取服务实例并调用
        val service = shijing.tianqu.runtime.ServiceManager.getService(AnalyticsService::class) as? AnalyticsService
        
        if (service != null) {
            service.trackEvent("feature_b_action_triggered")
        } else {
            println("FeatureBManager: AnalyticsService not found!")
        }
    }
}
