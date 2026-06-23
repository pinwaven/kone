package poct.device.app.ui.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.ui.res.painterResource
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.App
import poct.device.app.BuildConfig
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.RouteConfig
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppMenuCard
import poct.device.app.component.AppMenuCardItem
import poct.device.app.component.AppOutlinedButton
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewWrapper
import poct.device.app.entity.User
import poct.device.app.state.ViewState
import poct.device.app.theme.bgColor
import poct.device.app.theme.borderColor
import poct.device.app.theme.fontColor
import poct.device.app.theme.inputBgColor
import poct.device.app.theme.inputFontColor
import poct.device.app.ui.home.HomeWorkPre
import poct.device.app.utils.app.AppToastUtil
import kotlinx.coroutines.delay

/**
 * 页面定义
 */
@Composable
fun SettingMain(
    navController: NavController,
    viewModel: SettingMainViewModel = viewModel(),
) {
    val viewState = viewModel.viewState.collectAsState()
    LaunchedEffect(viewState.value) {
        if (viewState.value == ViewState.Default) {
            viewModel.onLoad()
        }
    }

    AppViewWrapper(
        viewState = viewState,
        onErrorClick = { navController.popBackStack() }
    ) {
        AppScaffold(
            topBar = {
                AppTopBar(
                    navController = navController,
                    title = "",
                    homeEnabled = true,
                    showNanoEnvironmentBadge = true,
                )
            },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
            ) {
                Column {
                    SettingMainBody(navController, viewModel)
                }
            }
        }
    }
}

/**
 * 内容主体
 */
@Composable
fun SettingMainBody(
    navController: NavController,
    viewModel: SettingMainViewModel
) {
    val workPreVisible = viewModel.workPreVisible.collectAsState()
    var factoryTestPasswordVisible by remember { mutableStateOf(false) }
    var factoryTestPassword by remember { mutableStateOf("") }
    fun submitFactoryTestPassword() {
        val result = submitFactoryTestPassword(factoryTestPassword) {
            AppParams.runtimeModeState.unlockFactoryTest(it)
        }
        factoryTestPasswordVisible = result.dialogVisible
        factoryTestPassword = result.password
        if (result.navigateToFactoryTest) {
            navController.navigate(RouteConfig.SAMPLE_SERIAL)
        }
        if (result.showWrongPassword) {
            AppToastUtil.shortShow(App.getContext().getString(R.string.msg_wrong_password))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp)
    ) {
        AppMenuCard(
            navController = navController,
            title = stringResource(id = R.string.sys_fun_menu)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AppMenuCardItem(
                    navController = navController,
                    label = stringResource(id = R.string.sys_config_other_wlan),
                    painter = painterResource(id = R.mipmap.wlan_icon),
                    onClick = {
                        viewModel.sysWifiConfig()
                    }
                )
                AppMenuCardItem(
                    navController = navController,
                    label = stringResource(id = R.string.sys_fun_menu_device),
                    painter = painterResource(id = R.mipmap.xtxx_icon),
                    onClick = { navController.navigate(RouteConfig.SYS_FUN_INFO) }
                )
                AppMenuCardItem(
                    navController = navController,
                    label = stringResource(id = R.string.sys_fun_api_test_menu),
                    painter = painterResource(id = R.mipmap.tjsjk_fwz_icon),
                    onClick = { navController.navigate(RouteConfig.SYS_FUN_API_TEST) }
                )
                // TODO 简化信息
//                AppMenuCardItem(
//                    navController = navController,
//                    label = stringResource(id = R.string.report_main),
//                    painter = painterResource(id = R.mipmap.ybjc_ckbg_icon_def),
//                    onClick = { navController.navigate(RouteConfig.REPORT) }
//                )
//
//                if (AppParams.curUser.role != User.ROLE_CHECKER) {
//                    AppMenuCardItem(
//                        navController = navController,
//                        label = stringResource(id = R.string.sys_fun_xtpz_user),
//                        painter = painterResource(id = R.mipmap.yhgl_icon),
//                        onClick = { navController.navigate(RouteConfig.SYS_FUN_USER) }
//                    )
//                }
            }

//            if (AppParams.curUser.role == User.ROLE_DEV) {
//                Spacer(modifier = Modifier.height(24.dp))
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceEvenly
//                ) {
//                    AppMenuCardItem(
//                        navController = navController,
//                        label = stringResource(id = R.string.work_config),
//                        painter = painterResource(id = R.mipmap.sjk_icon),
//                        onClick = { viewModel.onWorkConfigCard(navController) }
//                    )
//                }
//            }

            Spacer(modifier = Modifier.height(20.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        AppMenuCard(
            navController = navController,
            title = stringResource(id = R.string.sys_fun_system)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AppMenuCardItem(
                    navController = navController,
                    label = stringResource(id = R.string.sys_config_sys),
                    painter = painterResource(id = R.mipmap.nbsz_icon),
                    onClick = { navController.navigate(RouteConfig.SYS_CONFIG_SYS_COMBINE) }
                )
                AppMenuCardItem(
                    navController = navController,
                    label = stringResource(id = R.string.home_work_pre_title),
                    painter = painterResource(id = R.mipmap.sbcsh_icon),
                    onClick = { viewModel.workPreVisible.value = true }
                )

                if (AppParams.curUser.role != User.ROLE_CHECKER) {
                    AppMenuCardItem(
                        navController = navController,
                        label = stringResource(id = R.string.factory_test),
                        painter = painterResource(id = R.mipmap.jcjdjz_icon),
                        onClick = {
                            if (AppParams.runtimeModeState.isFactoryTestUnlocked()) {
                                navController.navigate(RouteConfig.SAMPLE_SERIAL)
                            } else {
                                factoryTestPassword = ""
                                factoryTestPasswordVisible = true
                            }
                        }
                    )
                }

                HomeWorkPre(
                    visible = workPreVisible.value,
                    onClose = { viewModel.workPreVisible.value = false },
                    onOk = {
                        viewModel.workPreVisible.value = false
                    }
                )
                FactoryTestPasswordDialog(
                    visible = factoryTestPasswordVisible,
                    password = factoryTestPassword,
                    onPasswordChange = { factoryTestPassword = it.filter(Char::isDigit).take(16) },
                    onCancel = {
                        factoryTestPasswordVisible = false
                        factoryTestPassword = ""
                    },
                    onConfirm = ::submitFactoryTestPassword
                )
            }
            if (AppParams.curUser.role != User.ROLE_CHECKER) {
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AppMenuCardItem(
                        navController = navController,
                        label = stringResource(id = R.string.after_sale_temp),
                        painter = painterResource(id = R.mipmap.lscz_icon),
                        onClick = { navController.navigate(RouteConfig.TEMP_OPERATION) }
                    )
                    AppMenuCardItem(navController = navController, label = "")
                    AppMenuCardItem(navController = navController, label = "")
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${stringResource(id = R.string.after_sale_version_current)}: ${BuildConfig.VERSION_NAME}",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun FactoryTestPasswordDialog(
    visible: Boolean,
    password: String,
    onPasswordChange: (String) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
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
        var textFieldValue by remember {
            mutableStateOf(
                TextFieldValue(
                    text = password,
                    selection = TextRange(password.length)
                )
            )
        }
        LaunchedEffect(password) {
            if (textFieldValue.text != password) {
                textFieldValue = TextFieldValue(
                    text = password,
                    selection = TextRange(password.length)
                )
            }
        }
        LaunchedEffect(Unit) {
            delay(100)
            focusRequester.requestFocus()
            keyboardController?.show()
        }
        Surface(
            modifier = Modifier
                .width(280.dp)
                .height(190.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 15.dp, end = 15.dp, top = 24.dp, bottom = 20.dp),
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
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = fontColor,
                    text = stringResource(id = R.string.factory_test_password_content)
                )
                Row(modifier = Modifier.height(36.dp)) {
                    val placeHolder = stringResource(id = R.string.confirm_password_pwd)
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
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { onConfirm() }),
                        onValueChange = { incoming ->
                            val digits = incoming.text.filter(Char::isDigit).take(16)
                            textFieldValue = if (digits == incoming.text) {
                                incoming
                            } else {
                                TextFieldValue(
                                    text = digits,
                                    selection = TextRange(digits.length)
                                )
                            }
                            onPasswordChange(digits)
                        },
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (textFieldValue.text.isEmpty()) {
                                    Text(
                                        text = placeHolder,
                                        color = poct.device.app.theme.placeHolderColor,
                                        style = TextStyle(
                                            fontSize = 14.sp,
                                            color = inputFontColor
                                        )
                                    )
                                }
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
                        onClick = onConfirm,
                        text = stringResource(id = R.string.btn_label_ok),
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

internal data class FactoryTestPasswordSubmitResult(
    val dialogVisible: Boolean,
    val password: String,
    val navigateToFactoryTest: Boolean,
    val showWrongPassword: Boolean,
)

internal fun submitFactoryTestPassword(
    password: String,
    unlockFactoryTest: (String) -> Boolean,
): FactoryTestPasswordSubmitResult {
    return if (unlockFactoryTest(password)) {
        FactoryTestPasswordSubmitResult(
            dialogVisible = false,
            password = "",
            navigateToFactoryTest = true,
            showWrongPassword = false,
        )
    } else {
        FactoryTestPasswordSubmitResult(
            dialogVisible = true,
            password = "",
            navigateToFactoryTest = false,
            showWrongPassword = true,
        )
    }
}

@Preview
@Composable
fun SettingMainPreview() {
    AppParams.curUser.role = "dev"
    AppPreviewWrapper {
        SettingMain(rememberNavController())
    }
}
