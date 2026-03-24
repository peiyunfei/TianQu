package shijing.tianqu.featureb

/**
 * 这是定义在 feature-b（底层模块）中的接口。
 * feature-b 不知道具体是谁来实现它，但它需要这个能力。
 */
interface AnalyticsService {
    fun trackEvent(eventName: String)
}
