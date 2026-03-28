package shijing.tianqu.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import shijing.tianqu.router.Router
import shijing.tianqu.router.RouterContext
import shijing.tianqu.runtime.transition.routerSharedBounds
import shijing.tianqu.runtime.LocalNavigator

@OptIn(ExperimentalMaterial3Api::class)
@Router(path = "/shared_element_demo", transition = "Fade")
@Composable
fun SharedElementDemoScreen(context: RouterContext) {
    val navigator = LocalNavigator.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("共享元素动画演示") },
                navigationIcon = {
                    IconButton(onClick = { navigator.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .routerSharedBounds(key = "shared_box_1")
                        .size(200.dp)
                        .background(Color.Red)
                        .clickable { navigator.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("我是大红块 (点我返回)", color = Color.White)
                }
            }
        }
    }
}
