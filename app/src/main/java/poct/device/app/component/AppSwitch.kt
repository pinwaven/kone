package poct.device.app.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import poct.device.app.theme.bgColor

@Composable
fun AppSwitch(
    modifier: Modifier = Modifier,
    checked: Boolean,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(
        checkedBorderColor = bgColor,
        uncheckedBorderColor = bgColor
    ),
    onCheckedChange: (Boolean) -> Unit,
) {
    Switch(
        modifier = modifier,
        checked = checked,
        colors = colors,
        enabled = enabled,
        onCheckedChange = onCheckedChange
    )
}

@Preview
@Composable
fun AppSwitchPreview() {
    var checked by remember { mutableStateOf(true) }
    LaunchedEffect(key1 = Unit) {

    }
    AppPreviewWrapper {
        Column {
            AppSwitch(
                checked = checked,
//                modifier = Modifier
//                    .width(100.dp)
//                    .height(60.dp),
            ) { }

            AppSwitch(
                modifier = Modifier
                    .width(100.dp)
                    .height(30.dp),
                checked = checked,
            ) { }
        }
    }
}