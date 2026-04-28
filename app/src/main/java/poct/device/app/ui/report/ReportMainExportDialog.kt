package poct.device.app.ui.report


import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import poct.device.app.R
import poct.device.app.component.AppAlert
import poct.device.app.theme.filledFontColor
import poct.device.app.theme.fontColor

@Composable
fun ReportMainExportDialog(
    state: String,
    exportUrl: String,
    errorMsg: String = "unknown error",
    onErrorOk: () -> Unit,
    onExportDone: () -> Unit,
) {
    if (state == ReportMainViewModel.EVT_EXPORT_ACTION_ERROR) {
        AppAlert(
            title = stringResource(id = R.string.confirm_title_remind),
            content = errorMsg,
            visible = true,
            onOk = onErrorOk
        )
    } else if (state == ReportMainViewModel.EVT_EXPORT_ACTION_ING || state == ReportMainViewModel.EVT_EXPORT_ACTION_DONE || state == ReportMainViewModel.EVT_EXPORT_ACTION_SUCCESS) {
        Dialog(
            onDismissRequest = {
                if (state == ReportMainViewModel.EVT_EXPORT_ACTION_DONE) {
                    onExportDone()
                }
            },
            properties = DialogProperties(dismissOnClickOutside = false)
        ) {
            Surface(
                modifier = Modifier
                    .width(180.dp)
                    .height(170.dp),
                shape = RoundedCornerShape(4.dp)
            ) {
                if (state == ReportMainViewModel.EVT_EXPORT_ACTION_DONE) {
                    onExportDone()
                } else if (state == ReportMainViewModel.EVT_EXPORT_ACTION_SUCCESS) {
//                    val msg = stringResource(id = R.string.export_success).format(exportUrl)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(34.dp))
                        Image(
                            modifier = Modifier.size(68.dp),
                            painter = painterResource(id = R.mipmap.tips_suc_img),
                            contentDescription = ""
                        )
                        Spacer(modifier = Modifier.height(13.5.dp))
                        Text(
                            text = stringResource(id = R.string.export_success),
                            color = fontColor,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(34.dp))
                        Image(
                            modifier = Modifier.size(68.dp),
                            painter = painterResource(id = R.mipmap.bgbs_img),
                            contentDescription = ""
                        )
                        Spacer(modifier = Modifier.height(13.5.dp))
                        Text(
                            text = stringResource(id = R.string.export_report),
                            color = filledFontColor,
                            fontSize = 14.sp
                        )
                    }
                }

            }
        }
    }
}