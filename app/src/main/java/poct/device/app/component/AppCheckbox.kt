package poct.device.app.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import poct.device.app.theme.bgColor
import poct.device.app.theme.filledFontColor


/**
 * start与end同时设置，仅start有效
 * top与bottom同时设置，仅top有效
 */
@Composable
fun AppCheckbox(
    text: String,
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    fontSize: TextUnit = TextUnit.Unspecified,
    onCheckedChange: ((Boolean) -> Unit) = {},
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            colors = CheckboxDefaults.colors(
                checkedColor = filledFontColor,
                uncheckedColor = filledFontColor
            ),
            onCheckedChange = { onCheckedChange(!checked) })
        Text(text = text, fontSize = fontSize)
    }
}

@Preview
@Composable
fun AppCheckboxPreview() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        AppCheckbox(
            modifier = Modifier
                .width(80.dp)
                .height(24.dp),
            checked = true,
            fontSize = 12.sp,
            text = "选 中"
        )
        AppCheckbox(
            modifier = Modifier
                .width(120.dp)
                .height(24.dp),
            checked = false,
            fontSize = 12.sp,
            text = "未选中"
        )
        AppCheckbox(
            modifier = Modifier
                .width(120.dp)
                .height(24.dp),
            checked = false,
            fontSize = 12.sp,
            text = ""
        )
    }
}
