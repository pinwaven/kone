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
import androidx.compose.runtime.mutableStateOf
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
import java.time.LocalDate

@Composable
fun AppDateWheel(
    value: String,
    fontSize: TextUnit = 14.sp,
    onValueChange: (it: String) -> Unit = {},
) {
    val date =
        if (value.isEmpty()) LocalDate.now() else AppLocalDateUtils.parseDate(value)
    var yearValue by remember { mutableIntStateOf(date.year) }
    var monthValue by remember { mutableIntStateOf(date.monthValue) }
    var dayValue by remember { mutableIntStateOf(date.dayOfMonth) }
    val yearOptions = AppDictUtils.yearOptions()
    val monthOptions = AppDictUtils.monthOptions()
    var dayOptions by remember { mutableStateOf(AppDictUtils.dayOptions(yearValue, monthValue)) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        AppWheel(
            modifier = Modifier.weight(1F),
            value = yearValue,
            fontSize = fontSize,
            options = yearOptions,
            onValueChange = {
                yearValue = it
                dayOptions = AppDictUtils.dayOptions(yearValue, monthValue)
                if (dayValue > dayOptions.size) {
                    dayValue = dayOptions.size
                }
                onValueChange(
                    AppLocalDateUtils.formatDate(
                        LocalDate.of(
                            yearValue,
                            monthValue,
                            dayValue
                        )
                    )
                )
            }
        )
        Spacer(modifier = Modifier.width(12.dp))
        AppWheel(
            modifier = Modifier.weight(1F),
            value = monthValue,
            fontSize = fontSize,
            options = monthOptions,
            onValueChange = {
                monthValue = it
                dayOptions = AppDictUtils.dayOptions(yearValue, monthValue)
                if (dayValue > dayOptions.size) {
                    dayValue = dayOptions.size
                }
                onValueChange(
                    AppLocalDateUtils.formatDate(
                        LocalDate.of(
                            yearValue,
                            monthValue,
                            dayValue
                        )
                    )
                )
            }
        )
        Spacer(modifier = Modifier.width(12.dp))
        AppWheel(
            modifier = Modifier.weight(1F),
            value = dayValue,
            fontSize = fontSize,
            options = dayOptions,
            onValueChange = {
                dayValue = it
                onValueChange(
                    AppLocalDateUtils.formatDate(
                        LocalDate.of(
                            yearValue,
                            monthValue,
                            dayValue
                        )
                    )
                )
            }
        )
    }
}

@Preview
@Composable
fun AppDateWheelPreview() {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
//        AppDateWheel(value = , options = )<Int>(
//
//        )
    }
}
