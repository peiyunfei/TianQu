package shijing.tianqu.featureb

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import shijing.tianqu.router.Router
import shijing.tianqu.router.RouterContext
import shijing.tianqu.runtime.LocalNavigator

@Router(path = "/featureb/home")
@Composable
fun FeatureBHomeScreen(context: RouterContext) {
    val navigator = LocalNavigator.current

    Box(modifier = Modifier.fillMaxSize().clickable{
        navigator.popBackStack()
    }, contentAlignment = Alignment.Center) {
        Text("我是跨模块页面，点击返回")
    }
}
