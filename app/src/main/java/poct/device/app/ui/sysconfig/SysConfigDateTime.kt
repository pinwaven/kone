package poct.device.app.ui.sysconfig

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.R
import poct.device.app.bean.ConfigDateTimeBean
import poct.device.app.component.AppBottomBar
import poct.device.app.component.AppConfirm
import poct.device.app.component.AppDateTimePicker
import poct.device.app.component.AppDivider
import poct.device.app.component.AppFieldWrapper
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppOutlinedButton
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppRadioGroup
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppSelect
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewLoading
import poct.device.app.component.AppViewWrapper
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.theme.bgColor
import poct.device.app.utils.app.AppDictUtils


/**
 * 页面定义
 */
@Composable
fun SysConfigDateTime(
    navController: NavController,
    viewModel: SysConfigDateTimeViewModel = viewModel(),
) {
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
        if (mode == "view") stringResource(id = R.string.sys_config_other_datetime)
        else stringResource(id = R.string.sys_config_other_datetime_modify)
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
                SysConfigDateTimeBottomBar(
                    mode = mode,
                    onModifyPre = { viewModel.onModifyPre() },
                    onBack = {
                        viewModel.onBackConfirm()
                    },
                    onModify = { viewModel.onSaveConfirm() }
                )
            }
        ) {
            SysConfigDateTimeForm(
                bean = bean,
                mode = mode,
                onBeanUpdate = { viewModel.onBeanUpdate(it) },
            )
        }
    }

    SysConfigDateTimeInteraction(
        actionState = actionState,
        onSave = { viewModel.onSave() },
        onClearInteraction = { viewModel.onClearInteraction() }
    ){
        viewModel.onClearInteraction()
        viewModel.onBack()
        viewModel.onLoad()
    }

}

@Composable
fun SysConfigDateTimeInteraction(
    actionState: ActionState,
    onSave: () -> Unit,
    onClearInteraction: () -> Unit,
    onBackPage: () -> Unit,
) {
    if (actionState.event == SysConfigDateTimeViewModel.EVT_EXIT) {
        AppConfirm(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = stringResource(id = R.string.sys_config_other_datetime_exit),
            onCancel = { onClearInteraction() },
            onConfirm = { onBackPage() }
        )
    }else if (actionState.event == SysConfigDateTimeViewModel.EVT_LOADING) {
        AppViewLoading(actionState.msg)
    } else if (actionState.event == SysConfigDateTimeViewModel.EVT_SAVE_CONFIRM) {
        AppConfirm(
            visible = true,
            content = stringResource(id = R.string.msg_time_save_confirm),
            onCancel = onClearInteraction,
            onConfirm = onSave
        )
    }
}

@Composable
private fun SysConfigDateTimeForm(
    bean: ConfigDateTimeBean,
    mode: String,
    onBeanUpdate: (ConfigDateTimeBean) -> Unit,
) {
    val readOnly = mode == "view"
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(15.dp),
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp)
        ) {
            val labelWidth = 108.dp
            AppFieldWrapper(
                text = stringResource(id = R.string.sys_config_other_datetime_f_network),
                labelWidth = labelWidth
            ) {
                val options = mapOf(
                    true to stringResource(id = R.string.sys_config_other_datetime_network_open),
                    false to stringResource(id = R.string.sys_config_other_datetime_network_close)
                )
                AppRadioGroup(
                    value = bean.timeSync == "y",
                    options = options,
                    readOnly = readOnly,
                    gap = 0.dp,
                    onValueChange = { onBeanUpdate(bean.copy(timeSync = (if (it) "y" else "n"))) }
                )
            }
            AppDivider(0)
            AppFieldWrapper(
                text = stringResource(id = R.string.sys_config_other_datetime_f_region),
                labelWidth = labelWidth
            ) {
                val options = AppDictUtils.timeZoneOptions()
                AppSelect(
                    value = bean.timeZone,
                    options = options,
                    readOnly = readOnly,
                    actionPainter = painterResource(id = R.mipmap.zk_icon),
                    onValueChange = { onBeanUpdate(bean.copy(timeZone = it)) })
            }
            AppDivider(0)
            AppFieldWrapper(
                text = stringResource(id = R.string.sys_config_other_datetime_f_datetime),
                labelWidth = labelWidth
            ) {
                AppDateTimePicker(
                    value = bean.time,
                    readOnly = readOnly,
                    actionPainter = painterResource(id = R.mipmap.zk_icon),
                    onValueChange = {onBeanUpdate(bean.copy(time = it))})
            }
            AppDivider(0)
        }
    }
}

@Composable
fun SysConfigDateTimeBottomBar(
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
fun SysConfigDateTimePreview() {
    val navController = rememberNavController()
    val viewModel: SysConfigDateTimeViewModel = viewModel()
    LaunchedEffect(key1 = Unit) {
        viewModel.viewState.value = ViewState.LoadSuccess()
//        viewModel.actionState.value = ActionState(SysConfigDateTimeViewModel.EVT_SAVE_CONFIRM)
    }

    AppPreviewWrapper {
        SysConfigDateTime(navController, viewModel)
    }
}