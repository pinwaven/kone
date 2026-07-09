package poct.device.app.ui.sysfun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import poct.device.app.AppParams
import poct.device.app.BuildConfig
import poct.device.app.R
import poct.device.app.bean.ConfigSysBean
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppOutlinedButton
import poct.device.app.component.wakeScreenOnTouch
import poct.device.app.entity.service.SysConfigService
import poct.device.app.theme.bgColor
import poct.device.app.theme.fontColor
import poct.device.app.thirdparty.NanoApi
import poct.device.app.ui.aftersale.AfterSaleVersionUpgradeViewModel
import poct.device.app.utils.app.VersionUtils

private sealed class TestState {
    object Idle : TestState()
    object Running : TestState()
    data class Done(val result: NanoApi.ProbeResult) : TestState()
}

private sealed class UpgradeCheckState {
    object Idle : UpgradeCheckState()
    object Checking : UpgradeCheckState()
    object UpToDate : UpgradeCheckState()
    data class Available(val version: String, val url: String) : UpgradeCheckState()
    data class Upgrading(val msg: String, val progress: Float? = null) : UpgradeCheckState()
    data class Error(val msg: String) : UpgradeCheckState()
}

/**
 * 版本升级：按钮 + 弹窗（检查 Nano 升级、下载安装进度）
 */
@Composable
fun SysFunInfoUpgradeBlock(
    upgradeVm: AfterSaleVersionUpgradeViewModel = viewModel(),
) {
    val scope = rememberCoroutineScope()
    var dialogVisible by remember { mutableStateOf(false) }
    var upgradeState by remember { mutableStateOf<UpgradeCheckState>(UpgradeCheckState.Idle) }
    // 最近一次检查到的可用版本；安装界面启动后恢复此状态，用户取消安装可重新下载/安装
    val lastAvailable = remember { mutableStateOf<UpgradeCheckState.Available?>(null) }

    suspend fun runCheck() {
        upgradeState = UpgradeCheckState.Checking
        val resp = NanoApi.checkUpgrade()
        upgradeState = when {
            resp == null -> UpgradeCheckState.Error("无法连接 Nano 升级接口")
            resp.version.isNullOrEmpty() || resp.url.isNullOrEmpty() ->
                UpgradeCheckState.Error("Nano 暂无可用版本")
            VersionUtils.isLessThan(BuildConfig.VERSION_NAME, resp.version) ->
                UpgradeCheckState.Available(resp.version, resp.url).also { lastAvailable.value = it }
            else -> UpgradeCheckState.UpToDate
        }
    }

    // Mirror the ViewModel's actionState into our upgradeState so progress/errors show in the dialog
    LaunchedEffect(upgradeVm) {
        upgradeVm.actionState.collectLatest { action ->
            when (action.event) {
                AfterSaleVersionUpgradeViewModel.EVT_DOWNLOADING -> {
                    val msg = action.msg ?: "下载中…"
                    val pct = Regex("(\\d+)%").find(msg)?.groupValues?.get(1)?.toFloatOrNull()
                    upgradeState = UpgradeCheckState.Upgrading(msg, pct?.div(100f))
                }
                AfterSaleVersionUpgradeViewModel.EVT_INSTALLING -> {
                    // 系统安装界面已启动，进度条复位；恢复"发现新版本"面板以便取消安装后重试
                    upgradeState = lastAvailable.value ?: UpgradeCheckState.Idle
                }
                AfterSaleVersionUpgradeViewModel.EVT_ERROR -> upgradeState = UpgradeCheckState.Error(action.msg ?: "升级失败")
                AfterSaleVersionUpgradeViewModel.EVT_DOWNLOAD_FAILED -> upgradeState = UpgradeCheckState.Error(action.msg ?: "下载失败")
            }
        }
    }

    // 打开弹窗时自动检查一次；下载/安装进行中不打断
    LaunchedEffect(dialogVisible) {
        if (dialogVisible && upgradeState !is UpgradeCheckState.Upgrading) {
            runCheck()
        }
    }

    AppFilledButton(
        onClick = { dialogVisible = true },
        text = stringResource(id = R.string.after_sale_version_upgrade),
        modifier = Modifier.fillMaxWidth(),
    )

    NanoDialog(
        visible = dialogVisible,
        title = stringResource(id = R.string.sys_fun_api_upgrade_title),
        onClose = { dialogVisible = false },
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow(
                    label = stringResource(id = R.string.sys_fun_api_upgrade_local_version),
                    value = BuildConfig.VERSION_NAME.ifEmpty { "—" },
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        AppFilledButton(
            onClick = {
                if (upgradeState == UpgradeCheckState.Checking) return@AppFilledButton
                scope.launch { runCheck() }
            },
            text = stringResource(id = R.string.sys_fun_api_upgrade_btn),
            modifier = Modifier.fillMaxWidth(),
        )
        if (upgradeState != UpgradeCheckState.Idle) {
            Spacer(Modifier.height(12.dp))
            UpgradeResultPanel(
                state = upgradeState,
                onUpgrade = { url, version ->
                    upgradeState = UpgradeCheckState.Upgrading("开始下载新版本…")
                    upgradeVm.onUpgradeFromUrl(url, version)
                },
            )
        }
    }
}

/**
 * 测试连接：按钮 + 弹窗（Nano 接口连通性探测）
 *
 * @param deviceCode 页面上展示的设备编号，弹窗内设备号与其保持一致
 */
@Composable
fun SysFunInfoProbeBlock(deviceCode: String) {
    val scope = rememberCoroutineScope()
    var dialogVisible by remember { mutableStateOf(false) }
    var config by remember { mutableStateOf(ConfigSysBean.Empty) }
    var state by remember { mutableStateOf<TestState>(TestState.Idle) }
    val nanoEnvironment by AppParams.runtimeModeState.nanoEnvironment.collectAsState()

    // 每次打开弹窗重新读取配置，保证机器号等信息为最新值
    LaunchedEffect(dialogVisible) {
        config = SysConfigService.findBean(ConfigSysBean.PREFIX, ConfigSysBean::class)
    }

    AppFilledButton(
        onClick = { dialogVisible = true },
        text = stringResource(id = R.string.sys_fun_api_test_btn),
        modifier = Modifier.fillMaxWidth(),
    )

    NanoDialog(
        visible = dialogVisible,
        title = stringResource(id = R.string.sys_fun_api_test_tab_probe),
        onClose = { dialogVisible = false },
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            color = Color.White,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoRow(
                    label = stringResource(id = R.string.sys_fun_api_test_url),
                    value = nanoEnvironment.baseUrl,
                )
                Spacer(Modifier.height(8.dp))
                InfoRow(
                    label = stringResource(id = R.string.sys_fun_api_test_device_id),
                    value = deviceCode.ifEmpty { "—" },
                )
                Spacer(Modifier.height(8.dp))
                InfoRow(
                    label = stringResource(id = R.string.sys_fun_api_test_flow),
                    value = ConfigSysBean.defaultFlow(config.flow),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        AppFilledButton(
            onClick = {
                if (state == TestState.Running) return@AppFilledButton
                state = TestState.Running
                scope.launch {
                    val result = NanoApi.probe()
                    state = TestState.Done(result)
                }
            },
            text = stringResource(id = R.string.sys_fun_api_test_btn),
            modifier = Modifier.fillMaxWidth(),
        )
        if (state != TestState.Idle) {
            Spacer(Modifier.height(12.dp))
            ResultPanel(state)
        }
    }
}

/**
 * 弹窗骨架：标题 + 可滚动内容 + 关闭按钮
 */
@Composable
private fun NanoDialog(
    visible: Boolean,
    title: String,
    onClose: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (!visible) {
        return
    }
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .wakeScreenOnTouch()
                .width(480.dp)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(8.dp),
            color = bgColor,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = fontColor,
                )
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    content()
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    AppOutlinedButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(36.dp),
                        onClick = onClose,
                        text = stringResource(id = R.string.btn_label_close),
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(label, color = Color(0xFF6B7280), fontSize = 12.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = Color(0xFF111827), fontSize = 14.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun ResultPanel(state: TestState) {
    when (state) {
        TestState.Idle -> {}
        TestState.Running -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(12.dp))
                Text(stringResource(id = R.string.sys_fun_api_test_running), fontSize = 14.sp)
            }
        }
        is TestState.Done -> {
            val r = state.result
            val accent = if (r.ok) Color(0xFF10B981) else Color(0xFFEF4444)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(accent, CircleShape)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = if (r.ok)
                                stringResource(id = R.string.sys_fun_api_test_ok)
                            else
                                stringResource(id = R.string.sys_fun_api_test_failed),
                            color = accent,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    if (r.url.isNotEmpty()) {
                        InfoRow(label = stringResource(id = R.string.sys_fun_api_test_endpoint), value = r.url)
                        Spacer(Modifier.height(8.dp))
                    }
                    if (r.status != null) {
                        InfoRow(label = stringResource(id = R.string.sys_fun_api_test_http), value = r.status.toString())
                        Spacer(Modifier.height(8.dp))
                    }
                    InfoRow(
                        label = stringResource(id = R.string.sys_fun_api_test_latency),
                        value = "${r.latencyMs} ms",
                    )
                    if (!r.error.isNullOrEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        InfoRow(label = stringResource(id = R.string.sys_fun_api_test_error), value = r.error)
                    }
                    if (!r.body.isNullOrEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(id = R.string.sys_fun_api_test_body),
                            color = Color(0xFF6B7280), fontSize = 12.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFF3F4F6),
                        ) {
                            Text(
                                text = r.body,
                                modifier = Modifier.padding(8.dp),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Color(0xFF374151),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpgradeResultPanel(
    state: UpgradeCheckState,
    onUpgrade: (url: String, version: String) -> Unit,
) {
    when (state) {
        UpgradeCheckState.Idle -> {}
        UpgradeCheckState.Checking -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
                Spacer(Modifier.size(12.dp))
                Text(stringResource(id = R.string.sys_fun_api_upgrade_checking), fontSize = 14.sp)
            }
        }
        is UpgradeCheckState.Upgrading -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.size(10.dp))
                        Text(state.msg, fontSize = 14.sp, color = Color(0xFF374151))
                    }
                    if (state.progress != null) {
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = state.progress,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFF6375EC),
                            trackColor = Color(0xFFE5E7EB),
                        )
                    }
                }
            }
        }
        UpgradeCheckState.UpToDate -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(12.dp).background(Color(0xFF10B981), CircleShape))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(id = R.string.sys_fun_api_upgrade_latest),
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                }
            }
        }
        is UpgradeCheckState.Available -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(12.dp).background(Color(0xFFF59E0B), CircleShape))
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = "${stringResource(id = R.string.sys_fun_api_upgrade_available)}: ${state.version}",
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    AppFilledButton(
                        onClick = { onUpgrade(state.url, state.version) },
                        text = stringResource(id = R.string.sys_fun_api_upgrade_go),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        is UpgradeCheckState.Error -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color.White,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(modifier = Modifier.size(12.dp).background(Color(0xFFEF4444), CircleShape))
                    Spacer(Modifier.size(8.dp))
                    Column {
                        Text(
                            text = stringResource(id = R.string.sys_fun_api_upgrade_error),
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                        if (state.msg.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(state.msg, color = Color(0xFF6B7280), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
