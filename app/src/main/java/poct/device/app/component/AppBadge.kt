package poct.device.app.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
fun AppBadge(
    modifier: Modifier = Modifier,
    text: String = "",
    size: Dp = 14.dp,
    fontSize: TextUnit = 9.sp,
    fontColor: Color = filledFontColor,
    filledColor: Color = dangerColor,
    shown: Boolean = text.isNotBlank(),
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier.then(modifier)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomStart
        ) {
            content()
        }
        if (shown) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopEnd
            ) {
                AppBadgeMarker(
                    size = size,
                    text = text,
                    fontSize = fontSize,
                    fontColor = fontColor,
                    filledColor = filledColor
                )
            }
        }
    }
}


@Preview
@Composable
fun AppBadgePreview() {
    AppPreviewWrapper {
        AppBadge(modifier = Modifier.size(50.dp), text = "") {
            AppFilledButton(
                modifier = Modifier
                    .width(20.dp)
                    .height(20.dp),
                text = "",
                onClick = { /*TODO*/ }) {

            }
        }
        AppBadge(modifier = Modifier.size(50.dp), text = "", shown = true) {
            AppFilledButton(
                modifier = Modifier
                    .width(20.dp)
                    .height(20.dp),
                text = "",
                onClick = { /*TODO*/ }) {

            }
        }
        AppBadge(modifier = Modifier.size(50.dp), text = "1") {
            AppFilledButton(
                modifier = Modifier
                    .width(20.dp)
                    .height(20.dp),
                text = "",
                onClick = { /*TODO*/ }) {

            }
        }
    }
}

