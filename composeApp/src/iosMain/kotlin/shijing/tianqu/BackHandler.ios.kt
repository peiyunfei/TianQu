package shijing.tianqu

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS typically handles back navigation via swipe gestures or navigation bar.
    // If custom back handling is required, it must intercept iOS gesture events.
}
