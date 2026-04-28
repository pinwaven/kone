package poct.device.app.ui.work

import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import poct.device.app.R
import poct.device.app.RouteConfig
import poct.device.app.bean.CaseBean
import poct.device.app.bean.ConfigSysBean
import poct.device.app.bean.card.CardConfig
import poct.device.app.component.AppAlert
import poct.device.app.component.AppConfirm
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppQRCodeAlert
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewLoading
import poct.device.app.component.AppViewWrapper
import poct.device.app.entity.service.SysConfigService
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.utils.app.AppSampleUtils
import poct.device.app.utils.app.AppSystemUtils
import timber.log.Timber

/**
 * 页面定义
 */
@Composable
fun WorkMain(navController: NavController, viewModel: WorkMainViewModel = viewModel()) {
    val viewState = viewModel.viewState.collectAsState()
    val actionState = viewModel.actionState.collectAsState()
    // 操作页面
    val actionValue = viewModel.action.collectAsState()
    // 顶部步骤
    val stepValue = viewModel.step.collectAsState()
    // 纪录
    val bean = viewModel.bean.collectAsState()
    // 检测进度
    val progress = viewModel.progress.collectAsState()
    val showFy0Time = viewModel.showFy0Time.collectAsState()
    val isFy0Finished = viewModel.isFy0Finished.collectAsState()
    val showTotalTime = viewModel.showTotalTime.collectAsState()
    val showTime = viewModel.showTime.collectAsState()
    // 等待进度
    val waitTotal = viewModel.waitTotal.collectAsState()
    val waitProgress = viewModel.waitProgress.collectAsState()
    // 试剂卡配置
    val checkStep = viewModel.checkStep.collectAsState()
    val sysConfig = viewModel.sysConfig.collectAsState()

    val curCardConfig = viewModel.curCardConfig.collectAsState()
    LaunchedEffect(viewState) {
        if (viewState.value == ViewState.Default) {
            viewModel.onLoad()
            viewModel.sysConfig.value =
                SysConfigService.findBean(ConfigSysBean.PREFIX, ConfigSysBean::class)
        }
    }

    if (navController.currentBackStackEntry != null) {
        val updateFlag by navController.currentBackStackEntry!!.savedStateHandle
            .getStateFlow("updateFlag", "false").collectAsState()
        LaunchedEffect(updateFlag) {
            if (updateFlag == "true") {
                viewModel.onLoad()
            }
        }
    }
    var waitVisible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000) // 闪烁间隔时间
            waitVisible = !waitVisible
        }
    }
    AppViewWrapper(viewState = viewState, onErrorClick = { navController.popBackStack() }) {
        AppScaffold(
            topBar = {
                AppTopBar(
                    navController = navController,
                    title = stringResource(id = R.string.work_main),
                    homeEnabled = true,
                    onHome = {
                        Timber.w("=========首页返回=========")
                        if (
                            actionValue.value == WorkMainViewModel.ACTION_CASE_WAIT
                            || actionValue.value == WorkMainViewModel.ACTION_WORK
                            || actionValue.value == WorkMainViewModel.ACTION_WORK_WAIT
                        ) {
                            viewModel.onActionWorkOutConfirm()
                        } else {
                            viewModel.onExitConfirm()
                        }
                    }
                )
            },
            bottomBar = {
                WorkMainBottomBar(
                    actionValue = actionValue,
                    checkStep = checkStep,
                    onReset = { viewModel.onReset() },
                    onActionStartNext = { viewModel.onActionStartNext() },
                    onActionCaseInputPre = { viewModel.onActionCaseInputPre() },
                    onActionCaseInputNext = { viewModel.onActionCaseInputNext() },
                    onActionCaseChipPre = { viewModel.onActionCaseChipPre() },
                    onActionCaseChipNext = { viewModel.onActionCaseChipNext() },
                    onActionCaseSamplePre = { viewModel.onActionCaseSamplePre() },
                    onActionCaseSampleNext = { viewModel.onActionCaseSampleNext() },
                    onActionCaseWaitPre = { viewModel.onActionCaseWaitPre() },
                    onActionCaseWaitNext = { viewModel.onActionCaseWaitNext() },
                    onActionWorkOutConfirm = { viewModel.onActionWorkOutConfirm() },
                    onActionContinueConfirm = { viewModel.onActionContinueConfirm() },
                    onActionWorkPre = { viewModel.onActionWorkPre() },
                    onActionWorkNext = { viewModel.onActionWorkNext() },
                    onActionReportGet = {
                        viewModel.onActionReportGet()
//                        viewModel.onActionReportGen()
                    },
                    onActionReportPre = { viewModel.onActionReportPre() },
                    onActionReportContinue = { viewModel.onActionReportContinue() },
                    onActionReportPrint = {
                        // AppToastUtil.devShow()
                        viewModel.onActionReportPrintConfirm()
                    },
                )
            },
            content = {
                WorkMainBody(
                    sysConfig,
                    bean,
                    curCardConfig,
                    stepValue,
                    actionValue,
                    viewModel,
                    progress,
                    showFy0Time,
                    isFy0Finished,
                    showTotalTime,
                    showTime,
                    waitTotal,
                    waitProgress,
                    waitVisible,
                    checkStep,
                    onDataDetail = {
                        viewModel.onDataDetail(bean.value) {
                            navController.navigate(RouteConfig.REPORT_DETAIL)
                        }
                    },
                    onReportGet = {
                        viewModel.onActionReportGet()
                    },
                    onBeanUpdate = { viewModel.onBeanUpdate(it) },
                    onUpload = { viewModel.uploadReport(bean.value) }
                )
            }
        )
    }

    WorkMainInteraction(
        viewModel = viewModel,
        actionState = actionState,
        onWorkout = { viewModel.onActionWorkOut() },
        onWorkDone = { viewModel.onWorkDone() },
        onWorkoutDone = {
            viewModel.onActionWorkOutDone {
                viewModel.viewModelScope.launch {
                    navController.navigate(RouteConfig.HOME_MAIN)
                }
            }
        },
        onWorkContinue = { viewModel.onActionWorkContinue() },
        onActionReportPrint = { viewModel.onActionReportPrint(bean.value.pdfPath) },
        onActionStart = { viewModel.onActionCaseInputPre() },
        onErrorNetwork = {
            viewModel.onActionWorkOutDone {
                viewModel.viewModelScope.launch {
                    navController.navigate(RouteConfig.SETTING_MAIN)
                }
            }
        },
        onClearInteraction = { viewModel.onClearInteraction() },
        onBackHome = {
            viewModel.onActionWorkOutDone {
                viewModel.viewModelScope.launch {
                    navController.navigate(RouteConfig.HOME_MAIN)
                }
            }
        },
    )
}

@Composable
fun WorkMainInteraction(
    viewModel: WorkMainViewModel,
    actionState: State<ActionState>,
    onWorkout: () -> Unit,
    onWorkoutDone: () -> Unit,
    onWorkContinue: () -> Unit,
    onWorkDone: () -> Unit,
    onActionReportPrint: () -> Unit,
    onActionStart: () -> Unit,
    onErrorNetwork: () -> Unit,
    onClearInteraction: () -> Unit,
    onBackHome: () -> Unit,
) {
    val context = LocalContext.current
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    if (actionState.value.event == WorkMainViewModel.EVT_EXIT) {
        AppConfirm(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = stringResource(id = R.string.work_main_exit),
            onCancel = { onClearInteraction() },
            onConfirm = { onBackHome() }
        )
    } else if (actionState.value.event == WorkMainViewModel.EVT_OUT_CONFIRM) {
        // 退出检测
        AppConfirm(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = stringResource(id = R.string.work_ing_exit),
            onCancel = { onWorkContinue() },
            onConfirm = { onWorkout() }
        )
    } else if (actionState.value.event == WorkMainViewModel.EVT_LOADING) {
        AppViewLoading(msg = actionState.value.msg)
    } else if (actionState.value.event == WorkMainViewModel.EVT_CHIP_TO_REMOVE) {
//        AppAlert(
//            title = stringResource(id = R.string.confirm_title_remind),
//            visible = true,
//            content = stringResource(id = R.string.work_remove_chip),
//            onOk = { onWorkoutDone() },
//            okText = stringResource(id = R.string.btn_label_i_know),
//        )
        onWorkoutDone()
    } else if (actionState.value.event == WorkMainViewModel.EVT_DEV_ERROR) {
        val msg = actionState.value.msg ?: "No Data"
        AppAlert(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = msg,
            onOk = { onActionStart() },
        )
    } else if (actionState.value.event == WorkMainViewModel.EVT_DEV_ERROR_NETWORK) {
        val msg = actionState.value.msg ?: "No Data"
        AppAlert(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = msg,
            onOk = { onErrorNetwork() },
        )
    } else if (actionState.value.event == WorkMainViewModel.EVT_DEV_WARNING) {
        val msg = actionState.value.msg ?: "No Data"
        AppViewLoading(msg = msg)
    } else if (actionState.value.event == WorkMainViewModel.EVT_PRINT_CONFIRM) {
        AppConfirm(
            title = stringResource(id = R.string.report_print),
            content = stringResource(id = R.string.report_print_more_confirm),
            visible = true,
            onCancel = onClearInteraction,
            onConfirm = {
                onActionReportPrint()
                // 创建浮窗
                AppSystemUtils.createFloatingWindow(context, windowManager)
            }
        )
    } else if (actionState.value.event == WorkMainViewModel.EVT_SHOW_REPORT_QRCODE) {
        val qrCodeContent = viewModel.qrCodeContent.collectAsState()
        AppQRCodeAlert(
            title = stringResource(id = R.string.confirm_title_report_qrcode),
            visible = true,
            content = qrCodeContent.value,
            onOk = { onClearInteraction() },
            okText = stringResource(id = R.string.btn_label_close),
        )
    } else if (actionState.value.event == WorkMainViewModel.EVT_CUT_OFF2_WAIT) {
        AppAlert(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = stringResource(id = R.string.work_case_cut_off2_wait),
            onOk = { onClearInteraction() },
        )
    }
}

/**
 * 内容主体
 */
@Composable
fun WorkMainBody(
    sysConfig: State<ConfigSysBean>,
    bean: State<CaseBean>,
    cardConfigBean: State<CardConfig>,
    stepValue: State<String>,
    actionValue: State<String>,
    viewModel: WorkMainViewModel,
    progress: State<Float>,
    showFy0Time: State<Boolean>,
    isFy0Finished: State<Boolean>,
    showTotalTime: State<Boolean>,
    showTime: State<Boolean>,
    waitTotal: State<Float>,
    waitProgress: State<Float>,
    waitVisible: Boolean,
    checkStep: State<Int>,
    onDataDetail: () -> Unit,
    onReportGet: () -> Unit,
    onBeanUpdate: (newBean: CaseBean) -> Unit,
    onUpload: (CaseBean) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(top = 25.dp)
            .fillMaxSize()
    ) {
        // TODO 简化信息
//        WorkStepBlock(stepValue)
        when (actionValue.value) {
            WorkMainViewModel.ACTION_START -> {
                WorkMainActionStartBlock(sysConfig.value, bean, onBeanUpdate)
            }

            WorkMainViewModel.ACTION_CASE_INPUT -> WorkMainActionCase4Block(bean, onBeanUpdate)
            WorkMainViewModel.ACTION_CASE_CHIP -> WorkActionCase1Block()

            WorkMainViewModel.ACTION_CASE_SAMPLE -> WorkMainActionCase3Block(bean, waitVisible)

//            WorkMainViewModel.ACTION_CASE_WAIT -> WorkMainActionCase2Block(
//                waitProgress,
//                waitTotal,
//                waitVisible
//            )
            WorkMainViewModel.ACTION_CASE_WAIT -> WorkMainActionCase2BlockV2(
                cardConfigBean,
                isFy0Finished,
            )

            WorkMainViewModel.ACTION_WORK_WAIT -> WorkMainActionWorkBlock(
                cardConfigBean,
                progress,
                showFy0Time,
                showTotalTime,
                checkStep,
                showTime,
            )

//            WorkMainViewModel.ACTION_WORK -> WorkMainActionWorkBlock(
//                cardConfigBean,
//                progress,
//                showFy0Time,
//                showTotalTime,
//                checkStep,
//                showTime,
//            )
            WorkMainViewModel.ACTION_WORK -> WorkMainActionWorkBlockV2(
                cardConfigBean
            ) { viewModel.onCutOff2TimeFinished() }

            WorkMainViewModel.ACTION_WORK_PROCESS

                -> WorkMainActionWorkBlock(
                cardConfigBean,
                progress,
                showFy0Time,
                showTotalTime,
                checkStep,
                showTime,
            )

            WorkMainViewModel.ACTION_REPORT1 -> WorkActionReport1Block(
                bean,
                onDataDetail,
                onReportGet,
                onBeanUpdate,
                onUpload,
            )

            WorkMainViewModel.ACTION_REPORT2 -> WorkMainActionReport2Block(bean)
        }
    }
}


@Preview
@Composable
fun WorkMainPreview() {
    val navController = rememberNavController()
    val viewModel: WorkMainViewModel = viewModel()
    LaunchedEffect(key1 = null) {
        viewModel.viewState.value = ViewState.LoadSuccess()
        viewModel.actionState.value = ActionState.Default
//        viewModel.actionState.value = ActionState(WorkMainViewModel.EVT_CHIP_TO_REMOVE)
//        viewModel.actionState.value = ActionState(WorkMainViewModel.EVT_CHECK_DONE)
//        viewModel.actionState.value = ActionState(WorkMainViewModel.EVT_DEV_ERROR, "无法读取芯片，请重新放入")
//        viewModel.actionState.value = ActionState(WorkMainViewModel.EVT_CHECK_DONE, "无法读取芯片，请重新放入")

        viewModel.action.value = WorkMainViewModel.ACTION_REPORT1

        viewModel.progress.value = 0F

        viewModel.bean.value = AppSampleUtils.genCaseInfo()
//        viewModel.bean.value.type = CaseBean.TYPE_3LJ_BIOAGE_L1
    }
    AppPreviewWrapper {
        WorkMain(navController, viewModel)
    }
}