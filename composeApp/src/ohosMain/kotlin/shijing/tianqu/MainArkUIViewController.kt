package shijing.tianqu

import androidx.compose.ui.window.ComposeArkUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.ArkTS.ArkTS_Napi_NativeModule.napi_env
import platform.ArkTS.ArkTS_Napi_NativeModule.napi_value
import kotlinx.coroutines.initMainHandler
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("MainArkUIViewController")
fun MainArkUIViewController(env: napi_env): napi_value {
    println("yunfei MainArkUIViewController")
    initMainHandler(env)
    return ComposeArkUIViewController(env) { App() }
}
