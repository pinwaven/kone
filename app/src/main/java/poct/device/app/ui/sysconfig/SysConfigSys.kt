package poct.device.app.ui.sysconfig

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.R
import poct.device.app.bean.ConfigSysBean
import poct.device.app.component.AppAlert
import poct.device.app.component.AppBottomBar
import poct.device.app.component.AppConfirm
import poct.device.app.component.AppFieldWrapper
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppOutlinedButton
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppRadioGroup
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTextField
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewLoading
import poct.device.app.component.AppViewWrapper
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.theme.bgColor
import poct.device.app.theme.sepColor
import poct.device.app.utils.app.AppDictUtils
import poct.device.app.utils.app.AppFormUtils


/**
 * 页面定义
 */
@Composable
fun SysConfigSys(navController: NavController, viewModel: SysConfigSysViewModel = viewModel()) {
    val viewState by viewModel.viewState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val bean by viewModel.bean.collectAsState()
    val mode by viewModel.mode.collectAsState()
    LaunchedEffect(viewState) {
        if (viewState == ViewState.Default) {
            viewModel.onLoad()
        }
    }
    val title =
        if (mode == "view") stringResource(id = R.string.sys_config_sys)
        else stringResource(id = R.string.sys_config_sys_modify)
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
                    }
                )
            },
            bottomBar = {
                SysConfigSysBottomBar(
                    mode = mode,
                    onModifyPre = { viewModel.onModifyPre() },
                    onBack = {
                        viewModel.onBackConfirm()
                    },
                    onModify = { viewModel.onSave() },
                )
            }
        ) {
            SysConfigSysForm(
                bean = bean,
                mode = mode,
                onBeanUpdate = { viewModel.onBeanUpdate(it) }
            )
        }
    }

    SysConfigSysInteraction(
        actionState = actionState,
        onClearInteraction = { viewModel.onClearInteraction() }
    ) {
        viewModel.onClearInteraction()
        viewModel.onBack()
        viewModel.onLoad()
    }
}

@Composable
fun SysConfigSysInteraction(
    actionState: ActionState,
    onClearInteraction: () -> Unit,
    onBackPage: () -> Unit,
) {
    if (actionState.event == SysConfigSysViewModel.EVT_EXIT) {
        AppConfirm(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = stringResource(id = R.string.sys_config_sys_exit),
            onCancel = { onClearInteraction() },
            onConfirm = { onBackPage() }
        )
    } else if (actionState.event == SysConfigSysViewModel.EVT_LOADING) {
        AppViewLoading(actionState.msg)
    } else if (actionState.event == SysConfigSysViewModel.EVT_SAVE_DONE) {
        AppAlert(
            visible = true,
            content = stringResource(id = R.string.save_ok),
            onOk = { onClearInteraction() }
        )
    }
}


@Composable
private fun SysConfigSysForm(
    bean: ConfigSysBean,
    mode: String,
    onBeanUpdate: (ConfigSysBean) -> Unit,
) {
    val readOnly = mode == "view"
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 15.dp, top = 15.dp, end = 15.dp),
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp)
        ) {
            val ynOptions = AppDictUtils.yesNoOptions(LocalContext.current)
            AppFieldWrapper(
                text = stringResource(id = R.string.sys_config_sys_f_sys)
            ) {
                AppTextField(
                    value = bean.sys,
                    readOnly = readOnly,
                    onValueChange = {
                        onBeanUpdate(bean.copy(sys = AppFormUtils.regulateLength(it, 16)))
                    }
                )
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(sepColor)
            )
            AppFieldWrapper(
                text = stringResource(id = R.string.sys_config_sys_f_scan),
            ) {
                AppRadioGroup(
                    value = bean.scan,
                    readOnly = readOnly,
                    options = ynOptions,
                    gap = 0.dp,
                    onValueChange = { onBeanUpdate(bean.copy(scan = it)) }
                )
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(sepColor)
            )
            AppFieldWrapper(
                text = stringResource(id = R.string.sys_config_sys_f_ige)
            ) {
                AppTextField(
                    value = bean.ige,
                    readOnly = readOnly,
                    onValueChange = {
                        onBeanUpdate(bean.copy(ige = AppFormUtils.regulateLength(it, 16)))
                    }
                )
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(sepColor)
            )
            AppFieldWrapper(
                text = stringResource(id = R.string.sys_config_sys_f_crp)
            ) {
                AppTextField(
                    value = bean.crp,
                    readOnly = readOnly,
                    onValueChange = {
                        onBeanUpdate(bean.copy(crp = AppFormUtils.regulateLength(it, 16)))
                    }
                )
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(sepColor)
            )
            AppFieldWrapper(
                text = stringResource(id = R.string.sys_config_sys_f_sf)
            ) {
                AppTextField(
                    value = bean.sf,
                    readOnly = readOnly,
                    onValueChange = {
                        onBeanUpdate(bean.copy(sf = AppFormUtils.regulateLength(it, 16)))
                    }
                )
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(sepColor)
            )
            AppFieldWrapper(
                text = stringResource(id = R.string.sys_config_sys_f_4lj)
            ) {
                AppTextField(
                    value = bean.slj,
                    readOnly = readOnly,
                    onValueChange = {
                        onBeanUpdate(bean.copy(slj = AppFormUtils.regulateLength(it, 16)))
                    }
                )
            }
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(sepColor)
            )
        }
    }
}


@Composable
fun SysConfigSysBottomBar(
    mode: String,
    onModifyPre: () -> Unit = {},
    onBack: () -> Unit = {},
    onModify: () -> Unit = {},
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
fun SysConfigSysPreview(viewModel: SysConfigSysViewModel = viewModel()) {
    LaunchedEffect(key1 = Unit) {
        viewModel.actionState.value = ActionState.Default
        viewModel.viewState.value = ViewState.LoadSuccess()
    }
    AppPreviewWrapper {
        SysConfigSys(rememberNavController(), viewModel)
    }
}