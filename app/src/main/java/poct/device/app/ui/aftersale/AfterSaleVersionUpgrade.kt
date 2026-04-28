package poct.device.app.ui.aftersale

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import info.szyh.comm.CommSocketMessageRecEvent
import org.greenrobot.eventbus.Subscribe
import poct.device.app.R
import poct.device.app.bean.VersionBean
import poct.device.app.bean.VersionUpgradeInfo
import poct.device.app.component.AppAlert
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewLoading
import poct.device.app.component.AppViewWrapper
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.theme.bgColor
import poct.device.app.theme.tipFontColor
import poct.device.app.ui.report.ReportMainViewModel
import poct.device.app.ui.sysfun.SysFunInfoViewModel
import poct.device.app.utils.app.AppEventUtils
import poct.device.app.utils.app.AppSampleUtils
import timber.log.Timber


/**
 * 页面定义
 */
@Composable
fun AfterSaleVersionUpgrade(
    navController: NavController,
    viewModel: AfterSaleVersionUpgradeViewModel = viewModel(),
) {
    val viewState by viewModel.viewState.collectAsState()
    val actionState = viewModel.actionState.collectAsState()
    val record by viewModel.record.collectAsState()
    val version = viewModel.version.collectAsState()
    val context = LocalContext.current
    val resultLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            val apkUri = it.data?.data
            apkUri?.let { uri ->
                Timber.w("====apkUri${uri}")
                viewModel.installApk(context, uri)
            }
        }
    }

    DisposableEffect(key1 = Unit) {
        val receiver = object {
            @Subscribe
            fun pluginMessageHandler(event: CommSocketMessageRecEvent) {
                viewModel.handlePluginEvent(event)
            }
        }
        AppEventUtils.register(receiver)
        onDispose {
            AppEventUtils.unregister(receiver)
        }
    }

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
                    title = stringResource(id = R.string.sys_fun),
                    backEnabled = true,
                    onBack = {// 设置刷新标志到前一个页面
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("shouldRefresh", true)
                        navController.popBackStack()
                    }
                )
            },
        ) {
            AfterSaleVersionUpgradeBody(
                record = record,
                onUDiskUpgrade = {
                    //viewModel.onUDiskUpgrade()
                    val intent = Intent(Intent.ACTION_GET_CONTENT).setType("*/*")
                    resultLauncher.launch(intent)
                },
                onCheckVersion = { viewModel.onCheckVersion() }
            )
        }
    }

    AfterSaleVersionUpgradeInteraction(
        actionState = actionState,
        version = version,
        onClearInteraction = { viewModel.onClearInteraction() },
    ) { viewModel.onUpgrade() }
}


@Composable
fun AfterSaleVersionUpgradeInteraction(
    actionState: State<ActionState>,
    version: State<VersionBean>,
    onClearInteraction: () -> Unit,
    onUpgradeNow: () -> Unit,
) {
    val state by actionState
    if (actionState.value.event == ReportMainViewModel.EVT_LOADING) {
        AppViewLoading(msg = actionState.value.msg)
    } else if (state.event == AfterSaleVersionUpgradeViewModel.EVT_HANDLE) {
        AfterSaleVersionHandleDialog(
            version = version.value,
            onUpgradeCancel = onClearInteraction,
            onUpgradeNow = onUpgradeNow
        )
    } else if (state.event == AfterSaleVersionUpgradeViewModel.EVT_UPGRADE) {
        val msg = state.msg ?: "Pls set msg for event"
        AppAlert(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = msg,
            onOk = { onClearInteraction() }
        )
    } else if (state.event == AfterSaleVersionUpgradeViewModel.EVT_CHECKING) {
        val msg = state.msg
        AppViewLoading(msg = msg, width = 160.dp)
    } else if (state.event == AfterSaleVersionUpgradeViewModel.EVT_ERROR) {
        val msg = state.msg ?: "Pls set msg for event"
        AppAlert(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = msg,
            onOk = { onClearInteraction() }
        )
    } else if (state.event == AfterSaleVersionUpgradeViewModel.EVT_CHECK_LATEST) {
        val msg = state.msg ?: "Pls set msg for event"
        AppAlert(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = msg,
            onOk = { onClearInteraction() }
        )
    } else if (state.event == AfterSaleVersionUpgradeViewModel.EVT_CHECK_DONE) {
        val msg = state.msg ?: "Pls set msg for event"
        AppAlert(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = msg,
            onOk = { onClearInteraction() }
        )
    } else if (state.event == AfterSaleVersionUpgradeViewModel.EVT_DOWNLOADING) {
        val msg = state.msg ?: "正在下载..."
        AppViewLoading(msg = msg)
    } else if (state.event == AfterSaleVersionUpgradeViewModel.EVT_INSTALLING) {
        val msg = state.msg ?: "正在安装..."
        AppViewLoading(msg = msg)
    } else if (state.event == AfterSaleVersionUpgradeViewModel.EVT_DOWNLOAD_FAILED) {
        val msg = state.msg ?: "下载失败"
        AppAlert(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = msg,
            onOk = { onClearInteraction() }
        )
    } else if (state.event == AfterSaleVersionUpgradeViewModel.EVT_INSTALL_FAILED) {
        val msg = state.msg ?: "安装失败"
        AppAlert(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = msg,
            onOk = { onClearInteraction() }
        )
    } else if (state.event == AfterSaleVersionUpgradeViewModel.EVT_LAUNCH_APP) {
        val msg = state.msg ?: "应用已重启"
        AppAlert(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = msg,
            onOk = { onClearInteraction() }
        )
    }
}

/**
 * 内容主体
 */
@Composable
fun AfterSaleVersionUpgradeBody(
    record: VersionUpgradeInfo,
    onUDiskUpgrade: () -> Unit = {},
    onCheckVersion: () -> Unit = {},
) {
    Timber.tag("record").d(record.sysInfo.software)
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(15.dp),
        color = bgColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(50.dp))
            Image(
                painter = painterResource(id = R.mipmap.bbgx_img),
                contentDescription = ""
            )
            Spacer(modifier = Modifier.height(12.dp))
            val name = record.sysInfo.name
            val type = record.sysInfo.type
            Text(
                text = "$name($type)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = stringResource(id = R.string.after_sale_version_current))
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 96.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    color = tipFontColor,
                    text = stringResource(id = R.string.sys_fun_xtpz_info_f_software)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(color = tipFontColor, text = record.sysInfo.software)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 96.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    color = tipFontColor,
                    text = stringResource(id = R.string.sys_fun_xtpz_info_f_hardware)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(color = tipFontColor, text = record.sysInfo.hardware)
            }
            Spacer(modifier = Modifier.height(53.dp))
            // TODO 简化信息
//            Row(
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                val ctTxt = if (record.count > 0) {
//                    if (record.count > 9) "9+" else record.count.toString()
//                } else ""
//                AppBadge(modifier = Modifier.size(76.dp), text = ctTxt) {
//                    Column(
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .clickable {
//                                onUDiskUpgrade()
//                            },
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        Spacer(modifier = Modifier.height(7.dp))
//                        Image(
//                            painter = painterResource(id = R.mipmap.bbgx_icon),
//                            contentDescription = ""
//                        )
//                        Spacer(modifier = Modifier.height(12.dp))
//                        Text(text = stringResource(id = R.string.after_sale_version_upgrade))
//                    }
//                }
//                Spacer(modifier = Modifier.width(80.dp))
//                Box(modifier = Modifier.size(76.dp)) {
//                    Column(
//                        modifier = Modifier.fillMaxSize(),
//                        horizontalAlignment = Alignment.CenterHorizontally
//                    ) {
//                        Spacer(modifier = Modifier.height(7.dp))
//                        Image(
//                            painter = painterResource(id = R.mipmap.gxjl_icon),
//                            contentDescription = ""
//                        )
//                        Spacer(modifier = Modifier.height(12.dp))
//                        Text(text = stringResource(id = R.string.after_sale_version_history))
//                    }
//                }
//            }
//            Spacer(modifier = Modifier.height(80.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppFilledButton(
                    modifier = Modifier
                        .width(120.dp)
                        .height(36.dp),
                    onClick = { onCheckVersion() },
                    text = stringResource(id = R.string.after_sale_check_upgrade)
                )
            }
        }
    }
}

@Preview
@Composable
fun AfterSaleVersionUpgradePreview() {
    val sfViewModel: SysFunInfoViewModel = viewModel()
    val viewModel: AfterSaleVersionUpgradeViewModel = viewModel()
    LaunchedEffect(null) {
        viewModel.record.value = AppSampleUtils.genVersionUpgradeInfo(3)
        viewModel.version.value = AppSampleUtils.genVersionInfo()
        viewModel.viewState.value = ViewState.LoadSuccess()
//        viewModel.actionState.value = ActionState(AfterSaleVersionUpgradeViewModel.EVT_CHECKING, "正在检查版本中...")
//        viewModel.actionState.value =
//            ActionState(event = AfterSaleVersionUpgradeViewModel.EVT_HANDLE)
//        viewModel.viewState.value = ViewState.Event(
//            event = AfterSaleVersionUpgradeViewModel.EVT_UPGRADE,
//            msg = "版本升级成功"
//        )
    }
    val navController = rememberNavController()
    AppPreviewWrapper {
        AfterSaleVersionUpgrade(navController, viewModel)
    }
}