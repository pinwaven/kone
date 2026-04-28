package poct.device.app.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import poct.device.app.R
import poct.device.app.theme.fontColor


/**
 */
@Composable
fun AppAlert(
    visible: Boolean = false,
    title: String = stringResource(id = R.string.confirm_title_remind),
    content: String = "",
    okText: String? = null,
    onOk: () -> Unit = {},
    paddingValues: PaddingValues = PaddingValues(
        start = 0.dp,
        end = 0.dp,
        top = 24.dp,
        bottom = 20.dp
    ),
) {
    if (!visible) {
        return
    }
    Dialog(
        onDismissRequest = onOk,
        properties = DialogProperties(dismissOnClickOutside = false)
    )  {
        Surface(
            modifier = Modifier
                .width(280.dp)
                .height(160.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = fontColor,
                    text = title
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = fontColor,
                    text = content
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(36.dp),
                        onClick = onOk,
                        text = okText ?: stringResource(id = R.string.btn_label_ok),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}


@Preview
@Composable
fun AppAlertPreview() {
    AppAlert(
        visible = true,
        title = "测试",
        content = "未插入“U盘”，请插入“U盘”后重试"
    )
}
