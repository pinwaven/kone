package poct.device.app.ui.report

import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import poct.device.app.RouteConfig
import poct.device.app.bean.CaseBean
import poct.device.app.component.AppAlert
import poct.device.app.component.AppConfirm
import poct.device.app.component.AppPDFView
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewWrapper
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.theme.bgColor
import poct.device.app.thirdparty.FrosApi
import poct.device.app.utils.app.AppSampleUtils
import poct.device.app.utils.app.AppSystemUtils

/**
 * 页面定义
 */
@Composable
fun ReportPDF(navController: NavController, viewModel: ReportPDFViewModel = viewModel()) {
    val viewState by viewModel.viewState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val record by viewModel.record.collectAsState()
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
        onErrorClick = {
            navController.popBackStack()
        }
    ) {
        AppScaffold(
            topBar = {
                AppTopBar(
                    navController = navController,
                    title = stringResource(id = R.string.report_pdf),
                    backEnabled = true,
                    onBack = {
                        navController.popBackStack()
                    },
                )
            },
            bottomBar = {
                ReportPDFBottomBar(
                    onLisConfirm = { viewModel.onLisConfirm() },
                    onEdit = {
                        if (navController.previousBackStackEntry != null) {
                            navController.previousBackStackEntry!!.savedStateHandle["updateFlag"] =
                                "true"
                        }
                        navController.navigate(RouteConfig.REPORT_EDIT)
                    },
                    onPrintConfirm = { viewModel.onPrintConfirm() }
                )
            }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 15.dp, top = 15.dp, end = 15.dp),
                color = bgColor,
                shape = RoundedCornerShape(4.dp)
            ) {
                AppPDFView(modifier = Modifier.fillMaxSize(), pdfPath = record.pdfPath)
            }
        }
    }

    ReportPDFInteraction(
        bean = record,
        actionState = actionState,
        onPrintPdf = { viewModel.onPrint(record.pdfPath) }
    ) { viewModel.onClearInteraction() }
}

@Composable
fun ReportPDFInteraction(
    bean: CaseBean,
    actionState: ActionState,
    onPrintPdf: () -> Unit,
    onClearInteraction: () -> Unit,
) {
    val context = LocalContext.current
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    if (actionState.event == ReportPDFViewModel.EVT_LIS_CONFIRM) {
        FrosApi.uploadPatientReportDataToServer(bean)
        onClearInteraction()

//        AppToastUtil.devShow()
//        LaunchedEffect(key1 = Unit) {
//            delay(1000)
//            onClearInteraction()
//        }
    } else if (actionState.event == ReportPDFViewModel.EVT_PRINT_CONFIRM) {
        AppConfirm(
            title = stringResource(id = R.string.report_print),
            content = stringResource(id = R.string.report_print_more_confirm),
            visible = true,
            onCancel = onClearInteraction,
            onConfirm = onPrintPdf
        )
    } else if (actionState.event == ReportPDFViewModel.EVT_PRINT_CONFIRM_AFTER) {
        // 创建浮窗
        AppSystemUtils.createFloatingWindow(context, windowManager)
        onClearInteraction()
    } else if (actionState.event == ReportPDFViewModel.EVT_REPORT_ERROR) {
        actionState.msg?.let {
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
fun ReportPDFPreview(
    navController: NavController = rememberNavController(),
    viewModel: ReportPDFViewModel = viewModel(),
) {
    LaunchedEffect(null) {
        viewModel.record.value = AppSampleUtils.genCaseInfo()
        viewModel.viewState.value = ViewState.LoadSuccess()
    }
    AppPreviewWrapper {
        ReportPDF(navController)
    }
}