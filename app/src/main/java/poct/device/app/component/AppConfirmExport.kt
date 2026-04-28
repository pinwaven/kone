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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun AppConfirmExport(
    visible: Boolean = false,
    title: String = stringResource(id = R.string.confirm_export_title),
    content: String = stringResource(id = R.string.confirm_export_choose),
    cancelText: String = stringResource(id = R.string.btn_label_cancel),
    confirmText: String = stringResource(id = R.string.btn_label_export),
    onCancel: () -> Unit = {},
    onConfirm: (expType: Int) -> Unit = {},
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
    var expType by remember { mutableIntStateOf(0) }
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
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

                val options = mapOf(
                    0 to stringResource(id = R.string.confirm_export_u_disk),
                    1 to stringResource(id = R.string.confirm_export_disk),
//                    2 to stringResource(id = R.string.confirm_export_email),
                )
                AppRadioGroup(
                    value = expType,
                    options = options,
                    gap = 0.dp,
                    onValueChange = { expType = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AppOutlinedButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(36.dp),
                        onClick = onCancel,
                        text = cancelText,
                        fontSize = 14.sp
                    )
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(36.dp),
                        onClick = { onConfirm(expType) },
                        text = confirmText,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}


@Preview
@Composable
fun AppConfirmExportPreview() {
    AppConfirmExport(visible = true)
}
