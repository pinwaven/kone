package poct.device.app.ui.workconfig

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import poct.device.app.bean.FileInfo
import poct.device.app.component.AppAlert
import poct.device.app.component.AppConfirm
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
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.theme.dangerColor
import poct.device.app.utils.app.AppSampleUtils
import poct.device.app.utils.app.AppToastUtil
import timber.log.Timber


/**
 * 页面定义
 */
@Composable
fun WorkConfigCard(navController: NavController, viewModel: WorkConfigCardViewModel = viewModel()) {
    val viewState = viewModel.viewState.collectAsState()
    val actionState = viewModel.actionState.collectAsState()
    val records = viewModel.records.collectAsState()
    // U盘导入信息
    val stepValue = viewModel.stepByUDisk.collectAsState()
    val files = viewModel.files.collectAsState()
    val selectFiles = viewModel.selectFiles.collectAsState()
    // 扫码录入信息
    val scannerInfo = viewModel.scannerInfo.collectAsState()

    LaunchedEffect(ViewState) {
        if (viewState.value == ViewState.Default) {
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
                    title = stringResource(id = R.string.work_config_card),
                    backEnabled = true
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp)
            ) {
                WorkConfigCardButtons(
                    onImportByUDiskConfirm = {
                        viewModel.onImportByUDiskConfirm()
                    },
                    onImportByNetworkConfirm = { AppToastUtil.devShow() },
                    onImportByScannerConfirm = { viewModel.onImportByScannerConfirm() },
                    onImportByManual = {
                        AppParams.varCardConfigMode = "add"
                        AppParams.varCardConfig = CardConfigBean.Empty
                        navController.navigate(RouteConfig.WORK_CONFIG_CARD_ADD)
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
                AppList<CardConfigBean>(
                    modifier = Modifier.fillMaxWidth(),
                    records = records
                ) { bean ->
                    WorkConfigCardItem(
                        bean = bean,
                        onDelete = {
                            viewModel.onDelete(it)
                        },
                        onView = {
                            viewModel.onItemDetail(it) {
                                navController.navigate(
                                    RouteConfig.WORK_CONFIG_CARD_VIEW
                                )
                            }
                        },
                        onViewQrCode = {
                            viewModel.onViewQrCode(it) {
                                navController.navigate(
                                    RouteConfig.WORK_CONFIG_CARD_QR
                                )
                            }
                        },
                    )
                }
            }
        }
    }
    WorkConfigCardInteraction(
        viewModel,
        step = stepValue,
        files = files,
        selectFiles = selectFiles,
        actionState = actionState,
        scannerInfo = scannerInfo,
        onScannerUpdate = { viewModel.onScannerUpdate(it) },
        onScannerImport = { viewModel.onScannerImport() },
        onScannerFinish = {
            viewModel.onScannerFinish(it) {
                navController.navigate(
                    RouteConfig.WORK_CONFIG_CARD_VIEW
                )
            }
        },
        onScannerCancel = { viewModel.onScannerCancel() },
        onImportByUDisk = { context: Context, imgUri: Uri ->
            viewModel.onImportByUDisk(context, imgUri)
        },
        onImportFinish = {
            viewModel.onClearInteraction()
            viewModel.onLoad()
        },
        onSelectFile = { viewModel.onSelectFile(it) },
        onDeleteConfirm = { viewModel.onDeleteConfirm(it) },
        onDeleteDone = { viewModel.onDeleteDone() },
        onClearInteraction = { viewModel.onClearInteraction() },
    )
}

@Composable
private fun WorkConfigCardInteraction(
    viewModel: WorkConfigCardViewModel,
    step: State<String>,
    files: State<List<FileInfo>>,
    selectFiles: State<List<FileInfo>>,
    scannerInfo: State<String>,
    onScannerUpdate: (String) -> Unit,
    onScannerImport: () -> Unit,
    onScannerCancel: () -> Unit,
    onScannerFinish: (cardConfigBean: CardConfigBean) -> Unit,
    actionState: State<ActionState>,
    onImportByUDisk: (Context, Uri) -> Unit,
    onImportFinish: () -> Unit,
    onSelectFile: (selectFile: FileInfo) -> Unit,
    onDeleteConfirm: (cardConfigBean: CardConfigBean) -> Unit,
    onDeleteDone: () -> Unit,
    onClearInteraction: () -> Unit
) {
    val event = actionState.value.event
    val msg = actionState.value.msg
    val payload = actionState.value.payload
    val context = LocalContext.current

    val activity = App.getContext()
    val manager = activity.packageManager
    val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val floatButton = Button(App.getContext()).apply {
        text = "返回"
        textSize = 16F
        width = 80
        height = 32
        setTextColor(App.getContext().getColor(R.color.white))
        setBackgroundColor(App.getContext().getColor(R.color.primaryColor))

        setOnClickListener {
            windowManager.removeView(it)

            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            // 启动主屏幕来清除所有顶层Activity
            activity.startActivity(intent)

            // 回到我们的应用
            val launchIntent =
                manager.getLaunchIntentForPackage(activity.packageName)
            launchIntent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            activity.startActivity(launchIntent)
        }
    }
    val resultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            windowManager.removeView(floatButton)
            if (result.resultCode == Activity.RESULT_OK) {
                Timber.w("====fileResult $result")

                val fileUrl = result.data?.data
                fileUrl?.let { uri ->
                    Timber.w("====fileUrl $uri")
                    onImportByUDisk(context, uri)
                }
            }
        }
    )

    if (event == WorkConfigCardViewModel.EVENT_LOADING) {
        AppViewLoading()
    } else if (event == WorkConfigCardViewModel.EVENT_IMPORT_FROM_SCANNER) {
        WorkConfigCardImportByScanner(
            visible = true,
            scannerInfo = scannerInfo,
            onCancel = { onScannerCancel() },
            onImportByScanner = { onScannerImport() },
            onScannerUpdate = onScannerUpdate
        )
    } else if (event == WorkConfigCardViewModel.EVENT_IMPORT_FROM_SCANNER_DONE) {
        val bean: CardConfigBean = (actionState.value.payload as CardConfigBean?)!!
        AppAlert(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = stringResource(id = R.string.import_from_u_disk_ok),
            onOk = { onScannerFinish(bean) },
            okText = stringResource(id = R.string.btn_label_i_know),
        )
    } else if (event == WorkConfigCardViewModel.EVENT_IMPORT_FROM_U_DISK) {
        AppConfirm(
            visible = true,
            content = stringResource(id = R.string.work_config_import_confirm),
            onCancel = onClearInteraction,
            confirmText = stringResource(id = R.string.btn_label_select),
            onConfirm = {
                val layoutParams = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.START or Gravity.TOP
                    x = 260 // 初始位置
                    y = 100 // 初始位置
                }

                windowManager.addView(floatButton, layoutParams)

                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                resultLauncher.launch(intent)
            }
        )
//        WorkConfigCardImportByUDisk(
//            visible = true,
//            files = files,
//            step = step,
//            selectFiles = selectFiles,
//            onCancel = { onClearInteraction() },
//            onSelectFile = {onSelectFile(it)},
//            onImportByUDisk ={ onImportByUDisk() }
//        )
    } else if (event == WorkConfigCardViewModel.EVENT_IMPORT_FROM_U_DISK_DONE) {
        AppAlert(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = stringResource(id = R.string.import_from_u_disk_ok),
            onOk = { onImportFinish() },
            okText = stringResource(id = R.string.btn_label_i_know),
        )
    } else if (event == WorkConfigCardViewModel.EVENT_DELETE_CONFIRM) {
        val bean: CardConfigBean = (actionState.value.payload as CardConfigBean?)!!
        val msg = stringResource(id = R.string.work_config_remove_card_confirm)
        AppConfirmDanger(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = msg.format(bean.code),
            confirmText = stringResource(id = R.string.btn_label_delete),
            onCancel = { onClearInteraction() },
            onConfirm = {
                onDeleteConfirm(bean)
                onClearInteraction()
            }
        )
    } else if (event == WorkConfigCardViewModel.EVENT_DELETE_DONE) {
        val bean: CardConfigBean = (actionState.value.payload as CardConfigBean?)!!
        val msg = stringResource(id = R.string.work_config_remove_card_done)
        AppAlert(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = msg.format(bean.code),
            onOk = { onDeleteDone() }
        )
    } else if (event == WorkConfigCardViewModel.EVENT_ERROR) {
        msg?.let {
            AppAlert(
                visible = true,
                content = it,
                onOk = { onClearInteraction() }
            )
        }
    }
}

@Composable
private fun WorkConfigCardItem(
    bean: CardConfigBean,
    onDelete: (record: CardConfigBean) -> Unit,
    onView: (record: CardConfigBean) -> Unit,
    onViewQrCode: (record: CardConfigBean) -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = bean.name)
                IconButton(onClick = { onDelete(bean) }) {
                    Icon(
                        painter = painterResource(id = R.mipmap.sc_icon),
                        tint = dangerColor,
                        contentDescription = null
                    )
                }
            }
            AppDivider()
            // 记录
            val labelWidth = 72.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, start = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.weight(1F)
                    ) {
                        AppValueWrapper(
                            labelWidth = labelWidth,
                            text = stringResource(id = R.string.work_config_f_code),
                            value = bean.code
                        )
                    }
//                    Row(
//                        modifier = Modifier.weight(1F)
//                    ) {
//                        AppValueWrapper(
//                            labelWidth = labelWidth,
//                            text = stringResource(id = R.string.work_config_f_code),
//                            value = bean.code
//                        )
//                    }
                }
                AppValueWrapper(
                    labelWidth = labelWidth,
                    text = stringResource(id = R.string.work_config_f_prod_date),
                    value = bean.prodDate
                )
                AppValueWrapper(
                    labelWidth = labelWidth,
                    text = stringResource(id = R.string.work_config_f_end_date),
                    value = bean.endDate
                )
            }
            // 操作
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 10.dp),
                horizontalArrangement = Arrangement.End
            ) {
                AppFilledButton(
                    modifier = Modifier
                        .width(90.dp)
                        .height(30.dp),
                    text = stringResource(id = R.string.work_config_view_qr),
                    onClick = { onViewQrCode(bean) },
                )
                Spacer(modifier = Modifier.width(16.dp))
                AppFilledButton(
                    modifier = Modifier
                        .width(90.dp)
                        .height(30.dp),
                    text = stringResource(id = R.string.btn_label_card_info),
                    onClick = { onView(bean) },
                )
            }
        }
    }
}


@Composable
fun WorkConfigCardButtons(
    onImportByUDiskConfirm: () -> Unit,
    onImportByNetworkConfirm: () -> Unit,
    onImportByScannerConfirm: () -> Unit,
    onImportByManual: () -> Unit,
) {
    val btnModifier = Modifier
        .width(100.dp)
        .height(36.dp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // TODO 简化信息
//        AppOutlinedButton(
//            modifier = btnModifier,
//            onClick = { onImportByUDiskConfirm() },
//            text = stringResource(id = R.string.btn_label_import_u)
//        )
//        AppOutlinedButton(
//            modifier = btnModifier,
//            onClick = { onImportByScannerConfirm() },
//            text = stringResource(id = R.string.btn_label_import_scanner)
//        )
        AppOutlinedButton(
            modifier = btnModifier,
            onClick = onImportByManual,
            text = stringResource(id = R.string.btn_label_import_manual)
        )
    }
}


@Preview
@Composable
fun WorkConfigCardPreview() {
    val navController = rememberNavController()
    val viewModel: WorkConfigCardViewModel = viewModel()
    LaunchedEffect(Unit) {
        viewModel.records.value = AppSampleUtils.genCardInfoList()

        viewModel.viewState.value = ViewState.LoadSuccess()
        viewModel.stepByUDisk.value = WorkConfigCardViewModel.STEP_IMPORT_DONE
//        viewModel.files.value = AppSampleUtils.genFiles()
        viewModel.actionState.value =
            ActionState(event = WorkConfigCardViewModel.EVENT_IMPORT_FROM_U_DISK)
    }

    AppPreviewWrapper {
        WorkConfigCard(navController, viewModel)
    }
}