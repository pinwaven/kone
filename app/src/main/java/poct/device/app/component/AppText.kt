package poct.device.app.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import poct.device.app.R
import poct.device.app.theme.bgColor
import poct.device.app.theme.borderColor
import poct.device.app.theme.inputBgColor
import poct.device.app.theme.inputFontColor
import poct.device.app.theme.primaryColor

@Composable
fun AppText(
    value: String,
    fontSize: TextUnit = 14.sp,
    fontColor: Color = inputFontColor,
    borderWidth: Dp = 0.dp,
    left: (@Composable () -> Unit)? = null,
    right: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    var containerModifier = Modifier
        .fillMaxSize()
        .background(inputBgColor)
    if (borderWidth.value > 0) {
        containerModifier = containerModifier.border(
            width = borderWidth,
            color = borderColor,
            shape = RoundedCornerShape(4.dp)
        )
    }
    Box(modifier = containerModifier) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
            ) {
                val boxWidth = this.maxWidth
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var dLeft = 0.dp
                    if (left != null) {
                        Box(modifier = Modifier.size(24.dp)) {
                            left()
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        dLeft += 36.dp
                    }
                    val inputModifier = Modifier.width(boxWidth - 48.dp - dLeft)
                    Row(
                        modifier = Modifier
                            .width(boxWidth - 48.dp - dLeft)
                            .fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClick?.apply { this() } },
                            text = value,
                            fontSize = fontSize,
                            color = fontColor
                        )
                    }
                    if (right != null) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(modifier = Modifier.size(24.dp)) {
                            right()
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AppTextPreview() {
    AppPreviewWrapper {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .background(bgColor)
        ) {
            Row(modifier = Modifier.height(40.dp)) {
                AppText(value = "", borderWidth = 1.dp)
            }
            Row(modifier = Modifier.height(40.dp)) {
                AppText(value = "123", borderWidth = 1.dp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.height(40.dp)) {
                AppText(
                    value = "xde",
                    borderWidth = 1.dp,
                    left = {
                        Icon(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { },
                            painter = painterResource(id = R.mipmap.time_icon),
                            tint = primaryColor,
                            contentDescription = ""
                        )
                    },
                    right = {
                        Icon(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { },
                            painter = painterResource(id = R.mipmap.time_icon),
                            tint = primaryColor,
                            contentDescription = ""
                        )
                    })
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.height(40.dp)) {
                AppText(
                    value = "",
                    borderWidth = 1.dp,
                    left = {
                        Icon(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { },
                            painter = painterResource(id = R.mipmap.time_icon),
                            tint = primaryColor,
                            contentDescription = ""
                        )
                    })
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.height(40.dp)) {
                AppText(
                    value = "",
                    borderWidth = 1.dp,
                    right = {
                        Icon(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { },
                            painter = painterResource(id = R.mipmap.time_icon),
                            tint = primaryColor,
                            contentDescription = ""
                        )
                    })
            }
        }
    }
}
