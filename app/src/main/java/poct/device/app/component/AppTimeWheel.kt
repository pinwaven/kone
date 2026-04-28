package poct.device.app.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import poct.device.app.theme.bgColor
import poct.device.app.utils.app.AppDictUtils
import poct.device.app.utils.app.AppLocalDateUtils
import java.time.LocalTime

@Composable
fun AppTimeWheel(
    value: String,
    fontSize: TextUnit = 14.sp,
    onValueChange: (it: String) -> Unit = {},
) {
    val time =
        if (value.isEmpty()) LocalTime.now() else AppLocalDateUtils.parseTime(value)
    var hourValue by remember { mutableIntStateOf(time.hour) }
    var minuteValue by remember { mutableIntStateOf(time.minute) }
    var secondValue by remember { mutableIntStateOf(time.second) }
    val hourOptions = AppDictUtils.hourOptions()
    val minuteOptions = AppDictUtils.minuteOptions()
    var secondOptions = AppDictUtils.secondOptions()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        AppWheel(
            modifier = Modifier.weight(1F),
            value = hourValue,
            fontSize = fontSize,
            options = hourOptions,
            onValueChange = {
                hourValue = it
                onValueChange(
                    AppLocalDateUtils.formatTime(LocalTime.of(hourValue, minuteValue, secondValue))
                )
            }
        )
        Spacer(modifier = Modifier.width(12.dp))
        AppWheel(
            modifier = Modifier.weight(1F),
            value = minuteValue,
            fontSize = fontSize,
            options = minuteOptions,
            onValueChange = {
                minuteValue = it
                onValueChange(
                    AppLocalDateUtils.formatTime(LocalTime.of(hourValue, minuteValue, secondValue))
                )
            }
        )
        Spacer(modifier = Modifier.width(12.dp))
        AppWheel(
            modifier = Modifier.weight(1F),
            value = secondValue,
            fontSize = fontSize,
            options = secondOptions,
            onValueChange = {
                secondValue = it
                onValueChange(
                    AppLocalDateUtils.formatTime(LocalTime.of(hourValue, minuteValue, secondValue))
                )
            }
        )
    }
}

@Preview
@Composable
fun AppTimeWheelPreview() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        AppTimeWheel(value = "23:15:36"){}
    }
}
