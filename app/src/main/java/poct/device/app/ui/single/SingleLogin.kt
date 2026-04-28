package poct.device.app.ui.single

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.R
import poct.device.app.RouteConfig
import poct.device.app.bean.LoginBean
import poct.device.app.component.AppAlert
import poct.device.app.component.AppFieldWrapper
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppPowerButton
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTextField
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewWrapper
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.theme.bgColor
import poct.device.app.theme.filledFontColor
import poct.device.app.utils.app.AppFormUtils


/**
 * 页面定义
 */
@Composable
fun SingleLogin(navController: NavController, viewModel: SingleLoginViewModel = viewModel()) {
    val viewState = viewModel.viewState.collectAsState()
    val actionState = viewModel.actionState.collectAsState()
    val loginBean = viewModel.loginBean.collectAsState()
    LaunchedEffect(viewState.value) {
        if (viewState.value == ViewState.Default) {
            viewModel.onLoad()
        }
    }
    AppViewWrapper(
        viewState = viewState,
        onErrorClick = { navController.popBackStack() }) {
        AppScaffold(
            topBar = {
                AppTopBar(
                    navController = navController,
                    title = LocalContext.current.getString(R.string.login),
                    homeEnabled = false,
                    backEnabled = false,
                    onBack = { navController.navigate(RouteConfig.SINGLE_SPLASH) }
                )
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
            ) {
                Column {
                    PageLoginBody(
                        loginBean = loginBean,
                        onRecordUpdate = { viewModel.loginBean.value = it },
                        onLogin = {
                            viewModel.onLogin {
                                navController.navigate(RouteConfig.HOME)
                            }
                        }
                    )
                }

                AppPowerButton(start = 24.dp, bottom = 40.dp)
            }
        }
    }
    SingleLoginInteraction(
        actionState = actionState,
        onClearInteraction = { viewModel.onClearInteraction() },
    )
}

@Composable
fun SingleLoginInteraction(actionState: State<ActionState>, onClearInteraction: () -> Unit) {
    if (actionState.value.event == SingleLoginViewModel.EVT_LOGIN_FAILED) {
        AppAlert(
            visible = true,
            content = actionState.value.msg!!,
            onOk = { onClearInteraction() }
        )
    }

}

/**
 * 内容主体
 */
@Composable
fun PageLoginBody(
    loginBean: State<LoginBean>,
    onRecordUpdate: (LoginBean) -> Unit,
    onLogin: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        // logo区域
        SingleLoginLogo()
        // 字段区域
        SingleLoginForm(
            loginBean = loginBean.value,
            onRecordUpdate = onRecordUpdate,
            onLogin = onLogin
        )
    }
}

@Composable
private fun SingleLoginForm(
    loginBean: LoginBean,
    onRecordUpdate: (LoginBean) -> Unit,
    onLogin: () -> Unit,
) {
    val username = loginBean.username
    val password = loginBean.password
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
    ) {
        AppFieldWrapper(labelWidth = 0.dp, borderWidth = 1.dp) {
            AppTextField(
                value = username,
                placeHolder = stringResource(id = R.string.login_ph_username),
                borderWidth = 0.dp,
                fontSize = 16.sp,
                left = {
                    Icon(
                        painter = painterResource(id = R.mipmap.login_icon_zhanghao),
                        tint = filledFontColor,
                        contentDescription = null
                    )
                },
                onValueChange = {
                    onRecordUpdate(loginBean.copy(username = AppFormUtils.regulateLength(it, 16)))
                }
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        AppFieldWrapper(labelWidth = 0.dp, borderWidth = 1.dp) {
            AppTextField(
                value = password,
                visualTransformation = PasswordVisualTransformation(),
                placeHolder = stringResource(id = R.string.login_ph_pwd),
                borderWidth = 0.dp,
                fontSize = 16.sp,
                left = {
                    Icon(
                        painter = painterResource(id = R.mipmap.login_icon_mima),
                        tint = filledFontColor,
                        contentDescription = null
                    )
                },
                onValueChange = {
                    onRecordUpdate(loginBean.copy(password = AppFormUtils.regulateLength(it, 16)))
                }
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        SingleLoginAction(onLogin = onLogin)
    }
}

@Composable
private fun SingleLoginAction(
    onLogin: () -> Unit,
) {
    AppFilledButton(
        modifier = Modifier
            .height(48.dp)
            .fillMaxWidth(),
        text = stringResource(R.string.login),
        fontSize = 16.sp,
        onClick = { onLogin() },
    )
}

@Composable
private fun SingleLoginLogo() {
    Spacer(modifier = Modifier.height(180.dp))
//    Spacer(modifier = Modifier.height(98.dp))
//    Image(
//        modifier = Modifier
//            .fillMaxWidth(),
//        painter = painterResource(id = R.mipmap.logo),
//        contentScale = ContentScale.Crop,
//        contentDescription = null
//    )
//    Image(
//        painter = painterResource(id = R.mipmap.vh_logo), contentDescription = "",
//        modifier = Modifier
//            .fillMaxWidth()
//            .wrapContentSize(Alignment.Center)
//    )
//    Spacer(modifier = Modifier.height(12.dp))
//    Text(
//        modifier = Modifier
//            .fillMaxWidth(),
//        textAlign = TextAlign.Center,
//        color = primaryColor,
//        text = stringResource(R.string.app_name)
//    )
//    Spacer(modifier = Modifier.height(27.5.dp))
}

@Preview
@Composable
fun PageLoginPreview() {
    val navController = rememberNavController()
    val viewModel: SingleLoginViewModel = viewModel()
    LaunchedEffect(null) {
        viewModel.viewState.value = ViewState.LoadSuccess()
    }

    AppPreviewWrapper {
        SingleLogin(navController, viewModel)
    }
}