package poct.device.app.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import poct.device.app.theme.bgColor
import poct.device.app.theme.filledFontColor


/**
 */
@Composable
fun AppOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    fontSize: TextUnit = TextUnit.Unspecified,
    color: Color = filledFontColor,
) {
    OutlinedButton(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        contentPadding = PaddingValues(0.dp),
        border = BorderStroke(
            width = 1.dp,
            color = color,
        ),
        onClick = onClick
    ) {
        Text(
            color = filledFontColor,
            fontSize = fontSize,
            text = text
        )
    }
}

@Preview
@Composable
fun AppOutlinedButtonPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        AppOutlinedButton(
            modifier = Modifier
                .width(120.dp)
                .height(36.dp), onClick = {},
            fontSize = 12.sp,
            text = "按 钮"
        )
    }
}
