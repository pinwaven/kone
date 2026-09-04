package poct.device.app.component

import androidx.activity.compose.BackHandler
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
 * 其余路由组全屏锁定，不可通过点击/返回键关闭，直到主板确认上电成功。
 */
@Composable
fun BoardPowerGuardOverlay(navController: NavController) {
    val blocked by AppParams.boardPowerBlocked.collectAsState()
    val agingWarn by AppParams.boardPowerAgingWarn.collectAsState()
    if (!blocked) return

    val backStackEntry by navController.currentBackStackEntryAsState()
    val groupRoute = backStackEntry?.destination?.parent?.route
    val isOperationalGroup = groupRoute == RouteConfig.SETTING ||
        groupRoute == RouteConfig.AFTER_SALE ||
        groupRoute == RouteConfig.SYS_FUN

    var agingDialogDismissed by remember { mutableStateOf(false) }
    LaunchedEffect(blocked) {
        // 每次进入新的锁定循环，重置老化提示的展示状态
        agingDialogDismissed = false
    }

    if (isOperationalGroup) {
        BoardPowerGuardBanner()
    } else {
        BackHandler(enabled = true) {}
        BoardPowerGuardFullScreenBlock()
    }

    if (agingWarn && !agingDialogDismissed) {
        BoardPowerGuardAgingDialog(onDismiss = { agingDialogDismissed = true })
    }
}

@Composable
private fun BoardPowerGuardFullScreenBlock() {
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
            Text(
                modifier = Modifier.padding(24.dp),
                text = stringResource(id = R.string.msg_board_power_need_charger),
                textAlign = TextAlign.Center,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = fontColor,
            )
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
