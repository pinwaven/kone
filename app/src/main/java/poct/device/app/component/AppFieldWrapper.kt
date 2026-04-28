package poct.device.app.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import poct.device.app.R
import poct.device.app.state.FieldState
import poct.device.app.theme.borderColor
import poct.device.app.theme.dangerColor
import poct.device.app.theme.primaryColor

@Composable
fun AppFieldWrapper(
    text: String = "",
    labelWidth: Dp = 80.dp,
    borderWidth: Dp = (-1).dp,
    background: Color = primaryColor,
    height: Dp = 48.dp,
    fontSize: TextUnit = 14.sp,
    fontWeight: FontWeight? = null,
    required: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    fieldState: FieldState? = null,
    content: @Composable (FieldState?) -> Unit,
) {
    Row(
        modifier = Modifier
            .background(color = background)
            .fillMaxWidth()
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(4.dp)
            )
            .height(height),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (labelWidth.value > 0) {
            Text(
                text = if (required) "*" else "",
                fontSize = fontSize,
                textAlign = TextAlign.Center,
                color = dangerColor,
                modifier = Modifier.width(12.dp)
            )
            Text(
                text = text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = fontSize,
                fontWeight = fontWeight,
                modifier = Modifier.width(labelWidth)
            )
        }
        Row(modifier = Modifier.weight(1F)) {
            content(fieldState)
        }
        if (fieldState != null) {
            Spacer(modifier = Modifier.width(12.dp))
        }
        var tipRes: Int = -1
        when (fieldState?.state) {
            FieldState.STATE_NORMAL -> {
                tipRes = R.mipmap.tip_p_icon
            }

            FieldState.STATE_WARNING -> {
                tipRes = R.mipmap.tip_y_icon
            }

            FieldState.STATE_ERROR -> {
                tipRes = R.mipmap.tip_r_icon
            }
        }
        if (fieldState != null) {
            var tipVisible by remember { mutableStateOf(false) }
            Image(
                modifier = Modifier
                    .size(18.dp)
                    .clickable { tipVisible = true },
                painter = painterResource(id = tipRes),
                contentDescription = "",
            )

            AppAlert(
                visible = tipVisible,
                content = fieldState.msg,
                onOk = { tipVisible = false }
            )
        }
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(12.dp))
            Row(
                modifier = Modifier
                    .width(24.dp)
                    .height(24.dp)
            ) {
                trailingIcon()
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
    }
}

@Preview
@Composable
fun AppFieldWrapperPreview() {
    AppPreviewWrapper {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            AppFieldWrapper(
                labelWidth = 80.dp,
                text = stringResource(id = R.string.sys_fun_xtpz_info_f_software)
            ) {
                Text(text = "V1.2")
            }
            AppFieldWrapper(
                labelWidth = 80.dp,
                text = stringResource(id = R.string.sys_fun_xtpz_info_f_software),
                required = true
            ) {
                AppTextField(value = "test")
            }
            AppFieldWrapper(
                labelWidth = 80.dp,
                text = stringResource(id = R.string.sys_fun_xtpz_info_f_software),
                required = true,
                trailingIcon = @Composable {
                    Image(
                        imageVector = Icons.Filled.Clear, // 清除图标
                        contentDescription = null
                    ) // 给图标添加点击事件，点击就清空text
                },
            ) {
                AppTextField(fieldState = it, value = "test")
            }
            AppFieldWrapper(
                labelWidth = 80.dp,
                text = stringResource(id = R.string.sys_fun_xtpz_info_f_software),
                required = true,
                fieldState = FieldState.normal("abc", "alalalalal")
            ) {
                AppTextField(fieldState = it, value = "test")
            }
            AppFieldWrapper(
                labelWidth = 80.dp,
                text = stringResource(id = R.string.sys_fun_xtpz_info_f_software),
                required = true,
                fieldState = FieldState.warning("abc", "alalalalal")
            ) {
                AppTextField(fieldState = it, value = "test")
            }
            AppFieldWrapper(
                labelWidth = 80.dp,
                text = stringResource(id = R.string.sys_fun_xtpz_info_f_software),
                required = true,
                fieldState = FieldState.error("abc", "alalalalal")
            ) {
                AppTextField(fieldState = it, value = "test")
            }
        }
    }
}