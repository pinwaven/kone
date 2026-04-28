package poct.device.app.ui.workconfig

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.RouteConfig
import poct.device.app.bean.CardConfigBean
import poct.device.app.bean.CardTopBean
import poct.device.app.bean.CardVarBean
import poct.device.app.component.AppAlert
import poct.device.app.component.AppConfirm
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewLoading
import poct.device.app.component.AppViewWrapper
import poct.device.app.state.ActionState
import poct.device.app.state.FieldStateHolder
import poct.device.app.state.ViewState
import poct.device.app.theme.bgColor
import poct.device.app.utils.app.AppSampleUtils
import timber.log.Timber


/**
 * 页面定义
 */
@Composable
fun WorkConfigCardAdd(
    navController: NavController,
    viewModel: WorkConfigCardAddViewModel = viewModel(),
) {
    val viewState = viewModel.viewState.collectAsState()
    val actionState = viewModel.actionState.collectAsState()
    val stepState = viewModel.stepState.collectAsState()
    val bean = viewModel.bean.collectAsState()
    val fieldStateHolder = viewModel.fieldStateHolder.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.onLoad()
    }
    Timber.w("当前bean:${App.gson.toJson(bean)}")
    val title =
        if (AppParams.varCardConfigMode != "add")
            stringResource(id = R.string.work_config_edit_card)
        else
            stringResource(id = R.string.work_config_add_card)
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
                WorkConfigCardAddBottomBar(
                    stepValue = stepState,
                    onInfoReset = { viewModel.onInfoReset() },
                    onInfoNext = { viewModel.onInfoNext() },
                    onTopPre = { viewModel.onTopPre() },
                    onTopNext = { viewModel.onTopNext() },
                    onVarPre = { viewModel.onVarPre() },
                    onVarPreview = { viewModel.onPreview { navController.navigate(RouteConfig.WORK_CONFIG_CARD_VIEW) } },
                    onVarSave = { viewModel.onSave() },
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                WorkConfigCardAddSteps(
                    stepValue = stepState
                )

                WorkConfigCardAddBody(
                    actionValue = actionState,
                    stepValue = stepState,
                    fieldStateHolder = fieldStateHolder,
                    bean = bean,
                    onBeanUpdate = { viewModel.onBeanUpdate(it) },
                    onCheck = { viewModel.onCheck() },
                    onTopAdd = { viewModel.onTopAddConfirm() },
                    onTopRemove = { viewModel.onTopRemoveConfirm(it) },
                    onVarAdd = { viewModel.onVarAddConfirm() },
                    onVarRemove = { viewModel.onVarRemoveConfirm(it) }
                )
            }
        }
    }
    WorkConfigCardAddInteraction(
        actionState = actionState,
        onAddTop = { viewModel.onTopAdd() },
        onAddVar = { viewModel.onVarAdd() },
        onRemoveTop = { viewModel.onTopRemove(it) },
        onRemoveVar = { viewModel.onVarRemove(it) },
        onSaveOk = {
            if (navController.previousBackStackEntry != null) {
                navController.previousBackStackEntry!!.savedStateHandle["updateFlag"] =
                    "true"
            }
            navController.popBackStack()
        },
        onBackPage = {
            viewModel.onClearInteraction()
            if (navController.previousBackStackEntry != null) {
                navController.previousBackStackEntry!!.savedStateHandle["updateFlag"] =
                    "false"
            }
            navController.popBackStack()
        },
        onClearInteraction = { viewModel.onClearInteraction() },
    )
}

@Composable
private fun WorkConfigCardAddInteraction(
    actionState: State<ActionState>,
    onAddTop: () -> Unit,
    onAddVar: () -> Unit,
    onRemoveTop: (CardTopBean) -> Unit,
    onRemoveVar: (CardVarBean) -> Unit,
    onSaveOk: () -> Unit,
    onBackPage: () -> Unit,
    onClearInteraction: () -> Unit,
) {
    when (actionState.value.event) {
        WorkConfigCardAddViewModel.EVT_EXIT -> {
            AppConfirm(
                title = stringResource(id = R.string.confirm_title_remind),
                visible = true,
                content = stringResource(id = R.string.work_config_exit_confirm),
                onCancel = { onClearInteraction() },
                onConfirm = { onBackPage() }
            )
        }

        WorkConfigCardAddViewModel.EVT_LOADING -> {
            AppViewLoading()
        }

        WorkConfigCardAddViewModel.EVT_ADD_TOP_PRE -> {
            AppConfirm(
                visible = true,
                content = stringResource(id = R.string.work_config_add_top_confirm),
                onCancel = { onClearInteraction() },
                onConfirm = { onAddTop() }
            )
        }

        WorkConfigCardAddViewModel.EVT_ADD_TOP_DONE -> {
            AppAlert(
                visible = true,
                content = stringResource(id = R.string.work_config_add_top_done),
                onOk = { onClearInteraction() }
            )
        }

        WorkConfigCardAddViewModel.EVT_ADD_VAR_PRE -> {
            AppConfirm(
                visible = true,
                content = stringResource(id = R.string.work_config_add_var_confirm),
                onCancel = { onClearInteraction() },
                onConfirm = { onAddVar() }
            )
        }

        WorkConfigCardAddViewModel.EVT_ADD_VAR_DONE -> {
            AppAlert(
                visible = true,
                content = stringResource(id = R.string.work_config_add_var_done),
                onOk = { onClearInteraction() }
            )
        }

        WorkConfigCardAddViewModel.EVT_REMOVE_TOP_PRE -> {
            val top = actionState.value.payload as CardTopBean
            AppConfirm(
                visible = true,
                content = stringResource(id = R.string.work_config_remove_top_confirm),
                onCancel = { onClearInteraction() },
                onConfirm = { onRemoveTop(top) }
            )
        }

        WorkConfigCardAddViewModel.EVT_REMOVE_TOP_DONE -> {
            AppAlert(
                visible = true,
                content = stringResource(id = R.string.work_config_remove_top_done),
                onOk = { onClearInteraction() }
            )
        }

        WorkConfigCardAddViewModel.EVT_REMOVE_VAR_PRE -> {
            val varBean = actionState.value.payload as CardVarBean
            AppConfirm(
                visible = true,
                content = stringResource(id = R.string.work_config_remove_var_confirm),
                onCancel = { onClearInteraction() },
                onConfirm = { onRemoveVar(varBean) }
            )
        }

        WorkConfigCardAddViewModel.EVT_REMOVE_VAR_DONE -> {
            AppAlert(
                visible = true,
                content = stringResource(id = R.string.work_config_remove_var_done),
                onOk = { onClearInteraction() }
            )
        }

        WorkConfigCardAddViewModel.EVT_SAVE_DONE -> {
            var doneVisible by remember { mutableStateOf(true) }
            AppAlert(
                visible = doneVisible,
                content = stringResource(id = R.string.save_ok),
                onOk = {
                    doneVisible = false
                    onSaveOk()
                }
            )
        }

        WorkConfigCardAddViewModel.EVT_VALIDATE_FAILED -> {
            val msg = actionState.value.msg ?: "No data"
            AppAlert(
                visible = true,
                content = msg,
                onOk = { onClearInteraction() }
            )
        }
    }
}

/**
 * 内容主体
 */
@Composable
fun WorkConfigCardAddBody(
    actionValue: State<ActionState>,
    stepValue: State<String>,
    fieldStateHolder: State<FieldStateHolder>,
    bean: State<CardConfigBean>,
    onBeanUpdate: (CardConfigBean) -> Unit,
    onCheck: () -> Unit,
    onTopAdd: () -> Unit,
    onTopRemove: (CardTopBean) -> Unit,
    onVarAdd: () -> Unit,
    onVarRemove: (CardVarBean) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 15.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(horizontal = 15.dp)
        ) {
            if (stepValue.value == WorkConfigCardAddViewModel.STEP_INFO) {
                WorkConfigCardAddInfo(
                    bean,
                    fieldStateHolder,
                    onBeanUpdate,
                    onCheck,
                )
            } else if (stepValue.value == WorkConfigCardAddViewModel.STEP_TOP) {
                WorkConfigCardAddTop(
                    bean,
                    actionValue,
                    fieldStateHolder,
                    onTopAdd,
                    onTopRemove,
                    onBeanUpdate
                )
            } else if (stepValue.value == WorkConfigCardAddViewModel.STEP_VAR) {
                WorkConfigCardAddVar(
                    bean,
                    actionValue,
                    fieldStateHolder,
                    onVarAdd,
                    onVarRemove,
                    onBeanUpdate
                )
            }
        }
    }
}


@Preview
@Composable
fun WorkConfigCardAddPreview() {
    val viewModel: WorkConfigCardAddViewModel = viewModel()
    LaunchedEffect(null) {
        viewModel.bean.value = AppSampleUtils.genCardInfo(5)
        viewModel.viewState.value = ViewState.LoadSuccess()
        viewModel.stepState.value = WorkConfigCardAddViewModel.STEP_INFO
    }
    val navController = rememberNavController()
    AppPreviewWrapper {
        WorkConfigCardAdd(navController, viewModel)
    }
}