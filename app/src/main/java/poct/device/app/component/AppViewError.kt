package poct.device.app.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import poct.device.app.R
import poct.device.app.theme.bg2Color
import poct.device.app.theme.bgColor
import poct.device.app.theme.fontColor

/**
 * *
 * @date ：2022/4/18
 * @desc：请求错误视图
 */
@Composable
fun AppViewError(
    msg: String = stringResource(R.string.load_error),
    retryMsg: String = stringResource(R.string.back),
    onClick: () -> Unit = {},
    content: (@Composable () -> Unit)? = null,
) {
    content?.apply { this() }
    Dialog(onDismissRequest = { }) {
        Card(
            modifier = Modifier
                .width(220.dp)
                .height(200.dp),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(bg2Color)
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.mipmap.img_error),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = msg, fontSize = 15.sp, color = fontColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    AppFilledButton(modifier = Modifier.width(100.dp), onClick = { onClick() }, text = retryMsg)
                }
            }
        }
    }

}

@Preview
@Composable
fun AppErrorContentPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        AppViewError()
    }
}