package shijing.tianqu.runtime.renderer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import shijing.tianqu.router.RouteType
import shijing.tianqu.runtime.LocalStackEntry
import shijing.tianqu.runtime.Navigator

/**
 * 弹窗类型（DIALOG）页面的渲染策略。
 * 负责渲染浮动在当前屏幕之上的 Dialog，可以同时渲染多个堆叠的弹窗。
 */
class DialogRouterRenderer : RouterRenderer {
    override val supportedType: RouteType = RouteType.DIALOG

    @Composable
    override fun Render(navigator: Navigator, saveableStateHolder: SaveableStateHolder) {
        // 渲染 弹窗 类型的页面，将栈里所有的 DIALOG 过滤出来进行层叠渲染
        val dialogEntries = navigator.backStack.filter { it.node.type == supportedType }
        dialogEntries.forEach { dialogEntry ->
            Dialog(
                onDismissRequest = {
                    navigator.popBackStack()
                },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                // 为弹窗保留并提供状态作用域
                saveableStateHolder.SaveableStateProvider(dialogEntry.id) {
                    DisposableEffect(dialogEntry) {
                        onDispose {
                            // 弹窗关闭时，彻底清理其状态
                            if (!navigator.isEntryAlive(dialogEntry)) {
                                saveableStateHolder.removeState(dialogEntry.id)
                            }
                        }
                    }
                    // 渲染弹窗内容
                    // 将当前页面的 StackEntry (它实现了 ViewModelStoreOwner) 注入给 Compose 上下文
                    CompositionLocalProvider(
                        LocalViewModelStoreOwner provides dialogEntry,
                        LocalStackEntry provides dialogEntry
                    ) {
                        dialogEntry.node.composable(dialogEntry.context)
                    }
                }
            }
        }
    }
}
