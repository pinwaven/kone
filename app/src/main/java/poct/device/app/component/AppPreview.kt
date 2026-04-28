package poct.device.app.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import poct.device.app.theme.bgColor


/**
 */
@Composable
fun AppPreviewWrapper(
    background: Color = bgColor,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(360.dp)
            .height(640.dp)
            .background(background)
    ) {
        content()
    }
}
