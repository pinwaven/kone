package poct.device.app.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import poct.device.app.AppParams
import poct.device.app.state.RuntimeModeState
import poct.device.app.theme.activeColor
import poct.device.app.theme.fontColor
import poct.device.app.theme.inputFontColor

@Composable
fun NanoEnvironmentSelector(
    modifier: Modifier = Modifier,
    width: Dp = 160.dp,
) {
    val nanoEnvironment by AppParams.runtimeModeState.nanoEnvironment.collectAsState()
    var dialogVisible by remember { mutableStateOf(false) }

    AppFilledButton(
        modifier = modifier
            .width(width)
            .height(36.dp),
        fontSize = 14.sp,
        text = "环境：${nanoEnvironment.label}",
        onClick = { dialogVisible = true }
    )

    NanoEnvironmentDialog(
        visible = dialogVisible,
        value = nanoEnvironment,
        onCancel = { dialogVisible = false },
        onSelected = {
            AppParams.runtimeModeState.setNanoEnvironment(it)
            dialogVisible = false
        }
    )
}

@Composable
private fun NanoEnvironmentDialog(
    visible: Boolean,
    value: RuntimeModeState.NanoEnvironment,
    onCancel: () -> Unit,
    onSelected: (RuntimeModeState.NanoEnvironment) -> Unit,
) {
    if (!visible) {
        return
    }
    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = Modifier
                .wakeScreenOnTouch()
                .width(300.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                Text(
                    text = "选择环境",
                    fontSize = 17.sp,
                    color = fontColor
                )
                Spacer(modifier = Modifier.height(10.dp))
                RuntimeModeState.NanoEnvironment.values().forEach { environment ->
                    NanoEnvironmentDialogItem(
                        environment = environment,
                        selected = environment == value,
                        onClick = { onSelected(environment) }
                    )
                }
            }
        }
    }
}

@Composable
private fun NanoEnvironmentDialogItem(
    environment: RuntimeModeState.NanoEnvironment,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clickable { onClick() },
        horizontalArrangement = Arrangement.Start
    ) {
        RadioButton(
            selected = selected,
            colors = RadioButtonDefaults.colors(selectedColor = activeColor),
            onClick = onClick
        )
        Text(
            modifier = Modifier.padding(top = 11.dp),
            text = "${environment.label}环境",
            fontSize = 15.sp,
            color = inputFontColor
        )
    }
}
