package poct.device.app.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import poct.device.app.theme.activeColor
import poct.device.app.theme.inputFontColor
import poct.device.app.utils.app.AppDictUtils

@Composable
fun <V> AppRadioGroup(
    value: V,
    // key, value映射表
    options: Map<V, String>,
    // 选项间距
    gap: Dp = 10.dp,
    fontSize: TextUnit = 14.sp,
    readOnly: Boolean = false,
    readOnlyStartPadding: Dp = 12.dp,
    onValueChange: (it: V) -> Unit = {},
) {
    if (readOnly) {
        Row(
            modifier = Modifier.padding(start = readOnlyStartPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = AppDictUtils.label(options, value),
                textAlign = TextAlign.Center,
                fontSize = fontSize,
                color = inputFontColor
            )
        }
        return
    }

    val radioColors = RadioButtonDefaults.colors(
        selectedColor = activeColor
    )
    Row(
        modifier = Modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        var sep = false
        options.forEach(action = {
            if (sep) {
                Spacer(modifier = Modifier.width(gap))
            }
            RadioButton(
                modifier = Modifier,
                selected = (it.key == value),
                colors = radioColors,
                onClick = {
                    onValueChange(it.key)
                }
            )
            Text(
                modifier = Modifier.clickable {
                    onValueChange(it.key)
                },
                text = it.value,
                fontSize = fontSize
            )
            sep = true
        })
    }
}