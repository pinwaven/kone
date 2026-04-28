package poct.device.app.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import poct.device.app.theme.sepColor


/**
 * 0代表横向，1代表竖向
 */

@Composable
fun AppDivider(type: Int = 0, size: Dp = 0.5.dp, color: Color = sepColor) {
    if (type == 0) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(size)
                .background(color)
        )
    } else {
        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .width(size)
                .background(color)
        )
    }
}

