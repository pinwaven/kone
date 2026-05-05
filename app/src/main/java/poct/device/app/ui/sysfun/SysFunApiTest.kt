package poct.device.app.ui.sysfun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.bean.ConfigInfoBean
import poct.device.app.bean.ConfigInfoV2Bean
import poct.device.app.bean.ConfigSysBean
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTopBar
import poct.device.app.entity.service.SysConfigService
import poct.device.app.theme.bgColor
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

@Composable
fun SysFunApiTest(navController: NavController) {
    val scope = rememberCoroutineScope()
    val upgradeVm: AfterSaleVersionUpgradeViewModel = viewModel()
    var config by remember { mutableStateOf(ConfigSysBean.Empty) }
    var deviceConfig by remember { mutableStateOf(ConfigInfoV2Bean.Empty) }
    var state by remember { mutableStateOf<TestState>(TestState.Idle) }
    var upgradeState by remember { mutableStateOf<UpgradeCheckState>(UpgradeCheckState.Idle) }

    LaunchedEffect(Unit) {
        config = SysConfigService.findBean(ConfigSysBean.PREFIX, ConfigSysBean::class)
        deviceConfig = SysConfigService.findBean(ConfigInfoBean.PREFIX, ConfigInfoV2Bean::class)
    }

    // Mirror the ViewModel's actionState into our upgradeState so progress/errors show on this page
    LaunchedEffect(upgradeVm) {
        upgradeVm.actionState.collectLatest { action ->
            when (action.event) {
                AfterSaleVersionUpgradeViewModel.EVT_DOWNLOADING -> {
                    val msg = action.msg ?: "下载中…"
                    val pct = Regex("(\\d+)%").find(msg)?.groupValues?.get(1)?.toFloatOrNull()
                    upgradeState = UpgradeCheckState.Upgrading(msg, pct?.div(100f))
                }
                AfterSaleVersionUpgradeViewModel.EVT_INSTALLING  -> upgradeState = UpgradeCheckState.Upgrading(action.msg ?: "安装中…")
                AfterSaleVersionUpgradeViewModel.EVT_ERROR       -> upgradeState = UpgradeCheckState.Error(action.msg ?: "升级失败")
                AfterSaleVersionUpgradeViewModel.EVT_DOWNLOAD_FAILED -> upgradeState = UpgradeCheckState.Error(action.msg ?: "下载失败")
            }
        }
    }

    AppScaffold(
        topBar = {
            AppTopBar(
                navController = navController,
                title = stringResource(id = R.string.sys_fun_api_test_title),
                backEnabled = true,
            )
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InfoRow(
                            label = stringResource(id = R.string.sys_fun_api_test_url),
                            value = AppParams.NANO_BASE_URL,
                        )
                        Spacer(Modifier.height(8.dp))
                        InfoRow(
                            label = stringResource(id = R.string.sys_fun_api_test_device_id),
                            value = config.nanoDeviceId.ifEmpty { "—" },
                        )
                        Spacer(Modifier.height(8.dp))
                        InfoRow(
                            label = stringResource(id = R.string.sys_fun_api_test_flow),
                            value = config.flow.ifEmpty { "clinical" },
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

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

                Spacer(Modifier.height(20.dp))

                ResultPanel(state)

                Spacer(Modifier.height(28.dp))
                Divider(color = Color(0xFFE5E7EB))
                Spacer(Modifier.height(20.dp))

                Text(
                    text = stringResource(id = R.string.sys_fun_api_upgrade_title),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF111827),
                )

                Spacer(Modifier.height(12.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        InfoRow(
                            label = stringResource(id = R.string.sys_fun_api_upgrade_local_version),
                            value = deviceConfig.software.ifEmpty { "—" },
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                AppFilledButton(
                    onClick = {
                        if (upgradeState == UpgradeCheckState.Checking) return@AppFilledButton
                        upgradeState = UpgradeCheckState.Checking
                        scope.launch {
                            val resp = NanoApi.checkUpgrade()
                            upgradeState = when {
                                resp == null -> UpgradeCheckState.Error("无法连接 Nano 升级接口")
                                resp.version.isNullOrEmpty() || resp.url.isNullOrEmpty() ->
                                    UpgradeCheckState.Error("Nano 暂无可用版本")
                                VersionUtils.isLessThan(deviceConfig.software, resp.version) ->
                                    UpgradeCheckState.Available(resp.version, resp.url)
                                else -> UpgradeCheckState.UpToDate
                            }
                        }
                    },
                    text = stringResource(id = R.string.sys_fun_api_upgrade_btn),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(20.dp))

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
                        Text(stringResource(id = R.string.sys_fun_api_test_body),
                             color = Color(0xFF6B7280), fontSize = 12.sp)
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
