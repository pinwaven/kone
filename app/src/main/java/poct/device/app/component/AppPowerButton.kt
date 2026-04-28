package poct.device.app.component

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import poct.device.app.MyDeviceAdminReceiver
import poct.device.app.R
import poct.device.app.theme.filledFontColor
import poct.device.app.theme.fontColor


/**
 * start与end同时设置，仅start有效
 * top与bottom同时设置，仅top有效
 */
@Composable
fun AppPowerButton(
    start: Dp? = null, end: Dp? = null, top: Dp? = null, bottom: Dp? = null,
    viewModel: AppPowerButtonViewModel = viewModel(),
) {
    val context = LocalContext.current
    val powerVisible by viewModel.powerVisible.collectAsState()
    val powerConfirm by viewModel.powerConfirm.collectAsState()
    val powerIng by viewModel.powerIng.collectAsState()
    val restartConfirm by viewModel.restartConfirm.collectAsState()
    val restartIng by viewModel.restartIng.collectAsState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = this.minWidth
        val height = this.maxHeight
        val iconSize = 32.dp
        var buttonStart = 0.dp
        var iconStart = 0.dp
        var buttonTop = 0.dp
        end?.let {
            buttonStart = width - it - iconSize
            iconStart = buttonStart - 56.dp
        }
        start?.let {
            buttonStart = start
            iconStart = buttonStart
        }
        bottom?.let {
            buttonTop = height - bottom - iconSize
        }
        top?.let { buttonTop = top }

        IconButton(
            modifier = Modifier.padding(start = buttonStart, top = buttonTop),
            onClick = {
                viewModel.updatePowerVisible(true)
            }
        ) {
            Icon(
                modifier = Modifier.size(iconSize),
                painter = painterResource(id = R.mipmap.power_off_icon),
                tint = filledFontColor,
                contentDescription = ""
            )
        }
        // 关机、重启选择
        DropdownMenu(
            expanded = powerVisible == true,
            onDismissRequest = { viewModel.updatePowerVisible(false) },
            modifier = Modifier
                .width(96.dp)
                .wrapContentSize(Alignment.TopStart),
//            offset = DpOffset(buttonStart, buttonTop - 112.dp)
            offset = DpOffset(iconStart, buttonTop)
        ) {
            MainPowerMenuItem(
                {
                    viewModel.updatePowerVisible(false)
                    viewModel.updateRestartConfirm(true)
                },
                painterResource(id = R.mipmap.cq_icon),
                stringResource(id = R.string.action_restart)
            )
        }
    }

    // 重启确认弹窗
    if (restartConfirm == true) {
        MainPowerConfirmDialog(
            text = stringResource(id = R.string.msg_restart_confirm),
            onCancel = { viewModel.updateRestartConfirm(false) },
            confirmText = stringResource(id = R.string.action_restart),
        ) {
            viewModel.restart(context)
        }
    }

    if (restartIng == true) {
        AppViewLoading(msg = stringResource(id = R.string.msg_restart_ing))
    }
}

@Composable
private fun MainPowerConfirmDialog(
    text: String,
    onCancel: () -> Unit,
    cancelText: String = stringResource(id = R.string.btn_label_cancel),
    confirmText: String = stringResource(id = R.string.btn_label_ok),
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = Modifier
                .width(280.dp)
                .height(140.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 15.dp, end = 15.dp, top = 24.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = fontColor,
                    text = text
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AppOutlinedButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(36.dp),
                        onClick = onCancel,
                        text = cancelText,
                        fontSize = 14.sp
                    )
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(36.dp),
                        onClick = onConfirm,
                        text = confirmText,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MainPowerMenuItem(
    onClick: () -> Unit,
    painter: Painter,
    text: String,
) {
    DropdownMenuItem(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    painter = painter,
                    tint = filledFontColor,
                    contentDescription = ""
                )
                Text(text = text)
            }
        }
    )
}

class AppPowerButtonViewModel : ViewModel() {
    val powerVisible = MutableStateFlow(false)
    val powerConfirm = MutableStateFlow(false)
    val powerIng = MutableStateFlow(false)
    val restartConfirm = MutableStateFlow(false)
    val restartIng = MutableStateFlow(false)

    fun updatePowerVisible(powerVisibleValue: Boolean) {
        powerVisible.value = powerVisibleValue
    }

    fun updatePowerConfirm(powerConfirmValue: Boolean) {
        powerConfirm.value = powerConfirmValue
    }

    fun updateRestartConfirm(restartConfirmValue: Boolean) {
        restartConfirm.value = restartConfirmValue
    }

    fun restart(context: Context) {
        restartConfirm.value = false
        restartIng.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val dpm = getDevicePolicyManager(context)
            val adminComponent = getAdminComponent(context)
            dpm.reboot(adminComponent)
        }
    }

    private fun getDevicePolicyManager(context: Context): DevicePolicyManager {
        return context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    private fun getAdminComponent(context: Context): ComponentName {
        return ComponentName(context, MyDeviceAdminReceiver::class.java)
    }
}

@Preview
@Composable
fun AppPowerButtonPreview(viewModel: AppPowerButtonViewModel = viewModel()) {
    viewModel.updatePowerVisible(true)
    AppPowerButton(start = 24.dp, bottom = 40.dp)
//    AppPowerButton(end = 24.dp, bottom = 40.dp)
}
