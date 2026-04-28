package poct.device.app.ui.sysfun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import poct.device.app.bean.ConfigAdjustBean
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
import poct.device.app.utils.app.AppFormUtils

/**
 * 页面定义
 */
@Composable
fun SysFunAdjust(navController: NavController, viewModel: SysFunAdjustViewModel = viewModel()) {
    val viewState = viewModel.viewState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val bean by viewModel.bean.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val checkState by viewModel.checkState.collectAsState()



    LaunchedEffect(viewState.value) {
        if (viewState.value == ViewState.Default) {
            viewModel.onLoad()
        }
    }
    LaunchedEffect(actionState) {

    }
    val title =
        if (mode == "view") stringResource(id = R.string.sys_fun_sbjz)
        else stringResource(id = R.string.sys_fun_sbjz)
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
                SysFunAdjustBottomBar(
                    mode = mode,
                    checkState = checkState,
                    onModifyPre = { viewModel.onModifyPre() },
                    onStartCheck = { viewModel.onStartCheck() },
                    onStopCheck = { viewModel.onStopCheck() },
                    onBack = {
                        viewModel.onBackConfirm()
                    },
                    onModify = { viewModel.onSave() }
                )
            }
        ) {
            SysFunAdjustBody(
                bean = bean,
                mode = mode,
                onBeanUpdate = { viewModel.onBeanUpdate(it) }
            )
        }
    }

    SysFunAdjustInteraction(
        actionState = actionState,
        onClearInteraction = { viewModel.onClearInteraction() }
    ) {
        viewModel.onClearInteraction()
        viewModel.onBack()
        viewModel.onLoad()
    }
}


@Composable
fun SysFunAdjustInteraction(
    actionState: ActionState,
    onClearInteraction: () -> Unit,
    onBackPage: () -> Unit,
) {
    if (actionState.event == SysFunAdjustViewModel.EVT_EXIT) {
        AppConfirm(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = stringResource(id = R.string.sys_fun_sbjz_exit),
            onCancel = { onClearInteraction() },
            onConfirm = { onBackPage() }
        )
    } else if (actionState.event == SysFunAdjustViewModel.EVT_LOADING) {
        AppViewLoading(actionState.msg)
    } else if (actionState.event == SysFunAdjustViewModel.EVT_SAVE_DONE) {
        AppAlert(
            visible = true,
            content = stringResource(id = R.string.save_ok),
            onOk = { onClearInteraction() }
        )
    } else if (actionState.event == SysFunAdjustViewModel.EVT_ERROR) {
        actionState.msg?.let {
            AppAlert(
                visible = true,
                content = it,
                onOk = { onClearInteraction() }
            )
        }
    }
}

/**
 * 内容主体
 */
@Composable
fun SysFunAdjustBody(
    bean: ConfigAdjustBean,
    mode: String,
    onBeanUpdate: (ConfigAdjustBean) -> Unit,
) {
    val readOnly = mode == "view"
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 15.dp, end = 15.dp, top = 15.dp)
    ) {
        val labelWidth = 160.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(horizontal = 15.dp)
        ) {
            Spacer(modifier = Modifier.height(15.dp))
            Text(
                text = stringResource(id = R.string.sys_fun_sbjz_jgjz),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppFieldWrapper(
                labelWidth = labelWidth,
                text = stringResource(id = R.string.sys_fun_sbjz_jgjz_1)
            )
            {
                AppTextField(
                    value = bean.jgName,
                    readOnly = readOnly,
                    onValueChange = {
                        onBeanUpdate(
                            bean.copy(jgName = AppFormUtils.regulateLength(it, 16))
                        )
                    }
                )
            }
            AppDivider()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(id = R.string.sys_fun_sbjz_wzjz),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppFieldWrapper(
                labelWidth = labelWidth,
                text = stringResource(id = R.string.sys_fun_sbjz_wzjz_1)
            ) {
                AppTextField(
                    value = bean.posName1,
                    readOnly = readOnly,
                    onValueChange = {
                        onBeanUpdate(
                            bean.copy(posName1 = AppFormUtils.regulateLength(it, 16))
                        )
                    }
                )
            }
            AppDivider()
            AppFieldWrapper(
                labelWidth = labelWidth,
                text = stringResource(id = R.string.sys_fun_sbjz_wzjz_2)
            ) {
                AppTextField(
                    value = bean.posName2,
                    readOnly = readOnly,
                    onValueChange = {
                        onBeanUpdate(
                            bean.copy(posName2 = AppFormUtils.regulateLength(it, 16))
                        )
                    }
                )
            }
            AppDivider()
            AppFieldWrapper(
                labelWidth = labelWidth,
                text = stringResource(id = R.string.sys_fun_sbjz_wzjz_3)
            ) {
                AppTextField(
                    value = bean.posName3,
                    readOnly = readOnly,
                    onValueChange = {
                        onBeanUpdate(
                            bean.copy(posName3 = AppFormUtils.regulateLength(it, 16))
                        )
                    }
                )
            }
            AppDivider()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(id = R.string.sys_fun_sbjz_jdjz),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppFieldWrapper(
                labelWidth = labelWidth,
                text = stringResource(id = R.string.sys_fun_sbjz_jdjz_1)
            )
            {
                AppTextField(
                    value = bean.jcName1,
                    readOnly = readOnly,
                    onValueChange = {
                        onBeanUpdate(
                            bean.copy(jcName1 = AppFormUtils.regulateLength(it, 16))
                        )
                    }
                )
            }
            AppDivider()
        }
    }
}

@Composable
fun SysFunAdjustBottomBar(
    mode: String,
    checkState: String,
    onModifyPre: () -> Unit,
    onStartCheck: () -> Unit,
    onStopCheck: () -> Unit,
    onBack: () -> Unit,
    onModify: () -> Unit,
) {
    AppBottomBar {
        if (mode == "view") {
            if (checkState == "start") {
                AppFilledButton(
                    modifier = Modifier
                        .width(120.dp)
                        .height(40.dp),
                    onClick = { onStartCheck() },
                    text = stringResource(id = R.string.btn_label_start)
                )
            } else {
                AppFilledButton(
                    modifier = Modifier
                        .width(120.dp)
                        .height(40.dp),
                    onClick = { onStopCheck() },
                    text = stringResource(id = R.string.btn_label_stop)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
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
fun SysFunAdjustPreview() {
    val viewModel: SysFunAdjustViewModel = viewModel()
    LaunchedEffect(null) {
        viewModel.viewState.value = ViewState.LoadSuccess()
    }
    SysFunAdjust(rememberNavController())
}