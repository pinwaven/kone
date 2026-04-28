package poct.device.app.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
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
fun AppFilledButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    textColor: Color = filledFontColor,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    shape: Shape = RoundedCornerShape(6.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    fontSize: TextUnit = TextUnit.Unspecified,
    enabled: Boolean = true,
    left: @Composable () -> Unit = {}
) {
    Button(
        modifier = modifier,
        onClick = onClick,
        shape = shape,
        colors = colors,
        contentPadding = contentPadding,
        enabled = enabled,
        border = null
    ) {
        left()
        Text(
            color = textColor,
            fontSize = fontSize,
            text = text
        )
    }
}

@Preview
@Composable
fun AppFilledButtonPreview() {
    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {
        AppFilledButton(
            modifier = Modifier
                .width(120.dp)
                .height(36.dp), onClick = {},
            fontSize = 12.sp,
            text = "按 钮"
        )
    }
}
