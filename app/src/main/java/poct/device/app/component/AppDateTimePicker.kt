package poct.device.app.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import poct.device.app.R
import poct.device.app.theme.activeColor
import poct.device.app.theme.inactiveColor
import poct.device.app.theme.inputFontColor
import poct.device.app.theme.placeHolderColor
import poct.device.app.utils.app.AppLocalDateUtils
import java.time.LocalDateTime

@Composable
fun AppDateTimePicker(
    value: String,
    placeHolder: String = stringResource(id = R.string.placeholder_date_time),
    fontSize: TextUnit = 14.sp,
    borderWidth: Dp = 0.dp,
    readOnly: Boolean = false,
    actionPainter: Painter = painterResource(id = R.mipmap.time_icon),
    onValueChange: (it: String) -> Unit = {},
) {
    var visible by remember { mutableStateOf(false) }
    val text = value.ifBlank { placeHolder }
    val fontColor = if (value.isBlank()) placeHolderColor else inputFontColor
    AppText(
        value = text,
        fontSize = fontSize,
        fontColor = fontColor,
        borderWidth = borderWidth,
        onClick = { visible = true },
        right = {
            if (!readOnly) {
                Icon(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { visible = true },
                    painter = actionPainter,
                    contentDescription = ""
                )
            }
        }
    )
    if (visible) {
        val dateTime = if (value.isEmpty()) LocalDateTime.now()
        else AppLocalDateUtils.parseDateTime(value)
        AppDateTimePickerInnerDialog(
            dateValue = AppLocalDateUtils.formatDate(dateTime.toLocalDate()),
            timeValue = AppLocalDateUtils.formatTime(dateTime.toLocalTime()),
            onDismissRequest = {visible = false},
            onValueChange = {
                onValueChange(it)
                visible = false
            })
    }
}

@Composable
private fun AppDateTimePickerInnerDialog(
    dateValue: String,
    timeValue: String,
    onDismissRequest: () -> Unit,
    onValueChange: (it: String) -> Unit,
) {
    var dateStr by remember { mutableStateOf(dateValue) }
    var timeStr by remember { mutableStateOf(timeValue) }
    var state by remember { mutableIntStateOf(0) }
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Surface(
            modifier = Modifier,
            shape = RoundedCornerShape(8.dp)
        ) {
            Surface(
                modifier = Modifier,
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    if (state == 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.date_time_date_part),
                                color = activeColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(id = R.string.date_time_time_part),
                                color = inactiveColor,
                                textAlign = TextAlign.End
                            )
                        }
                        AppDateWheel(value = dateStr, onValueChange = { dateStr = it })
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            AppOutlinedButton(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(40.dp),
                                text = stringResource(id = R.string.btn_label_cancel),
                                onClick = { onDismissRequest() })
                            Spacer(modifier = Modifier.width(10.dp))
                            AppFilledButton(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(40.dp),
                                text = stringResource(id = R.string.btn_label_next),
                                onClick = {
                                    state = 1
                                })
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.date_time_date_part),
                                color = inactiveColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(id = R.string.date_time_time_part),
                                color = activeColor,
                                textAlign = TextAlign.End
                            )
                        }
                        AppTimeWheel(value = timeStr, onValueChange = { timeStr = it })
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            AppOutlinedButton(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(40.dp),
                                text = stringResource(id = R.string.btn_label_previous),
                                onClick = {
                                    state = 0
                                })
                            Spacer(modifier = Modifier.width(10.dp))
                            AppFilledButton(
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(40.dp),
                                text = stringResource(id = R.string.btn_label_ok),
                                onClick = {
                                    onValueChange("$dateStr $timeStr")
                                })
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AppDateTimePickerPreview() {
    AppPreviewWrapper {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                AppDateTimePicker(
                    value = "",
                    borderWidth = 1.dp,
                    fontSize = 12.sp,
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                AppDateTimePicker(
                    value = "",
                    readOnly = true,
                    borderWidth = 1.dp,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
