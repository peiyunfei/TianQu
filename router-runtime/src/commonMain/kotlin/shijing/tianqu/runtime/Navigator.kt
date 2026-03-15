package shijing.tianqu.runtime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf

class Navigator(private val routeRegistry: Map<String, @Composable () -> Unit>) {
    private val backStack = mutableStateListOf<String>()

    fun navigateTo(path: String) {
        if (routeRegistry.containsKey(path)) {
            backStack.add(path)
        } else {
            println("Route not found: $path")
        }
    }

    fun popBackStack() {
        if (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    @Composable
    fun currentScreen() {
        val currentRoute = backStack.lastOrNull()
        if (currentRoute != null) {
            val screen = routeRegistry[currentRoute]
            screen?.invoke()
        }
    }
}