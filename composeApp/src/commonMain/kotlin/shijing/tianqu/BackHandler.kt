package shijing.tianqu

import androidx.compose.runtime.Composable

/**
 * 拦截系统返回事件的处理器。
 * 在 Android 上会拦截物理返回键或手势返回，在 iOS 上则视具体实现而定（通常 iOS 使用原生侧滑返回）。
 * 
 * @param enabled 是否启用拦截，默认为 true
 * @param onBack 触发返回事件时的回调动作
 */
@Composable
expect fun BackHandler(enabled: Boolean = true, onBack: () -> Unit)
