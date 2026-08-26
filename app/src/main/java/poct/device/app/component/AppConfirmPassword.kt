package poct.device.app.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import poct.device.app.AppParams
import poct.device.app.MainActivity
import poct.device.app.R
import poct.device.app.theme.borderColor
import poct.device.app.theme.fontColor
import poct.device.app.theme.inputBgColor
import poct.device.app.theme.inputFontColor


/**

 */
@Composable
fun AppConfirmPassword(
    visible: Boolean = false,
    onCancel: () -> Unit = {},
    onConfirm: (String) -> Unit = { _ -> },
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
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.parent as? DialogWindowProvider)?.window
                window?.let {
                    WindowCompat.setDecorFitsSystemWindows(it, false)
                    WindowInsetsControllerCompat(it, view).apply {
                        hide(WindowInsetsCompat.Type.navigationBars())
                        systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                }
            }
        }
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
        // 每次提交后自增，重新触发下面的 LaunchedEffect 唤起数字键盘（密码错误时对话框不关闭需要重新弹键盘）
        var focusRequestTrigger by remember { mutableIntStateOf(0) }
        LaunchedEffect(focusRequestTrigger) {
            delay(100)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
        fun submit() {
            onConfirm(textFieldValue.text)
            textFieldValue = TextFieldValue("")
            focusRequestTrigger++
        }
        Surface(
            modifier = Modifier
                .wakeScreenOnTouch()
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
                    text = stringResource(id = R.string.confirm_password_title)
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = fontColor,
                    text = stringResource(id = R.string.confirm_password_content)
                )
                AppFieldWrapper(
                    labelWidth = 40.dp,
                    text = stringResource(id = R.string.confirm_password_pwd)
                ) {
                    BasicTextField(
                        value = textFieldValue,
                        modifier = Modifier
                            .fillMaxSize()
                            .focusRequester(focusRequester)
                            .background(inputBgColor)
                            .border(
                                width = 1.dp,
                                color = borderColor,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 12.dp),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = inputFontColor,
                        ),
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            autoCorrect = false,
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        onValueChange = { incoming ->
                            val digits = incoming.text.filter(Char::isDigit)
                            textFieldValue = if (digits == incoming.text) {
                                incoming
                            } else {
                                TextFieldValue(
                                    text = digits,
                                    selection = TextRange(digits.length)
                                )
                            }
                            // 系统输入法弹窗按键不经过 Activity.dispatchTouchEvent，靠按键回调唤醒屏幕
                            (AppParams.curActivity as? MainActivity)?.onUserTouch()
                        },
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                innerTextField()
                            }
                        }
                    )
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
                        onClick = { submit() },
                        text = stringResource(id = R.string.btn_label_ok),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}


@Preview
@Composable
fun AppConfirmPasswordPreview() {
    AppConfirmPassword(visible = true)
}
