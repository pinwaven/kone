package poct.device.app.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import poct.device.app.App
import poct.device.app.R
import poct.device.app.theme.activeColor
import poct.device.app.theme.bg2Color
import poct.device.app.theme.inactiveColor
import poct.device.app.theme.inputFontColor
import poct.device.app.theme.placeHolderColor
import poct.device.app.theme.primaryColor
import poct.device.app.utils.app.AppLocalDateUtils
import poct.device.app.utils.app.AppToastUtil
import java.time.LocalDate
import java.time.Period

/**
 * values必须是两个字符串，且前面是开始日期，后面是结束日期
 * limit: 限制天数， 0代表不限制
 */
@Composable
fun AppDateRangePicker(
    values: List<String>?,
    placeHolder: String = stringResource(id = R.string.placeholder_date_range),
    fontSize: TextUnit = 14.sp,
    borderWidth: Dp = 0.dp,
    readOnly: Boolean = false,
    limit: Int = 0,
    onValueChange: (it: List<String>) -> Unit = {},
) {
    var visible by remember { mutableStateOf(false) }
    val text =
        if (values == null || values[0].isEmpty() || values[1].isEmpty()) placeHolder
        else stringResource(id = R.string.text_date_range).format(values[0], values[1])
    val fontColor =
        if (values == null || values[0].isEmpty() || values[1].isEmpty()) placeHolderColor
        else inputFontColor

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
                    painter = painterResource(id = R.mipmap.time_icon),
                    tint = primaryColor,
                    contentDescription = ""
                )
            }
        }
    )
    if (visible) {
        AppDateRangePickerInnerDialog(
            values = values,
            limit = limit,
            onDismissRequest = { visible = false },
            onValueChange = {
                onValueChange(it)
                visible = false
            })
    }
}

@Composable
private fun AppDateRangePickerInnerDialog(
    values: List<String>?,
    limit: Int = 0,
    onDismissRequest: () -> Unit = {},
    onValueChange: (it: List<String>) -> Unit = {},
) {
    var dateStarted by remember { mutableStateOf(values?.get(0) ?: "") }
    var dateEnded by remember { mutableStateOf(values?.get(1) ?: "") }
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
                                text = stringResource(id = R.string.date_start),
                                color = activeColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(id = R.string.date_end),
                                color = inactiveColor,
                                textAlign = TextAlign.End
                            )
                        }
                        AppDateWheel(value = dateStarted, onValueChange = { dateStarted = it })
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
                                    if(dateEnded < dateStarted) {
                                        dateEnded = dateStarted
                                    }
                                    state = 1
                                })
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = stringResource(id = R.string.date_start),
                                color = inactiveColor,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(id = R.string.date_end),
                                color = activeColor,
                                textAlign = TextAlign.End
                            )
                        }
                        AppDateWheel(value = dateEnded, onValueChange = { dateEnded = it })
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
                                    val value1 = dateStarted.ifEmpty {
                                        AppLocalDateUtils.formatDate(LocalDate.now())
                                    }
                                    val value2 = dateEnded.ifEmpty {
                                        AppLocalDateUtils.formatDate(LocalDate.now())
                                    }
                                    val date1 = AppLocalDateUtils.parseDate(value1)
                                    val date2 = AppLocalDateUtils.parseDate(value2)
                                    if (date1.isAfter(date2)) {
                                        AppToastUtil.shortShow(
                                            App.getContext()
                                                .getString(R.string.date_end_not_b4_start)
                                        )
                                        return@AppFilledButton
                                    }
                                    if (limit > 0) {
                                        val period = Period.between(date1, date2)
                                        if (period.days > limit) {
                                            AppToastUtil.shortShow(
                                                App.getContext().getString(R.string.date_range_over)
                                                    .format(period.days.toString())
                                            )
                                            return@AppFilledButton
                                        }
                                    }
                                    onValueChange(listOf(value1, value2))
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
fun AppDateRangePreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg2Color)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            AppDateRangePicker(
                values = null,
                borderWidth = 1.dp,
                fontSize = 12.sp,
            )
        }
    }
}
