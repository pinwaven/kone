package poct.device.app.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import poct.device.app.R
import poct.device.app.theme.bgColor
import poct.device.app.theme.borderColor
import poct.device.app.theme.inputBgColor
import poct.device.app.theme.inputFontColor
import poct.device.app.theme.placeHolderColor
import poct.device.app.utils.app.AppDictUtils

@Composable
fun <V> AppSelect(
    value: V,
    // key, value映射表
    options: Map<V, String>,
    placeHolder: String = stringResource(id = R.string.placeholder_select),
    fontSize: TextUnit = 14.sp,
    borderWidth: Dp = 0.dp,
    readOnly: Boolean = false,
    actionPainter: Painter = painterResource(id = R.mipmap.more_icon),
    paddingValues: PaddingValues = PaddingValues(
        start = 15.dp,
        end = 15.dp,
        top = 12.dp,
        bottom = 20.dp
    ),
    onValueChange: (it: V) -> Unit = {},
) {
    if (readOnly) {
        Row(
            modifier = Modifier.padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = AppDictUtils.label(options, value),
                fontSize = fontSize,
                color = inputFontColor
            )
        }
        return
    }

    var visible by rememberSaveable { mutableStateOf(false) }
    val valueDsp = if (options.containsKey(value)) options.getValue(value) else ""
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
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(start = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                val boxWidth = this.minWidth
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(boxWidth - 48.dp)
                            .clickable { visible = true },
                    ) {
                        if (valueDsp.isEmpty()) {
                            Text(
                                text = placeHolder,
                                color = placeHolderColor,
                                style = TextStyle(
                                    fontSize = fontSize,
                                    color = inputFontColor
                                )
                            )
                        }
                        Text(
                            text = valueDsp,
                            style = TextStyle(
                                fontSize = fontSize,
                                color = inputFontColor
                            )
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { visible = true },
                        painter = actionPainter,
                        tint = Color.Unspecified,
                        contentDescription = ""
                    )

                }
            }
        }

    }

    AppSelectOptionDialog(
        visible = visible,
        value = value,
        options = options,
        paddingValues = paddingValues,
        onCancel = { visible = false },
        onConfirm = {
            onValueChange(it)
            visible = false
        })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <V> AppSelectOptionDialog(
    visible: Boolean = false,
    value: V,
    // key, value映射表
    options: Map<V, String>,
    cancelText: String = stringResource(id = R.string.btn_label_cancel),
    confirmText: String = stringResource(id = R.string.btn_label_ok),
    onCancel: () -> Unit = {},
    paddingValues: PaddingValues,
    onConfirm: (it: V) -> Unit,
) {
    if (!visible) {
        return
    }
    var pickerValue by rememberSaveable { mutableStateOf(value) }
    Dialog(onDismissRequest = onCancel) {
        Surface(
            modifier = Modifier
                .width(280.dp)
                .height(200.dp),
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    AppWheel(
                        value = pickerValue,
                        options = options,
                        onValueChange = { pickerValue = it })
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
                        text = cancelText,
                        fontSize = 14.sp
                    )
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(36.dp),
                        onClick = { onConfirm(pickerValue) },
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
fun AppSelectPreview() {
    val options = mapOf(1 to "男", 2 to "女", 3 to "A", 4 to "B", 5 to "C")
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .background(bgColor)
    ) {
        Row(
            modifier = Modifier
                .height(40.dp)
                .padding(5.dp)
        ) {
            AppSelect<Int>(
                value = 1,
                borderWidth = 1.dp,
                options = options,
                onValueChange = {}
            )
        }
    }
}
