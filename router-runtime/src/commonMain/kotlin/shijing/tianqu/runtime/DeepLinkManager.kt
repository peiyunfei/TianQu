package shijing.tianqu.runtime

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * 离线状态下的 DeepLink 全局意图缓存管理器。
 * 
 * 当用户从外部唤起 App 时，Compose UI 层和 Navigator 可能还未初始化。
 * 直接将 DeepLink 推送给即时分发的通道会导致意图丢失。
 * 因此，我们使用带有缓冲区的 Channel 作为意图队列。
 * 
 * Channel 的特性：一旦被收集（消费），数据就会从通道中移除。
 * 这保证了每个 DeepLink 只会被消费执行一次。
 */
object DeepLinkManager {
    
    // 使用无限容量的 Channel 缓存意图队列，防止在极端情况下队列满导致阻塞
    private val intentChannel = Channel<String>(Channel.UNLIMITED)
    
    // 供 Navigator 订阅消费的 Flow
    val pendingIntents = intentChannel.receiveAsFlow()

    /**
     * 外部系统（例如 Android 的 Activity.onNewIntent 或 iOS 的 application:openURL）调用。
     * 发送 DeepLink 意图到缓存队列。
     */
    fun dispatch(url: String) {
        intentChannel.trySend(url)
    }
}
