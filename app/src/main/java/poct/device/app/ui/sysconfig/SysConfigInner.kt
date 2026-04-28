package poct.device.app.ui.sysconfig

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.R
import poct.device.app.bean.ConfigInnerBean
import poct.device.app.component.AppAlert
import poct.device.app.component.AppBottomBar
import poct.device.app.component.AppConfirm
import poct.device.app.component.AppDivider
import poct.device.app.component.AppFieldWrapper
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppOutlinedButton
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTextField
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewLoading
import poct.device.app.component.AppViewWrapper
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.theme.bgColor
import poct.device.app.utils.app.AppFormUtils


/**
 * 页面定义
 */
@Composable
fun SysConfigInner(navController: NavController, viewModel: SysConfigInnerViewModel = viewModel()) {
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
        if (mode == "view")stringResource(id = R.string.sys_config_inner)
        else stringResource(id = R.string.sys_config_inner_modify)
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
                SysConfigInnerBottomBar(
                    mode = mode,
                    onModifyPre = { viewModel.onModifyPre() },
                    onBack = {
                        viewModel.onBackConfirm()
                    },
                    onModify = { viewModel.onSave() }
                )
            }
        ) {
            SysConfigInnerForm(
                bean = bean,
                mode = mode,
                onBeanUpdate = { viewModel.onBeanUpdate(it) }
            )
        }
    }

    SysConfigInnerInteraction(
        actionState = actionState,
        onClearInteraction = { viewModel.onClearInteraction() }
    ){
        viewModel.onClearInteraction()
        viewModel.onBack()
        viewModel.onLoad()
    }
}

@Composable
fun SysConfigInnerInteraction(
    actionState: ActionState,
    onClearInteraction: () -> Unit,
    onBackPage: () -> Unit,
) {
    if (actionState.event == SysConfigInnerViewModel.EVT_EXIT) {
        AppConfirm(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = stringResource(id = R.string.sys_config_inner_exit),
            onCancel = { onClearInteraction() },
            onConfirm = { onBackPage() }
        )
    }else if (actionState.event == SysConfigInnerViewModel.EVT_LOADING) {
        AppViewLoading(actionState.msg)
    } else if (actionState.event == SysConfigInnerViewModel.EVT_SAVE_DONE) {
        AppAlert(
            visible = true,
            content = stringResource(id = R.string.save_ok),
            onOk = { onClearInteraction() }
        )
    }
}

@Composable
private fun SysConfigInnerForm(
    bean: ConfigInnerBean,
    mode: String,
    onBeanUpdate: (ConfigInnerBean) -> Unit,
) {
    val readOnly = mode == "view"
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 15.dp, end = 15.dp, top = 15.dp),
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp)
        ) {
            val labelWidth = 136.dp
            AppFieldWrapper(
                text = stringResource(id = R.string.sys_config_inner_f_kdbs),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = bean.kdbs,
                    readOnly = readOnly,
                    onValueChange = {
                        onBeanUpdate(
                            bean.copy(kdbs = AppFormUtils.regulateLength(it, 16))
                        )
                    }
                )
            }
            AppDivider(0)
            AppFieldWrapper(
                text = stringResource(id = R.string.sys_config_inner_f_ydbs),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = bean.ydbs,
                    readOnly = readOnly,
                    onValueChange = {
                        onBeanUpdate(
                            bean.copy(ydbs = AppFormUtils.regulateLength(it, 16))
                        )
                    }
                )
            }
            AppDivider(0)
            AppFieldWrapper(
                text = stringResource(id = R.string.sys_config_inner_f_rdss),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = bean.rdss,
                    readOnly = readOnly,
                    onValueChange = {
                        onBeanUpdate(
                            bean.copy(rdss = AppFormUtils.regulateLength(it, 16))
                        )
                    }
                )
            }
            AppDivider(0)
            AppFieldWrapper(
                text = stringResource(id = R.string.sys_config_inner_f_rbc),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = bean.rbc,
                    readOnly = readOnly,
                    onValueChange = {
                        onBeanUpdate(
                            bean.copy(rbc = AppFormUtils.regulateLength(it, 16))
                        )
                    }
                )
            }
            AppDivider(0)
            AppFieldWrapper(
                text = stringResource(id = R.string.sys_config_inner_f_shbs),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = bean.shbs,
                    readOnly = readOnly,
                    onValueChange = {
                        onBeanUpdate(
                            bean.copy(shbs = AppFormUtils.regulateLength(it, 16))
                        )
                    }
                )
            }
            AppDivider(0)
            AppFieldWrapper(
                text = stringResource(id = R.string.sys_config_inner_f_ft),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = bean.ft,
                    readOnly = readOnly,
                    onValueChange = {
                        onBeanUpdate(
                            bean.copy(ft = AppFormUtils.regulateLength(it, 16))
                        )
                    }
                )
            }
            AppDivider(0)
            AppFieldWrapper(
                text = stringResource(id = R.string.sys_config_inner_f_xt),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = bean.xt,
                    readOnly = readOnly,
                    onValueChange = {
                        onBeanUpdate(
                            bean.copy(xt = AppFormUtils.regulateLength(it, 16))
                        )
                    }
                )
            }
            AppDivider(0)
            AppFieldWrapper(
                text = stringResource(id = R.string.sys_config_inner_f_zxfw),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = bean.zxfw,
                    readOnly = readOnly,
                    onValueChange = {
                        onBeanUpdate(
                            bean.copy(zxfw = AppFormUtils.regulateLength(it, 16))
                        )
                    }
                )
            }
            AppDivider(0)
            AppFieldWrapper(
                text = stringResource(id = R.string.sys_config_inner_f_tkbs),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = bean.tkbs,
                    readOnly = readOnly,
                    onValueChange = {
                        onBeanUpdate(
                            bean.copy(tkbs = AppFormUtils.regulateLength(it, 16))
                        )
                    }
                )
            }
            AppDivider(0)
        }
    }
}


@Composable
fun SysConfigInnerBottomBar(
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
fun SysConfigInnerPreview() {
    val viewModel: SysConfigInnerViewModel = viewModel()
    LaunchedEffect(key1 = Unit) {
        viewModel.viewState.value = ViewState.LoadSuccess()
    }
    AppPreviewWrapper {
        Box(modifier = Modifier.fillMaxSize()) {
            SysConfigInner(rememberNavController(), viewModel)
        }
    }
}