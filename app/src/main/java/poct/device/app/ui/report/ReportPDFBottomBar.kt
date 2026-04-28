package poct.device.app.ui.report


import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import poct.device.app.R
import poct.device.app.component.AppBottomBar
import poct.device.app.component.AppFilledButton

/**
 * 页面底部按钮
 */
@Composable
fun ReportPDFBottomBar(
    onLisConfirm: () -> Unit,
    onEdit: () -> Unit,
    onPrintConfirm: () -> Unit,
) {
    AppBottomBar {
        // TODO 简化信息
//        AppFilledButton(
//            modifier = Modifier
//                .width(90.dp)
//                .height(40.dp),
//            colors = ButtonDefaults.buttonColors(
//                containerColor = greenColor
//            ),
//            onClick = {
//                onLisConfirm()
//            },
//            text = stringResource(id = R.string.report_upload)
//        )
//        Spacer(modifier = Modifier.width(10.dp))
        AppFilledButton(
            modifier = Modifier
                .width(90.dp)
                .height(40.dp), onClick = {
                onEdit()
            }, text = stringResource(id = R.string.report_edit)
        )
        // TODO 简化信息
//        Spacer(modifier = Modifier.width(10.dp))
//        AppFilledButton(
//            modifier = Modifier
//                .width(90.dp)
//                .height(40.dp),
//            onClick = { onPrintConfirm() },
//            text = stringResource(id = R.string.work_report_print)
//        )
    }
}