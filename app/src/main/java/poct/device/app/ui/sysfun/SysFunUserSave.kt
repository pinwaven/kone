package poct.device.app.ui.sysfun

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
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.component.AppAlert
import poct.device.app.component.AppBottomBar
import poct.device.app.component.AppConfirm
import poct.device.app.component.AppDivider
import poct.device.app.component.AppFieldWrapper
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppOutlinedButton
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppSelect
import poct.device.app.component.AppTextField
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewWrapper
import poct.device.app.entity.User
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.utils.app.AppDictUtils
import poct.device.app.utils.app.AppFormUtils
import poct.device.app.utils.app.AppSampleUtils


/**
 * 页面定义
 */
@Composable
fun SysFunUserSave(navController: NavController, viewModel: SysFunUserSaveViewModel = viewModel()) {
    val viewState by viewModel.viewState.collectAsState()
    val actionState = viewModel.actionState.collectAsState()
    val record = viewModel.record.collectAsState()
    val title =
        if (AppParams.varUserMode == "add") stringResource(id = R.string.sys_fun_xtpz_user_add)
        else stringResource(id = R.string.sys_fun_xtpz_user_edit)
    LaunchedEffect(viewState) {
        if (viewState == ViewState.Default) {
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
                    title = title,
                    backEnabled = true,
                    onBack = {
                        viewModel.onBackConfirm()
                    }
                )
            },
            bottomBar = {
                SysFunUserSaveBottomBar(
                    onSaveUser = {
                        viewModel.onSave(record.value)
                    },
                    onBack = {
                        viewModel.onBackConfirm()
                    }
                )
            }
        ) {
            SysFunUserSaveBody(
                record = record,
                onRecordUpdate = { viewModel.record.value = it })
        }
    }
    SysFunUserSaveInteraction(
        actionState = actionState,
        onUserSaveDone = {viewModel.onSaveDone(){
            if (navController.previousBackStackEntry != null) {
                navController.previousBackStackEntry!!.savedStateHandle["updateFlag"] =
                    "true"
            }
            navController.popBackStack()
        }},
        onClearInteraction = { viewModel.onClearInteraction() },
    ){
        viewModel.onClearInteraction()
        if (navController.previousBackStackEntry != null) {
            navController.previousBackStackEntry!!.savedStateHandle["updateFlag"] =
                "false"
        }
        navController.popBackStack()
    }
}

@Composable
fun SysFunUserSaveInteraction(
    actionState: State<ActionState>,
    onClearInteraction: () -> Unit,
    onUserSaveDone: () -> Unit,
    onBackPage: () -> Unit,
) {
    if (actionState.value.event == SysFunUserSaveViewModel.EVT_SAVE_FAILED) {
        val msg = actionState.value.msg ?: "Pls set msg for event"
        AppAlert(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = msg,
            onOk = { onClearInteraction() }
        )
    } else if (actionState.value.event == SysFunUserSaveViewModel.EVT_SAVE_DONE) {
        val msg = actionState.value.msg ?: "Pls set msg for event"
        val payload = actionState.value.payload
        AppAlert(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = msg.format(payload),
            onOk = { onUserSaveDone() }
        )
    } else if (actionState.value.event == SysFunUserSaveViewModel.EVT_EXIT) {
        AppConfirm(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = stringResource(id = R.string.sys_fun_xtpz_user_exit),
            onCancel = { onClearInteraction() },
            onConfirm = { onBackPage() }
        )
    }
}

/**
 * 内容主体
 */
@Composable
fun SysFunUserSaveBody(
    record: State<User>,
    onRecordUpdate: (newRecord: User) -> Unit,
) {
    val roleOptions = AppDictUtils.roleOptions(LocalContext.current)
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 15.dp, end = 15.dp, top = 15.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp),
        ) {
            AppFieldWrapper(
                text = stringResource(id = R.string.sys_fun_xtpz_user_f_role)
            ) {
                AppSelect(
                    value = record.value.role,
                    options = roleOptions,
                    actionPainter = painterResource(id = R.mipmap.zk_icon),
                    onValueChange = {
                        onRecordUpdate(
                            record.value.copy(role = it)
                        )
                    }
                )
            }
            AppDivider()
            AppFieldWrapper(
                text = stringResource(id = R.string.sys_fun_xtpz_user_f_username)
            ) {
                AppTextField(
                    value = record.value.username,
                    readOnly = AppParams.varUserMode != "add",
                    onValueChange = {
                        onRecordUpdate(record.value.copy(username = AppFormUtils.regulateLength(it, 16)))
                    }
                )
            }
            AppDivider()
            AppFieldWrapper(
                text = stringResource(id = R.string.sys_fun_xtpz_user_f_pwd)
            ) {
                AppTextField(
                    value = record.value.pwd,
                    onValueChange = {
                        onRecordUpdate(
                            record.value.copy(
                                pwd = AppFormUtils.regulateLength(
                                    it,
                                    16
                                )
                            )
                        )
                    })
            }
            AppDivider()
            AppFieldWrapper(
                text = stringResource(id = R.string.sys_fun_xtpz_user_f_nickname)
            ) {
                AppTextField(
                    value = record.value.nickname,
                    onValueChange = {
                        onRecordUpdate(
                            record.value.copy(
                                nickname = AppFormUtils.regulateLength(
                                    it,
                                    16
                                )
                            )
                        )
                    })
            }
            AppDivider()
        }
    }
}

@Composable
fun SysFunUserSaveBottomBar(onSaveUser: () -> Unit, onBack: () -> Unit) {
    AppBottomBar {
        AppOutlinedButton(
            modifier = Modifier
                .width(120.dp)
                .height(40.dp),
            onClick = { onBack() },
            text = stringResource(id = R.string.back)
        )
        Spacer(modifier = Modifier.width(10.dp))
        AppFilledButton(
            modifier = Modifier
                .width(120.dp)
                .height(40.dp),
            onClick = { onSaveUser() },
            text = stringResource(id = R.string.btn_label_save)
        )
    }
}


@Preview
@Composable
fun SysFunUserSavePreview() {
    val viewModel: SysFunUserSaveViewModel = viewModel()
    LaunchedEffect(null) {
        viewModel.record.value = AppSampleUtils.genUserInfo()
        viewModel.viewState.value = ViewState.LoadSuccess()
    }
    val navController = rememberNavController()
    AppPreviewWrapper {
        SysFunUserSave(navController, viewModel)
    }
}