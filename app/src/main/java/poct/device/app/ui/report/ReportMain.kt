package poct.device.app.ui.report

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.RouteConfig
import poct.device.app.bean.CaseBean
import poct.device.app.component.AppAlert
import poct.device.app.component.AppConfirmDanger
import poct.device.app.component.AppConfirmExport
import poct.device.app.component.AppList
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewLoading
import poct.device.app.component.AppViewWrapper
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.utils.app.AppSampleUtils

/**
 * 页面定义
 */
@Composable
fun ReportMain(navController: NavController, viewModel: ReportMainViewModel = viewModel()) {
    val viewState = viewModel.viewState.collectAsState()
    val actionState = viewModel.actionState.collectAsState()
    val exportUrl = viewModel.exportUrl.collectAsState()
    val records = viewModel.records.collectAsState()
    val selected = viewModel.selected.collectAsState()
    val query = viewModel.query.collectAsState()
    val listState = rememberLazyListState()


    LaunchedEffect(viewState) {
        if (viewState.value == ViewState.Default) {
            viewModel.load()
        }
    }
    if (navController.currentBackStackEntry != null) {
        val updateFlag by navController.currentBackStackEntry!!.savedStateHandle
            .getStateFlow("updateFlag", "false").collectAsState()
        LaunchedEffect(updateFlag) {
            if (updateFlag == "true") {
                viewModel.load()
            }
        }
    }

    AppViewWrapper(
        viewState = viewState,
        onErrorClick = { navController.popBackStack() }
    ) {
        AppScaffold(
            topBar = {
                AppTopBar(
                    navController = navController,
                    title = stringResource(id = R.string.report_main),
                    backEnabled = true
                )
            },
            bottomBar = {
                ReportMainBottomBar(
                    records = records,
                    selected = selected,
                    onCheckAll = { viewModel.onCheckAll() },
                    onDelMoreConfirm = { viewModel.onDelMoreConfirm() },
                    onExportMoreConfirm = { viewModel.onExportMoreConfirm() },
                )
            }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ReportMainFilter(query) { viewModel.onQueryUpdate(it) }
                AppList(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 15.dp),
                    state = listState,
                    records = records
                ) { bean ->
                    ReportMainRecordItem(
                        bean = bean,
                        reportExist = viewModel.pdfReportExist(bean),
                        selected = selected,
                        onItemChecked = { id: String -> viewModel.onCheckItem(id) },
                        onItemExportConfirm = { viewModel.onItemExportConfirm(it) },
                        onItemDelConfirm = { viewModel.onItemDeleteConfirm(it) },
                        onItemEdit = {
                            AppParams.varReport = bean
                            navController.navigate(RouteConfig.REPORT_EDIT)
                        },
                        onItemDetail = {
                            viewModel.onItemDetail(bean) {
                                navController.navigate(RouteConfig.REPORT_DETAIL)
                            }
                        },
                        onItemPDF = {
                            viewModel.onItemPDF(it) {
                                navController.navigate(RouteConfig.REPORT_PDF)
                            }
                        },
                        onGenPDF = {
                            viewModel.onGenPDF(it)
                        },
                        onUpload = {
                            viewModel.uploadReport(it)
                        }
                    )
                }
            }
        }
    }
    ReportMainInteraction(
        actionState = actionState,
        exportUrl,
        onItemDelete = { viewModel.onDelete(it) },
        onItemExport = { record: CaseBean, expType: Int ->
            viewModel.onExport(record, expType = expType)
        },
        onDelMore = { viewModel.onDelMore() },
        onExportMore = { expType: Int ->
            viewModel.onExportMore(expType)
        },
        onExportReset = { viewModel.onExportReset() },
        onExportDone = { viewModel.onClearInteraction() },
    ) { viewModel.onClearInteraction() }
}

@Composable
fun ReportMainInteraction(
    actionState: State<ActionState>,
    exportUrl: State<String>,
    onItemDelete: (bean: CaseBean) -> Unit,
    onItemExport: (bean: CaseBean, expType: Int) -> Unit,
    onDelMore: () -> Unit,
    onExportMore: (Int) -> Unit,
    onExportReset: () -> Unit,
    onExportDone: () -> Unit,
    onClearInteraction: () -> Unit,
) {
    if (actionState.value.event == ReportMainViewModel.EVT_LOADING) {
        AppViewLoading()
    } else if (actionState.value.event == ReportMainViewModel.EVT_DEL_CONFIRM) {
        val bean: CaseBean = (actionState.value.payload as CaseBean?)!!
        AppConfirmDanger(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = stringResource(id = R.string.report_del_confirm).format(bean.caseId),
            onCancel = { onClearInteraction() },
            confirmText = stringResource(id = R.string.btn_label_delete),
            onConfirm = { onItemDelete(bean) }
        )

    } else if (actionState.value.event == ReportMainViewModel.EVT_EXP_CONFIRM) {
        val bean: CaseBean = (actionState.value.payload as CaseBean?)!!
        AppConfirmExport(
            visible = true,
            onCancel = { onClearInteraction() },
            onConfirm = {
                onItemExport(bean, it)
            }
        )
    } else if (actionState.value.event == ReportMainViewModel.EVT_DEL_MORE_CONFIRM) {
        AppConfirmDanger(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = stringResource(id = R.string.report_del_more_confirm),
            onCancel = { onClearInteraction() },
            confirmText = stringResource(id = R.string.btn_label_delete),
            onConfirm = { onDelMore() }
        )
    } else if (actionState.value.event == ReportMainViewModel.EVT_EXP_MORE_CONFIRM) {
        AppConfirmExport(
            visible = true,
            onCancel = { onClearInteraction() },
            onConfirm = { onExportMore(it) }
        )
    } else if (actionState.value.event == ReportMainViewModel.EVT_NO_SELECTED) {
        AppAlert(
            visible = true,
            content = stringResource(id = R.string.pls_select_records),
            onOk = { onClearInteraction() }
        )
    } else if (actionState.value.event.startsWith(ReportMainViewModel.EVT_EXPORT_ACTION)) {
        val msg = actionState.value.msg.toString()
        ReportMainExportDialog(
            actionState.value.event,
            exportUrl.value,
            errorMsg = msg.ifEmpty { "unknown error" },
            onErrorOk = { onExportReset() },
            onExportDone = { onExportDone() },
        )
    } else if (actionState.value.event == ReportMainViewModel.EVT_ERROR) {
        actionState.value.msg?.let {
            AppAlert(
                visible = true,
                content = it,
                onOk = { onClearInteraction() }
            )
        }
    }
}

@Preview
@Composable
fun ReportMainPreview() {
    val navController: NavController = rememberNavController()
    val viewModel: ReportMainViewModel = viewModel()
    LaunchedEffect(null) {
        viewModel.records.value = AppSampleUtils.genCaseInfos()
        viewModel.viewState.value = ViewState.LoadSuccess()
        viewModel.actionState.value = ActionState.Default
//        viewModel.expState.value = ViewState.LoadError("未插入“U盘”，请插入“U盘”后重试")
//        viewModel.expState.value = ViewState.Loading()
//        viewModel.expState.value = ViewState.LoadSuccess()
    }
    AppPreviewWrapper {
        ReportMain(navController, viewModel)
    }
}