package poct.device.app.ui.sysfun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.R
import poct.device.app.RouteConfig
import poct.device.app.bean.ConfigInfoBean
import poct.device.app.bean.ConfigInfoV2Bean
import poct.device.app.component.AppAlert
import poct.device.app.component.AppBottomBar
import poct.device.app.component.AppConfirm
import poct.device.app.component.AppDivider
import poct.device.app.component.AppFieldWrapper
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppOutlinedButton
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTextField
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewLoading
import poct.device.app.component.AppViewWrapper
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.theme.bgColor
import poct.device.app.utils.app.AppSampleUtils


/**
 * 页面定义
 */
@Composable
fun SysFunInfo(navController: NavController, viewModel: SysFunInfoViewModel = viewModel()) {
    // 监听 SavedStateHandle 中的值变化
    val shouldRefresh by navController.currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<Boolean?>("shouldRefresh", null)
        ?.collectAsState()
        ?: remember { mutableStateOf(null) }

    val viewState by viewModel.viewState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
//    val bean by viewModel.bean.collectAsState()
    val bean by viewModel.beanV2.collectAsState()
    val mode by viewModel.mode.collectAsState()

    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh == true) {
            viewModel.onLoadV2()
            // 清除标志
            navController.currentBackStackEntry
                ?.savedStateHandle
                ?.remove<Boolean>("shouldRefresh")
        }
    }

    LaunchedEffect(viewState) {
        if (viewState == ViewState.Default) {
//            viewModel.onLoad()
            viewModel.onLoadV2()
        }
    }

    val title =
        if (mode == "view") stringResource(id = R.string.sys_fun_xtpz_info)
        else stringResource(id = R.string.sys_fun_xtpz_info_modify)

    AppViewWrapper(
        viewState = viewState,
        onErrorClick = { navController.popBackStack() }) {
        AppScaffold(
            topBar = {
                AppTopBar(
                    navController = navController,
                    title = title,
                    backEnabled = true,
                    onBack = {
                        if (mode == "view") {
                            navController.popBackStack()
                        } else {
                            viewModel.onBackConfirm()
                        }
                        viewModel.beanV2.value = ConfigInfoV2Bean.Empty
                    }
                )
            },
            bottomBar = {
                // TODO 简化信息
//                SysFunInfoBottomBar(
//                    mode = mode,
//                    onModifyPre = { viewModel.onModifyPre() },
//                    onBack = {
//                        viewModel.onBackConfirm()
//                    },
//                    onModify = { viewModel.onSave() }
//                )
            }
        ) {
            SysFunInfoBody(
                navController = navController,
                bean = bean,
                mode = mode,
                onBeanUpdate = { viewModel.onBeanUpdate(it) }
            )
        }
    }

    SysFunInfoInteraction(
        actionState = actionState,
        onClearInteraction = { viewModel.onClearInteraction() }
    ) {
        viewModel.onClearInteraction()
        viewModel.onBack()
//        viewModel.onLoad()
        viewModel.onLoadV2()
    }
}


@Composable
fun SysFunInfoInteraction(
    actionState: ActionState,
    onClearInteraction: () -> Unit,
    onBackPage: () -> Unit,
) {
    if (actionState.event == SysFunInfoViewModel.EVT_EXIT) {
        AppConfirm(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = stringResource(id = R.string.sys_fun_xtpz_exit),
            onCancel = { onClearInteraction() },
            onConfirm = { onBackPage() }
        )
    } else if (actionState.event == SysFunInfoViewModel.EVT_LOADING) {
        AppViewLoading(actionState.msg)
    } else if (actionState.event == SysFunInfoViewModel.EVT_SAVE_DONE) {
        AppAlert(
            visible = true,
            content = stringResource(id = R.string.save_ok),
            onOk = { onClearInteraction() }
        )
    } else if (actionState.event == SysFunInfoViewModel.EVT_CONTACT_ADMIN) {
        AppAlert(
            visible = true,
            content = stringResource(id = R.string.contact_admin),
            onOk = { onClearInteraction() }
        )
    }
}

/**
 * 内容主体
 */
@Composable
fun SysFunInfoBody(
    navController: NavController,
//    bean: ConfigInfoBean,
    bean: ConfigInfoV2Bean,
    mode: String,
    onBeanUpdate: (ConfigInfoBean) -> Unit,
) {
    val readOnly = mode == "view"
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 15.dp, end = 15.dp, top = 15.dp)
    ) {
        val labelWidth = 80.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(horizontal = 15.dp)
        ) {
            Spacer(modifier = Modifier.height(15.dp))
            Text(
                text = stringResource(id = R.string.sys_fun_xtpz_info_f_basic),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppFieldWrapper(
                labelWidth = labelWidth,
                text = stringResource(id = R.string.sys_fun_xtpz_info_f_code)
            )
            {
                AppTextField(
                    value = bean.code,
                    readOnly = readOnly,
//                    onValueChange = {
//                        onBeanUpdate(
//                            bean.copy(code = AppFormUtils.regulateLength(it, 16))
//                        )
//                    }
                )
            }
            AppDivider()
            AppFieldWrapper(
                labelWidth = labelWidth,
                text = stringResource(id = R.string.sys_fun_xtpz_info_f_type)
            ) {
                AppTextField(
                    value = bean.type,
                    readOnly = readOnly,
//                    onValueChange = {
//                        onBeanUpdate(
//                            bean.copy(type = AppFormUtils.regulateLength(it, 16))
//                        )
//                    }
                )
            }
            AppDivider()
            AppFieldWrapper(
                labelWidth = labelWidth,
                text = stringResource(id = R.string.sys_fun_xtpz_info_f_name)
            ) {
                AppTextField(
                    value = bean.name,
                    readOnly = readOnly,
//                    onValueChange = {
//                        onBeanUpdate(
//                            bean.copy(name = AppFormUtils.regulateLength(it, 16))
//                        )
//                    }
                )
            }
            AppDivider()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(id = R.string.sys_fun_xtpz_info_f_version),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppFieldWrapper(
                labelWidth = labelWidth,
                text = stringResource(id = R.string.sys_fun_xtpz_info_f_software)
            ) {
                AppTextField(
                    value = bean.software,
                    readOnly = readOnly,
                )
            }
            AppDivider()
            AppFieldWrapper(
                labelWidth = labelWidth,
                text = stringResource(id = R.string.sys_fun_xtpz_info_f_hardware)
            ) {
                AppTextField(
                    value = bean.hardware,
                    readOnly = readOnly,
                )
            }
            AppDivider()
            Spacer(modifier = Modifier.height(12.dp))
            AppFilledButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { navController.navigate(RouteConfig.AFTER_SALE_VERSION_UPGRADE) },
                text = stringResource(id = R.string.after_sale_version_upgrade)
            )
            AppDivider()
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun SysFunInfoBottomBar(
    mode: String,
    onModifyPre: () -> Unit,
    onBack: () -> Unit,
    onModify: () -> Unit,
) {
    AppBottomBar {
        if (mode == "view") {
            AppFilledButton(
                modifier = Modifier
                    .width(120.dp)
                    .height(40.dp),
                onClick = { onModifyPre() },
                text = stringResource(id = R.string.btn_label_modify)
            )
        } else {
            AppOutlinedButton(
                modifier = Modifier
                    .width(120.dp)
                    .height(40.dp),
                onClick = {
                    onBack()
                }, text = stringResource(id = R.string.back)
            )
            Spacer(modifier = Modifier.width(10.dp))
            AppFilledButton(
                modifier = Modifier
                    .width(120.dp)
                    .height(40.dp),
                onClick = { onModify() },
                text = stringResource(id = R.string.btn_label_save)
            )
        }
    }
}

@Preview
@Composable
fun SysFunInfoPreview() {
    val viewModel: SysFunInfoViewModel = viewModel()
    LaunchedEffect(null) {
        viewModel.bean.value = AppSampleUtils.genSysInfo()
        viewModel.viewState.value = ViewState.LoadSuccess()
    }
    SysFunInfo(rememberNavController())
}