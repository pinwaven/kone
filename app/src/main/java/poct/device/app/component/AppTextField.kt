package poct.device.app.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import poct.device.app.R
import poct.device.app.state.FieldState
import poct.device.app.theme.bgColor
import poct.device.app.theme.borderColor
import poct.device.app.theme.inputBgColor
import poct.device.app.theme.inputFontColor
import poct.device.app.theme.primaryColor

@Composable
fun AppTextField(
    value: String,
    focusState: Boolean = false,
    fieldState: FieldState? = null,
    placeHolder: String = stringResource(id = R.string.placeholder_input),
    fontSize: TextUnit = 14.sp,
    fontColor: Color = inputFontColor,
    placeHolderColor: Color = poct.device.app.theme.placeHolderColor,
    borderWidth: Dp = 0.dp,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    trimValue: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    left: (@Composable () -> Unit)? = null,
    right: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null, // 这里设置null，是因为如果有onClick事件，会导致无法输入
    onValueChange: (String) -> Unit = {},
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val realFontColor = fieldState?.color ?: fontColor
    val realPlaceHolderColor = fieldState?.color ?: placeHolderColor
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
    // 自动聚焦
    val focusRequester = remember { FocusRequester() }
    if (focusState) {
        containerModifier.focusRequester(focusRequester)
    }
    // readonly属性有BUG，切换时会报异常，所以只读时换掉输入框
    if (readOnly) {
        Box(modifier = containerModifier) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = value, fontSize = fontSize, color = realFontColor)
            }
        }
        return
    }

    BasicTextField(
        value = value,
        modifier = containerModifier,
        singleLine = singleLine,
        enabled = enabled,
        visualTransformation = visualTransformation,
        onValueChange = {
            val realValue = if (trimValue) it.trim() else it
            onValueChange(realValue)
        },
        keyboardOptions = keyboardOptions,
        textStyle = TextStyle(
            fontSize = fontSize,
            color = realFontColor
        ),
        decorationBox = {
            Box(modifier = containerModifier) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        val boxWidth = this.maxWidth
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var dLeft = 0.dp
                            if (left != null) {
                                Box(modifier = Modifier.size(24.dp)) {
                                    left()
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                dLeft += 36.dp
                            }
                            var inputModifier = Modifier.width(boxWidth - 48.dp - dLeft)
                            if (right != null) {
                                inputModifier = Modifier.width(boxWidth - 48.dp - dLeft)
                            } else {
                                inputModifier = Modifier.width(boxWidth - 12.dp - dLeft)
                            }
                            if (onClick != null) {
                                inputModifier.clickable { onClick() }
                            }
                            Box(
                                modifier = inputModifier
                            ) {
                                if (value.isEmpty()) {
                                    Text(
                                        text = placeHolder,
                                        color = realPlaceHolderColor,
                                        style = TextStyle(
                                            fontSize = fontSize,
                                            color = realFontColor
                                        )
                                    )
                                }
                                it()
                            }

                            if (right != null) {
                                Spacer(modifier = Modifier.width(12.dp))
                                Box(modifier = Modifier.size(24.dp)) {
                                    right()
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Preview
@Composable
fun AppTextFieldPreview() {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .background(bgColor)
    ) {
        Row(modifier = Modifier.height(40.dp)) {
            AppTextField(value = "", borderWidth = 1.dp, focusState = true)
        }
        Row(modifier = Modifier.height(40.dp)) {
            AppTextField(value = "123", borderWidth = 1.dp)
        }
        Row(modifier = Modifier.height(40.dp)) {
            AppTextField(value = "123", borderWidth = 1.dp, readOnly = true)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.height(40.dp)) {
            AppTextField(
                value = "",
                borderWidth = 1.dp,
                left = {
                    Icon(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { },
                        painter = painterResource(id = R.mipmap.time_icon),
                        tint = primaryColor,
                        contentDescription = ""
                    )
                },
                right = {
                    Icon(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { },
                        painter = painterResource(id = R.mipmap.time_icon),
                        tint = primaryColor,
                        contentDescription = ""
                    )
                })
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.height(40.dp)) {
            AppTextField(
                value = "",
                borderWidth = 1.dp,
                left = {
                    Icon(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { },
                        painter = painterResource(id = R.mipmap.time_icon),
                        tint = primaryColor,
                        contentDescription = ""
                    )
                })
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.height(40.dp)) {
            AppTextField(
                value = "",
                borderWidth = 1.dp,
                right = {
                    Icon(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { },
                        painter = painterResource(id = R.mipmap.time_icon),
                        tint = primaryColor,
                        contentDescription = ""
                    )
                })
        }
    }
}
