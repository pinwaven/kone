package poct.device.app.ui.workconfig

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import poct.device.app.R
import poct.device.app.component.AppDivider
import poct.device.app.component.AppFieldWrapper
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppOutlinedButton
import poct.device.app.component.AppTextField
import poct.device.app.theme.bgColor
import poct.device.app.theme.tipFontColor


@Composable
fun WorkConfigCardImportByScanner(
    visible: Boolean = false,
    scannerInfo: State<String>,
    onCancel: () -> Unit = {},
    onImportByScanner: () -> Unit = {},
    onScannerUpdate: (curScannerInfo: String) -> Unit,
) {
    if(!visible) {
        return
    }
    var scannerContent = scannerInfo.value
    Dialog(onDismissRequest = onCancel) {
        Card(
            modifier = Modifier.width(280.dp).height(300.dp)
                .padding(15.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
                    .padding(start = 15.dp, end = 15.dp, top = 15.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    text = stringResource(id = R.string.btn_label_import_scanner)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Image(
                    modifier = Modifier
                        .width(102.dp)
                        .height(70.dp),
                    painter = painterResource(id = R.mipmap.icon_saoma),
                    contentDescription = ""
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    modifier = Modifier.padding(10.dp, 0.dp, 10.dp, 0.dp),
                    text = stringResource(id = R.string.work_info_scanner),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = tipFontColor
                )
                Spacer(modifier = Modifier.height(10.dp))
                AppFieldWrapper(
                    labelWidth = 5.dp,
                    height = 40.dp,
                    borderWidth = 1.dp,
                    trailingIcon = @Composable {
                        Image(imageVector = Icons.Outlined.Clear, // 清除图标
                            contentDescription = null,
                            modifier = Modifier.clickable { onScannerUpdate("") }) // 给图标添加点击事件，点击就清空text
                    },
                ) {
                    AppTextField(
                        focusState = true,
                        value = scannerContent,
                        onValueChange = { onScannerUpdate(it.trim()) })
                }
                AppDivider()
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AppOutlinedButton(
                        modifier = Modifier
                            .width(100.dp)
                            .height(36.dp),
                        onClick = { onCancel() },
                        text = stringResource(id = R.string.btn_label_exit)
                    )
                    AppFilledButton(
                        modifier = Modifier
                            .width(100.dp)
                            .height(36.dp),
                        onClick = { onImportByScanner() },
                        text = stringResource(id = R.string.btn_label_import)
                    )
                }
            }
        }
    }

}