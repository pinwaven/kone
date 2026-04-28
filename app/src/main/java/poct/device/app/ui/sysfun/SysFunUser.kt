package poct.device.app.ui.sysfun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.RouteConfig
import poct.device.app.component.AppAlert
import poct.device.app.component.AppBottomBar
import poct.device.app.component.AppConfirmDanger
import poct.device.app.component.AppDivider
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppList
import poct.device.app.component.AppOutlinedButton
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppValueWrapper
import poct.device.app.component.AppViewLoading
import poct.device.app.component.AppViewWrapper
import poct.device.app.entity.User
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.theme.bgColor
import poct.device.app.theme.dangerColor
import poct.device.app.utils.app.AppDictUtils
import poct.device.app.utils.app.AppSampleUtils
import poct.device.app.utils.app.AppUserUtils


/**
 * 页面定义
 */
@Composable
fun SysFunUser(navController: NavController, viewModel: SysFunUserViewModel = viewModel()) {
    val viewState by viewModel.viewState.collectAsState()
    val actionState = viewModel.actionState.collectAsState()
    val records = viewModel.records.collectAsState()
    LaunchedEffect(viewState) {
        if (viewState == ViewState.Default) {
            viewModel.onLoad()
        }
    }
    // 处理下级页面返回到当前页时的逻辑
    if (navController.currentBackStackEntry != null) {
        val updateFlag by navController.currentBackStackEntry!!.savedStateHandle
            .getStateFlow("updateFlag", "false").collectAsState()
        LaunchedEffect(updateFlag) {
            if (updateFlag == "true") {
                viewModel.onLoad()
            }
        }
    }

    AppViewWrapper(
        viewState = viewState,
        onErrorClick = { navController.popBackStack() }) {

        AppScaffold(
            topBar = {
                AppTopBar(
                    navController = navController,
                    title = stringResource(id = R.string.sys_fun_xtpz_user),
                    backEnabled = true
                )
            },
            bottomBar = {
                SysFunUserBottomBar(
                    onAddUser = {
                        viewModel.onAdd {
                            navController.navigate(RouteConfig.SYS_FUN_USER_SAVE)
                        }
                    },
                )
            }
        ) {
            SysFunUserBody(
                records = records,
                onUserEdit = {
                    viewModel.onEdit(it) {
                        navController.navigate(RouteConfig.SYS_FUN_USER_SAVE)
                    }
                },
                onUserDelete = { viewModel.onDeleteConfirm(it) },
            )
        }
    }
    SysFunUserInteraction(
        actionState = actionState,
        onUserDelete = { viewModel.onDelete(it) },
        onUserDeleteDone = { viewModel.onDeleteDone() },
        onClearInteraction = { viewModel.onClearInteraction() }
    )
}

@Composable
fun SysFunUserInteraction(
    actionState: State<ActionState>,
    onUserDelete: (user: User) -> Unit,
    onUserDeleteDone: () -> Unit,
    onClearInteraction: () -> Unit,
) {
    val userInfo = AppParams.varUser
    if (actionState.value.event == SysFunUserViewModel.EVT_DEL_CONFIRM) {
        val msg = stringResource(id = R.string.sys_fun_xtpz_user_del_confirm)
        AppConfirmDanger(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = msg.format(userInfo.nickname),
            confirmText = stringResource(id = R.string.btn_label_delete),
            onCancel = { onClearInteraction() },
            onConfirm = {
                onUserDelete(userInfo)
            }
        )
    } else if (actionState.value.event == SysFunUserViewModel.EVT_DEL_DONE) {
        val msg = stringResource(id = R.string.sys_fun_xtpz_user_del_done)
        AppAlert(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = msg.format(userInfo.nickname),
            onOk = { onUserDeleteDone() }
        )
    } else if (actionState.value.event == SysFunUserViewModel.EVT_LOADING) {
        AppViewLoading()
    }
}

/**
 * 内容主体
 */
@Composable
fun SysFunUserBody(
    records: State<List<User>>,
    onUserEdit: (user: User) -> Unit,
    onUserDelete: (user: User) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 15.dp, end = 15.dp, top = 15.dp)
    ) {
        AppList(
            modifier = Modifier.fillMaxWidth(),
            records = records
        ) { it ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(143.dp)
                        .background(bgColor)
                        .padding(10.dp)
                ) {
                    Text(text = it.nickname, fontSize = 12.sp, fontWeight = FontWeight.Normal)
                    Spacer(modifier = Modifier.height(10.dp))
                    AppDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.weight(1F)
                        ) {
                            AppValueWrapper(
                                text = stringResource(id = R.string.sys_fun_xtpz_user_f_username),
                                value = it.username
                            )
                        }
                        Row(
                            modifier = Modifier.weight(1F)
                        ) {
                            AppValueWrapper(
                                text = stringResource(id = R.string.sys_fun_xtpz_user_f_pwd),
                                value = it.pwd
                            )
                        }
                    }
                    AppValueWrapper(
                        text = stringResource(id = R.string.sys_fun_xtpz_user_f_role),
                        value = AppDictUtils.label(AppDictUtils.roleOptions(LocalContext.current), it.role)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        if (!AppUserUtils.isDefault(it.username)) {
                            AppOutlinedButton(
                                modifier = Modifier
                                    .width(66.dp)
                                    .height(30.dp),
                                onClick = { onUserDelete(it) },
                                text = stringResource(id = R.string.btn_label_delete),
                                color = dangerColor,
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        AppFilledButton(
                            modifier = Modifier
                                .width(66.dp)
                                .height(30.dp),
                            onClick = { onUserEdit(it) },
                            text = stringResource(id = R.string.btn_label_edit)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SysFunUserBottomBar(onAddUser: () -> Unit) {
    AppBottomBar {
        AppFilledButton(
            modifier = Modifier
                .width(120.dp)
                .height(40.dp),
            onClick = { onAddUser() },
            text = stringResource(id = R.string.btn_label_add_user)
        )
    }
}


@Preview
@Composable
fun SysFunUserPreview() {
    val viewModel: SysFunUserViewModel = viewModel()
    LaunchedEffect(null) {
        viewModel.records.value = AppSampleUtils.genUserInfos()
//        viewModel.viewState.value = ViewState.Event(SysFunUserViewModel.EVT_DEL_DONE)
        viewModel.viewState.value = ViewState.LoadSuccess()
    }
    AppPreviewWrapper {
        SysFunUser(rememberNavController())
    }
}