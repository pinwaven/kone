package poct.device.app.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import poct.device.app.R
import poct.device.app.theme.tipBgColor
import poct.device.app.theme.tipBgColor2
import poct.device.app.theme.tipFontColor1

/**
 * *
 * @date ：2022/4/14
 * @desc：正在加载提示框
 */
@Composable
fun AppViewLoading(
    msg: String? = null,
    size: Dp = 130.dp,
    padding: PaddingValues = PaddingValues(20.dp),
    content: (@Composable () -> Unit)? = null,
) {
    AppViewLoading(
        msg = msg,
        width = size,
        height = size,
        padding = padding,
        content = content
    )
}

@Composable
fun AppViewLoading(
    msg: String? = null,
    width: Dp = 130.dp,
    height: Dp = 130.dp,
    padding: PaddingValues = PaddingValues(20.dp),
    content: (@Composable () -> Unit)? = null,
) {
    content?.apply { this() }

    Surface(
        Modifier.fillMaxSize(),
        color = tipBgColor2
    ) {
        Row(Modifier
                .fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val infiniteTransition = rememberInfiniteTransition()
            val rotate by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                )
            )
            Column(
                Modifier
                    .width(width)
                    .height(height)
                    .background(color = tipBgColor, shape = RoundedCornerShape(16.dp))
                    .padding(2.dp, 20.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.mipmap.lodding_white_icon),
                    tint = tipFontColor1,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .rotate(rotate)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = msg ?: stringResource(R.string.wait_please),
                    fontSize = 14.sp,
                    color = tipFontColor1,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}



@Preview
@Composable
fun AppViewLoadingPreview() {
    AppPreviewWrapper {
        AppViewLoading()
    }
}