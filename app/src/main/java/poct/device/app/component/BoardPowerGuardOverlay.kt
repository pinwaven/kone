package poct.device.app.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.RouteConfig
import poct.device.app.theme.bgColor
import poct.device.app.theme.dangerColor
import poct.device.app.theme.fontColor
import poct.device.app.theme.tipBgColor

/**
 * 主板欠压重启保护：主流程锁定 overlay。
 * 运维路由组（设置/售后/系统功能）显示常驻 banner，不阻断操作；
 * 其余路由组显示可关闭的提示框——关闭只是把提示收起，锁定状态(boardPowerBlocked)
 * 本身不受影响；用户真正尝试开始检测时会调用 AppParams.reassertBoardPowerBlock()
 * 重新弹出同样的提示拦下来（见 HomeMain.kt "开始检测" 入口）。
 */
@Composable
fun BoardPowerGuardOverlay(navController: NavController) {
    val blocked by AppParams.boardPowerBlocked.collectAsState()
    val agingWarn by AppParams.boardPowerAgingWarn.collectAsState()
    val reassertEvent by AppParams.boardPowerBlockReassertEvent.collectAsState()
    if (!blocked) return

    val backStackEntry by navController.currentBackStackEntryAsState()
    val groupRoute = backStackEntry?.destination?.parent?.route
    val isOperationalGroup = groupRoute == RouteConfig.SETTING ||
        groupRoute == RouteConfig.AFTER_SALE ||
        groupRoute == RouteConfig.SYS_FUN

    var promptDismissed by remember { mutableStateOf(false) }
    var agingDialogDismissed by remember { mutableStateOf(false) }
    LaunchedEffect(blocked, reassertEvent) {
        // 每次进入新的锁定循环、或用户尝试开始检测被拦下，都重新展示提示
        promptDismissed = false
        agingDialogDismissed = false
    }

    if (isOperationalGroup) {
        BoardPowerGuardBanner()
    } else if (!promptDismissed) {
        BoardPowerGuardFullScreenBlock(onDismiss = { promptDismissed = true })
    }

    if (agingWarn && !agingDialogDismissed) {
        BoardPowerGuardAgingDialog(onDismiss = { agingDialogDismissed = true })
    }
}

/**
 * 需要充电的锁定提示框本体，公开供运维路由组（设置/系统功能等）里会实际驱动
 * 硬件的入口（如"设备初始化"）在点击时按需弹出——那些页面本身默认只显示
 * banner 不锁全屏，但真要驱动板子时仍需跟主流程一样拦住。
 *
 * 用 Dialog 承载而不是裸 Box(fillMaxSize())：Dialog 独立于调用处所在的布局树，
 * 不管从哪个页面的哪层 Row/Column 里调用都能真正铺满全屏；裸 Box 曾经在嵌套进
 * SettingMain 菜单的 Row 里时把 fillMaxSize 的约束带进那个 Row，把同一 Row/
 * Column 里后面的兄弟内容（"临时操作"菜单）撑挤消失。
 */
@Composable
fun BoardPowerGuardFullScreenBlock(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(tipBgColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.width(320.dp),
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        modifier = Modifier.padding(top = 24.dp, start = 24.dp, end = 24.dp),
                        text = stringResource(id = R.string.msg_board_power_need_charger),
                        textAlign = TextAlign.Center,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = fontColor,
                    )
                    AppFilledButton(
                        modifier = Modifier
                            .padding(bottom = 20.dp)
                            .width(120.dp),
                        onClick = onDismiss,
                        text = stringResource(id = R.string.btn_label_ok),
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardPowerGuardBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(dangerColor)
            .padding(vertical = 8.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(id = R.string.msg_board_power_need_charger),
            color = bgColor,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun BoardPowerGuardAgingDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(280.dp),
            shape = RoundedCornerShape(8.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = R.string.msg_board_power_aging_warn),
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp,
                    color = fontColor,
                )
                AppFilledButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onDismiss,
                    text = stringResource(id = R.string.btn_label_ok),
                )
            }
        }
    }
}
