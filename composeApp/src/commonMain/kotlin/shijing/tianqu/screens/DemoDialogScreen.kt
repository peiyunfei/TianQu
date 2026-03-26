package shijing.tianqu.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import shijing.tianqu.BackHandler
import shijing.tianqu.router.RouteType
import shijing.tianqu.router.Router
import shijing.tianqu.router.RouterContext
import shijing.tianqu.runtime.LocalNavigator

@Router(
    path = "/demo_dialog",
    type = RouteType.DIALOG
)
@Composable
fun DemoDialogScreen(context: RouterContext) {
    val navigator = LocalNavigator.current

    // 业务层状态：是否允许点击外部空白处关闭弹窗
    var dismissOnClickOutside by remember { mutableStateOf(true) }
    // 业务层状态：是否允许点击返回键关闭弹窗
    var dismissOnBackPress by remember { mutableStateOf(true) }

    // 业务层控制：拦截物理返回键或手势返回
    // 如果 dismissOnBackPress 为 false，拦截后不做任何操作（即不关闭弹窗）
    // 如果 dismissOnBackPress 为 true，正常调用 popBackStack
    BackHandler(enabled = true) {
        if (dismissOnBackPress) {
            navigator.popBackStack()
        } else {
            println("业务层拦截了返回键，禁止关闭弹窗")
        }
    }

    // 外层全屏 Box，作为所谓的“外部空白区域”
    Box(
        modifier = Modifier
            .fillMaxSize()
            // 业务层控制：点击空白处的逻辑
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // 去除点击波纹
            ) {
                if (dismissOnClickOutside) {
                    navigator.popBackStack()
                } else {
                    println("业务层设置了点击外部不关闭弹窗")
                }
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        // 这是真正可见的弹窗内容区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                // 阻断点击事件向下传递，防止点击弹窗内部也触发了外部 Box 的 clickable
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 顶部的小横条装饰
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "业务层控制弹窗行为演示",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // 控制点击外部是否关闭的开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("允许点击外部关闭", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = dismissOnClickOutside,
                        onCheckedChange = { dismissOnClickOutside = it }
                    )
                }

                // 控制物理返回键是否关闭的开关
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("允许返回键关闭", style = MaterialTheme.typography.bodyLarge)
                    Switch(
                        checked = dismissOnBackPress,
                        onCheckedChange = { dismissOnBackPress = it }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { navigator.popBackStack() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("通过按钮主动关闭")
                }
                Spacer(modifier = Modifier.height(16.dp)) // 底部留白
            }
        }
    }
}