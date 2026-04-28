package poct.device.app.ui.workconfig


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import poct.device.app.R
import poct.device.app.component.AppBottomBar
import poct.device.app.component.AppFilledButton
import poct.device.app.theme.bg2Color


@Composable
fun WorkConfigCardAddBottomBar(
    stepValue: State<String>,
    onInfoReset: () -> Unit,
    onInfoNext: () -> Unit,
    onTopPre: () -> Unit,
    onTopNext: () -> Unit,
    onVarPre: () -> Unit,
    onVarPreview: () -> Unit,
    onVarSave: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.5.dp)
            .background(bg2Color)
    ) {
        AppBottomBar {
            when (stepValue.value) {
                WorkConfigCardAddViewModel.STEP_INFO -> {
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(40.dp),
                        onClick = { onInfoReset() },
                        text = stringResource(id = R.string.btn_label_rest)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(40.dp),
                        onClick = { onInfoNext() },
                        text = stringResource(id = R.string.btn_label_next)
                    )
                }
                WorkConfigCardAddViewModel.STEP_TOP -> {
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(40.dp),
                        onClick = { onTopPre() },
                        text = stringResource(id = R.string.btn_label_previous)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(40.dp),
                        onClick = { onTopNext() },
                        text = stringResource(id = R.string.btn_label_next)
                    )
                }
                else -> {
                    AppFilledButton(
                        modifier = Modifier
                            .width(90.dp)
                            .height(40.dp),
                        onClick = { onVarPre() },
                        text = stringResource(id = R.string.btn_label_previous)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    AppFilledButton(
                        modifier = Modifier
                            .width(90.dp)
                            .height(40.dp),
                        onClick = { onVarPreview() },
                        text = stringResource(id = R.string.btn_label_preview)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    AppFilledButton(
                        modifier = Modifier
                            .width(90.dp)
                            .height(40.dp),
                        onClick = { onVarSave() },
                        text = stringResource(id = R.string.btn_label_save)
                    )
                }
            }
        }
    }
}