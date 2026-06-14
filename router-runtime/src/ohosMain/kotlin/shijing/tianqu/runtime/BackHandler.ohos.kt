package shijing.tianqu.runtime

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler as ComposeBackHandler

@OptIn(ExperimentalComposeUiApi::class)
@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    ComposeBackHandler(enabled = enabled, onBack = onBack)
}
