package poct.device.app.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 值显示包装类
 */
@Composable
fun AppValueWrapper(
    text: String = "",
    value: Any,
    labelWidth: Dp = 80.dp,
    height: Dp = 24.dp,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (labelWidth.value > 0) {
            Text(
                modifier = Modifier.width(labelWidth),
                text = text,
                fontSize = 12.sp
            )
        }
        Text(
            text = value.toString(),
            fontSize = 12.sp
        )
    }
}