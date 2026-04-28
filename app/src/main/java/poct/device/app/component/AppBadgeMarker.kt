package poct.device.app.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import poct.device.app.theme.dangerColor
import poct.device.app.theme.filledFontColor

@Composable
fun AppBadgeMarker(
    size: Dp = 14.dp,
    text: String = "",
    fontSize: TextUnit = 9.sp,
    fontColor: Color = filledFontColor,
    filledColor: Color = dangerColor,
) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(size / 2),
        color = filledColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(dangerColor),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = text,
                color = fontColor,
                lineHeight = fontSize,
                fontSize = fontSize
            )
        }
    }
}

@Preview
@Composable
fun AppBadgeMarkerPreview() {
    AppPreviewWrapper {
        AppBadgeMarker(
            size = 50.dp,
            text = "12",
            fontSize = 32.sp
        )
    }
}

