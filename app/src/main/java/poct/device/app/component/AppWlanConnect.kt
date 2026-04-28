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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import poct.device.app.R
import poct.device.app.bean.WlanBean
import poct.device.app.theme.fontColor


/**

 */
@Composable
fun AppWlanConnect(
    visible: Boolean = false,
    wlanBean: WlanBean,
    onCancel: () -> Unit = {},
    onConnect: (WlanBean, String) -> Unit = { _, _ -> },
    paddingValues: PaddingValues = PaddingValues(
        start = 15.dp,
        end = 15.dp,
        top = 24.dp,
        bottom = 20.dp
    ),
) {
    if (!visible) {
        return
    }
    Dialog(onDismissRequest = onCancel) {
        var pwd by remember { mutableStateOf("") }
        Surface(
            modifier = Modifier
                .width(280.dp)
                .height(210.dp),
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
                    text = stringResource(id = R.string.wlan_connect_title)
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = fontColor,
                    text = String.format(
                        stringResource(id = R.string.wlan_connect_content),
                        wlanBean.ssid
                    )
                )
                AppFieldWrapper(
                    labelWidth = 40.dp,
                    text = stringResource(id = R.string.wlan_connect_pwd)
                ) {
                    AppTextField(
                        value = pwd,
                        visualTransformation = PasswordVisualTransformation(),
                        onValueChange = {
                            pwd = it
                        })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AppOutlinedButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(36.dp),
                        onClick = onCancel,
                        text = stringResource(id = R.string.btn_label_cancel),
                        fontSize = 14.sp
                    )
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(36.dp),
                        onClick = {
                            onConnect(wlanBean, pwd)
                        },
                        text = stringResource(id = R.string.btn_label_save),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}


@Preview
@Composable
fun AppWlanConnectPreview() {
    AppWlanConnect(
        visible = true,
        wlanBean = WlanBean(ssid = "SZTM"),
    )
}
