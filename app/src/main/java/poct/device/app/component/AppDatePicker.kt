package poct.device.app.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import poct.device.app.R
import poct.device.app.theme.bg2Color
import poct.device.app.theme.filledFontColor
import poct.device.app.theme.inputFontColor
import poct.device.app.theme.placeHolderColor
import poct.device.app.utils.app.AppLocalDateUtils
import java.time.LocalDate

@Composable
fun AppDatePicker(
    value: String,
    placeHolder: String = stringResource(id = R.string.placeholder_date),
    fontSize: TextUnit = 14.sp,
    readOnly: Boolean = false,
    borderWidth: Dp = 0.dp,
    onValueChange: (it: String) -> Unit = {},
) {
    var visible by remember { mutableStateOf(false) }
    val text = value.ifBlank { placeHolder }
    val fontColor = if (value.isBlank()) placeHolderColor else inputFontColor
    AppText(
        value = text,
        fontSize = fontSize,
        fontColor = fontColor,
        borderWidth = borderWidth,
        onClick = { visible = true },
        right = {
            if (!readOnly) {
                Icon(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { visible = true },
                    painter = painterResource(id = R.mipmap.time_icon),
                    tint = filledFontColor,
                    contentDescription = ""
                )
            }
        }
    )
    if (visible) {
        AppDatePickerInnerDialog(
            value = value,
            onDismissRequest = { visible = false },
            onValueChange = {
                onValueChange(it)
                visible = false
            }
        )
    }
}

@Composable
private fun AppDatePickerInnerDialog(
    value: String = "",
    onDismissRequest: () -> Unit = {},
    onValueChange: (it: String) -> Unit = {},
) {
    var dateState by remember { mutableStateOf(value) }
    Dialog(onDismissRequest = { onDismissRequest() }) {
        Surface(
            modifier = Modifier,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                AppDateWheel(value = dateState, onValueChange = { dateState = it })
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    AppOutlinedButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(40.dp),
                        text = stringResource(id = R.string.btn_label_cancel),
                        onClick = { onDismissRequest() })
                    Spacer(modifier = Modifier.width(10.dp))
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(40.dp),
                        text = stringResource(id = R.string.btn_label_ok),
                        onClick = {
                            onValueChange(
                                dateState.ifEmpty {
                                    AppLocalDateUtils.formatDate(LocalDate.now())
                                }
                            )
                        })
                }
            }
        }
    }
}

@Preview
@Composable
fun AppDatePickerPreview() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg2Color)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            AppDatePicker(
                value = "",
                borderWidth = 1.dp,
                fontSize = 12.sp,
            )
        }
    }
}
