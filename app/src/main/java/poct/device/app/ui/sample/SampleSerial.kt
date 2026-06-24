package poct.device.app.ui.sample

import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.scroll.rememberChartScrollSpec
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.chart.layout.HorizontalLayout
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import com.patrykandpatrick.vico.core.marker.MarkerLabelFormatter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.RouteConfig
import poct.device.app.chart.rememberMarker
import poct.device.app.chart.rememberTestModePointChartStyle
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTextField
import poct.device.app.entity.CasePoint
import poct.device.app.serial.v2.ctl.CtlCommandsV2
import poct.device.app.serial.v2.ctl.CtlConstantsV2
import poct.device.app.theme.filledFontColor
import poct.device.app.theme.fontColor
import poct.device.app.theme.inputFontColor
import poct.device.app.thirdparty.NanoApi
import poct.device.app.thirdparty.model.nano.NanoAuthSupport
import poct.device.app.ui.report.TestModePointChartData
import poct.device.app.ui.report.TestModeSlopeRegion
import poct.device.app.utils.app.AppCardUtils
import poct.device.app.utils.app.AppSystemUtils
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale

/**
 * 页面定义
 */
@Composable
fun SampleSerial(
    navController: NavController,
    viewModel: SampleSerialViewModel = viewModel(),
) {
    val specialTestButtonColor = Color(0xFF92D8C8)
    val text by viewModel.text.collectAsState()
    val laserPower by viewModel.laserPower.collectAsState()
    val oneKeySteps by viewModel.oneKeyTestSteps.collectAsState()
    val oneKeyRunning by viewModel.oneKeyTestRunning.collectAsState()
    val oneKeyAwaitingConfirm by viewModel.oneKeyTestAwaitingConfirm.collectAsState()
    val oneKeyMessage by viewModel.oneKeyTestMessage.collectAsState()
    val oneKeyChartVisible by viewModel.oneKeyTestChartVisible.collectAsState()
    val oneKeySlopeRegions by viewModel.oneKeyTestSlopeRegions.collectAsState()
    val screwTestRunning by viewModel.screwTestRunning.collectAsState()
    val screwTestMessage by viewModel.screwTestMessage.collectAsState()
    var laserDialogVisible by remember { mutableStateOf(false) }
    var screwDialogVisible by remember { mutableStateOf(false) }
    var oneKeyDialogVisible by remember { mutableStateOf(false) }
    AppScaffold(
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
            ) {
                Text(
                    modifier = Modifier.fillMaxSize(),
                    fontSize = 13.sp,
                    color = inputFontColor,
                    text = text
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(
                    space = 48.dp,
                    alignment = Alignment.CenterHorizontally
                ),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FactoryTestButton(
                        text = "返回",
                        onClick = { navController.navigate(RouteConfig.SETTING_MAIN) }
                    )
                    FactoryTestButton(text = "开启供电", onClick = { viewModel.powerOn() })
                    FactoryTestButton(
                        text = "串口重连",
                        onClick = { viewModel.reconnect() }
                    )
                    FactoryTestButton(
                        text = "获取SN",
                        onClick = { viewModel.getDeviceId() }
                    )
                    FactoryTestButton(
                        text = "握手",
                        onClick = { viewModel.handshake() }
                    )
                    FactoryTestButton(
                        text = "gpio_read",
                        onClick = { viewModel.gpioRead() }
                    )
                    FactoryTestButton(
                        text = "片仓复位",
                        onClick = { viewModel.sendResetCase() }
                    )
                    FactoryTestButton(
                        text = "片仓移出",
                        onClick = { viewModel.moveOut() }
                    )
                    FactoryTestButton(
                        text = "丝杆调试",
                        containerColor = specialTestButtonColor,
                        onClick = { screwDialogVisible = true }
                    )
                    FactoryTestButton(
                        text = "激光测试",
                        containerColor = specialTestButtonColor,
                        onClick = { laserDialogVisible = true }
                    )
                    FactoryTestButton(
                        text = "退出应用",
                        onClick = { viewModel.exit() }
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FactoryTestButton(
                        text = "首页",
                        onClick = { navController.navigate(RouteConfig.HOME_MAIN) }
                    )
                    FactoryTestButton(text = "关闭供电", onClick = { viewModel.powerOff() })
                    FactoryTestButton(
                        text = "取消指令",
                        containerColor = specialTestButtonColor,
                        onClick = { viewModel.cancel() }
                    )
                    FactoryTestButton(
                        text = stringResource(id = R.string.nano_auth_activate_button),
                        containerColor = specialTestButtonColor,
                        onClick = { viewModel.activateDevice() }
                    )
                    FactoryTestButton(
                        text = "扫描二维码",
                        containerColor = specialTestButtonColor,
                        onClick = { viewModel.scanQRCode() }
                    )
                    FactoryTestButton(
                        text = "poll",
                        containerColor = specialTestButtonColor,
                        onClick = { viewModel.poll() }
                    )
                    FactoryTestButton(text = "吸水240秒", onClick = { viewModel.absorb() })
                    FactoryTestButton(text = "扫描试剂卡", onClick = { viewModel.scanCard() })
                    FactoryTestButton(text = "扫描结果写入文件", onClick = { viewModel.readData() })
                    FactoryTestButton(
                        text = "快速扫描",
                        containerColor = specialTestButtonColor,
                        onClick = { viewModel.quickScan() }
                    )
                    FactoryTestButton(
                        text = "一键测试",
                        containerColor = specialTestButtonColor,
                        onClick = {
                            oneKeyDialogVisible = true
                            viewModel.startOneKeyTest()
                        }
                    )
//                    Button(onClick = { viewModel.moveDatabase(navController) }) {
//                        Text(text = "移动数据库")
//                    }
//                    Spacer(modifier = Modifier.height(12.dp))
//                    Button(onClick = { viewModel.sendTest2(navController) }) {
//                        Text(text = "上传测试")
//                    }
//                    Spacer(modifier = Modifier.height(12.dp))
//                    Button(onClick = { viewModel.updatePdf(navController) }) {
//                        Text(text = "更新PDF")
//                    }
                }
            }
        }
    }
    LaserTestDialog(
        visible = laserDialogVisible,
        power = laserPower,
        onPowerChange = { viewModel.updateLaserPower(it) },
        onOpen = { viewModel.openLaser() },
        onClose = { viewModel.closeLaser() },
        onDismiss = {
            laserDialogVisible = false
            viewModel.closeLaser()
        }
    )
    ScrewTestDialog(
        visible = screwDialogVisible,
        running = screwTestRunning,
        message = screwTestMessage,
        onUp = { viewModel.screwMoveUp() },
        onDown = { viewModel.screwMoveDown() },
        onReset = { viewModel.screwReset() },
        onFront = { viewModel.screwMoveFront() },
        onBack = { viewModel.screwMoveBack() },
        onCycle = { viewModel.screwCycleFiveTimes() },
        onCancel = {
            screwDialogVisible = false
            viewModel.cancelScrewTest()
        }
    )
    OneKeyTestDialog(
        visible = oneKeyDialogVisible,
        steps = oneKeySteps,
        running = oneKeyRunning,
        awaitingConfirm = oneKeyAwaitingConfirm,
        message = oneKeyMessage,
        onConfirmChipInserted = { viewModel.confirmChipInserted() },
        onDismiss = {
            oneKeyDialogVisible = false
            viewModel.cancelOneKeyTest()
        }
    )
    OneKeyChartDialog(
        visible = oneKeyChartVisible,
        slopeRegions = oneKeySlopeRegions,
        chartModelProducer = viewModel.oneKeyChartModelProducer,
        onDismiss = { viewModel.dismissOneKeyChart() }
    )
}

@Composable
private fun FactoryTestButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = filledFontColor,
) {
    AppFilledButton(
        modifier = modifier
            .width(120.dp)
            .height(32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Color.White,
        ),
        textColor = Color.White,
        fontSize = 13.sp,
        text = text,
        onClick = onClick
    )
}

@Composable
private fun LaserTestDialog(
    visible: Boolean,
    power: String,
    onPowerChange: (String) -> Unit,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) {
        return
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true)
    ) {
        Surface(
            modifier = Modifier
                .width(300.dp)
                .height(220.dp),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 15.dp, end = 15.dp, top = 24.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = fontColor,
                    text = "激光测试"
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = fontColor,
                    text = "激光强度（-100 到 0）"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppTextField(
                        value = power,
                        focusState = true,
                        borderWidth = 1.dp,
                        placeHolder = "激光强度",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        onValueChange = onPowerChange
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FactoryTestButton(
                        modifier = Modifier.width(120.dp),
                        text = "打开",
                        onClick = onOpen
                    )
                    FactoryTestButton(
                        modifier = Modifier.width(120.dp),
                        text = "关闭",
                        onClick = onClose
                    )
                }
            }
        }
    }
}

@Composable
private fun ScrewTestDialog(
    visible: Boolean,
    running: Boolean,
    message: String,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onReset: () -> Unit,
    onFront: () -> Unit,
    onBack: () -> Unit,
    onCycle: () -> Unit,
    onCancel: () -> Unit,
) {
    if (!visible) {
        return
    }
    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(dismissOnClickOutside = true)
    ) {
        Surface(
            modifier = Modifier
                .width(440.dp)
                .height(340.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = fontColor,
                    text = "丝杆调试"
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SmallScrewTestButton(text = "向上", onClick = onUp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmallScrewTestButton(text = "移出", onClick = onFront)
                        SmallScrewTestButton(text = "复位", onClick = onReset)
                        SmallScrewTestButton(text = "移入", onClick = onBack)
                    }
                    SmallScrewTestButton(text = "向下", onClick = onDown)
                }
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = inputFontColor,
                    text = message.ifBlank { if (running) "执行中..." else "" }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FactoryTestButton(
                        text = "往复5次",
                        onClick = onCycle
                    )
                    FactoryTestButton(
                        text = "取消",
                        onClick = onCancel
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallScrewTestButton(
    text: String,
    onClick: () -> Unit,
) {
    AppFilledButton(
        modifier = Modifier
            .width(60.dp)
            .height(32.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = filledFontColor,
            contentColor = Color.White,
        ),
        textColor = Color.White,
        fontSize = 13.sp,
        text = text,
        onClick = onClick
    )
}

@Composable
private fun OneKeyTestDialog(
    visible: Boolean,
    steps: List<OneKeyTestStep>,
    running: Boolean,
    awaitingConfirm: Boolean,
    message: String,
    onConfirmChipInserted: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) {
        return
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier
                .width(720.dp)
                .height(520.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = fontColor,
                    text = "一键测试"
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(
                        modifier = Modifier.width(210.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        steps.forEach { step ->
                            OneKeyTestStepRow(step)
                        }
                        if (message.isNotBlank()) {
                            Text(
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = 12.sp,
                                color = fontColor,
                                text = message
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        if (awaitingConfirm) {
                            FactoryTestButton(
                                modifier = Modifier.width(160.dp),
                                text = "已插入，继续",
                                onClick = onConfirmChipInserted
                            )
                        }
                        FactoryTestButton(
                            modifier = Modifier.width(160.dp),
                            text = if (running) "终止/关闭" else "关闭",
                            onClick = onDismiss
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OneKeyChartDialog(
    visible: Boolean,
    slopeRegions: List<TestModeSlopeRegion>,
    chartModelProducer: ChartEntryModelProducer,
    onDismiss: () -> Unit,
) {
    if (!visible) {
        return
    }
    DisposableEffect(Unit) {
        val previousOrientation =
            AppParams.curActivity?.requestedOrientation ?: oneKeyChartDialogCloseOrientation()
        AppParams.curActivity?.requestedOrientation = oneKeyChartDialogOpenOrientation()
        onDispose {
            AppParams.curActivity?.requestedOrientation = previousOrientation
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = oneKeyChartDialogUsePlatformDefaultWidth(),
            )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "扫描结果图",
                        color = fontColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    FactoryTestButton(
                        modifier = Modifier.width(90.dp),
                        text = "关闭",
                        onClick = onDismiss
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF8F8F8), RoundedCornerShape(4.dp))
                        .padding(10.dp)
                ) {
                    OneKeyTestResultChart(
                        chartModelProducer = chartModelProducer,
                        slopeRegions = slopeRegions
                    )
                }
            }
        }
    }
}

@Composable
private fun OneKeyTestStepRow(step: OneKeyTestStep) {
    val statusColor = when (step.status) {
        OneKeyStepStatus.Success -> Color(0xFF1B8F3A)
        OneKeyStepStatus.Failure -> Color(0xFFC62828)
        OneKeyStepStatus.Running, OneKeyStepStatus.Waiting -> filledFontColor
        OneKeyStepStatus.Pending -> inputFontColor
    }
    val statusText = when (step.status) {
        OneKeyStepStatus.Success -> "✓"
        OneKeyStepStatus.Failure -> "✕"
        OneKeyStepStatus.Running -> "..."
        OneKeyStepStatus.Waiting -> "!"
        OneKeyStepStatus.Pending -> ""
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
            text = step.label,
            color = fontColor,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun OneKeyTestResultChart(
    chartModelProducer: ChartEntryModelProducer,
    slopeRegions: List<TestModeSlopeRegion>,
) {
    val marker =
        rememberMarker(
            labelFormatter =
                remember {
                    MarkerLabelFormatter { markedEntries, _ ->
                        markedEntries.firstOrNull()?.entry?.let { entry ->
                            "x=${entry.x.toInt()}\ny=${entry.y.toInt()}"
                        } ?: ""
                    }
                },
            labelLineCount = 2,
        )
    val bottomAxisValueFormatter =
        remember {
            AxisValueFormatter<AxisPosition.Horizontal.Bottom> { x, _ ->
                x.toInt().toString()
            }
        }
    val chartColors = remember(slopeRegions) {
        listOf(filledFontColor) +
            slopeRegions.map { Color.Red } +
            slopeRegions.map { Color.Yellow } +
            slopeRegions.map { Color.Blue } +
            slopeRegions.map { Color.Green }
    }
    val pointSeriesStartIndex = remember(slopeRegions) { 1 + slopeRegions.size }
    ProvideChartStyle(
        rememberTestModePointChartStyle(
            lineChartColors = chartColors,
            pointSeriesStartIndex = pointSeriesStartIndex,
            wide = 2f,
            pointSize = 6f,
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 30.dp, vertical = 20.dp)
        ) {
            if (slopeRegions.isNotEmpty()) {
                Row(
                    modifier =
                        Modifier.align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                            .background(
                                color = Color(0xFFF8F8F8).copy(alpha = 0.86f),
                                shape = RoundedCornerShape(4.dp),
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    slopeRegions.chunked(2).filter { it.size == 2 }.forEach { pair ->
                        val ratio = if (pair[1].area != 0.0) pair[0].area / pair[1].area else 0.0
                        Text(
                            text = String.format(
                                Locale.US,
                                "%.2f/%.2f=%.2f",
                                pair[0].area,
                                pair[1].area,
                                ratio,
                            ),
                            color = fontColor,
                            fontSize = 12.sp,
                        )
                    }
                }
            }
            Chart(
                modifier = Modifier.fillMaxSize(),
                chart = lineChart(spacing = 1.dp),
                isZoomEnabled = false,
                chartScrollSpec = rememberChartScrollSpec(isScrollEnabled = false),
                chartModelProducer = chartModelProducer,
                horizontalLayout = HorizontalLayout.FullWidth(),
                startAxis = rememberStartAxis(),
                bottomAxis =
                    rememberBottomAxis(
                        valueFormatter = bottomAxisValueFormatter,
                        itemPlacer =
                            remember {
                                AxisItemPlacer.Horizontal.default(spacing = 20)
                            },
                    ),
                marker = marker,
                runInitialAnimation = false,
            )
        }
    }
}

internal fun oneKeyChartDialogOpenOrientation(): Int = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

internal fun oneKeyChartDialogCloseOrientation(): Int = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

internal fun oneKeyChartDialogUsePlatformDefaultWidth(): Boolean = false

internal data class OneKeyChartData(
    val slopeRegions: List<TestModeSlopeRegion>,
    val entrySets: List<List<FloatEntry>>,
)

internal fun buildOneKeyChartData(points: List<CasePoint>): OneKeyChartData {
    val regions = TestModePointChartData.findSlopeRegions(points)
    return OneKeyChartData(
        slopeRegions = regions,
        entrySets = TestModePointChartData.toEntrySets(points, regions),
    )
}

enum class OneKeyStepStatus {
    Pending,
    Running,
    Waiting,
    Success,
    Failure,
}

data class OneKeyTestStep(
    val label: String,
    val status: OneKeyStepStatus = OneKeyStepStatus.Pending,
)

class SampleSerialViewModel : ViewModel() {
    val text = MutableStateFlow("")
    val laserPower = MutableStateFlow("-100")
    val oneKeyTestSteps = MutableStateFlow(defaultOneKeyTestSteps())
    val oneKeyTestRunning = MutableStateFlow(false)
    val oneKeyTestAwaitingConfirm = MutableStateFlow(false)
    val oneKeyTestMessage = MutableStateFlow("")
    val oneKeyTestChartVisible = MutableStateFlow(false)
    val oneKeyTestSlopeRegions = MutableStateFlow<List<TestModeSlopeRegion>>(emptyList())
    val oneKeyChartModelProducer = ChartEntryModelProducer()
    val screwTestRunning = MutableStateFlow(false)
    val screwTestMessage = MutableStateFlow("")
    private var oneKeyJob: Job? = null
    private var screwTestJob: Job? = null
    private var chipConfirmDeferred: CompletableDeferred<Unit>? = null

    fun getDeviceId() {
        val sn: String = App.getDeviceId()
        text.value = ("获取SN success: $sn")
    }

    fun updateLaserPower(value: String) {
        laserPower.value = value.toLaserPowerInput()
    }

    fun openLaser() {
        viewModelScope.launch {
            val power = normalizedLaserPower()
            laserPower.value = power.toString()
            text.value = ("激光打开中。。。")
            val result = withContext(Dispatchers.IO) {
                val setResult = CtlCommandsV2.readAllData(CtlCommandsV2.setLDPwr(power))
                val openResult = CtlCommandsV2.readAllData(CtlCommandsV2.powerLD(true))
                "setLDPwr: $setResult powerLD(true): $openResult"
            }
            text.value = ("激光打开 success: $result")
        }
    }

    fun closeLaser() {
        viewModelScope.launch {
            text.value = ("激光关闭中。。。")
            val result = withContext(Dispatchers.IO) {
                CtlCommandsV2.readAllData(CtlCommandsV2.powerLD(false))
            }
            text.value = ("激光关闭 success: $result")
        }
    }

    private fun normalizedLaserPower(): Int {
        return laserPower.value.toIntOrNull()?.coerceIn(LASER_POWER_MIN, LASER_POWER_MAX)
            ?: LASER_POWER_MAX
    }

    fun activateDevice() {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                text.value = App.getContext().getString(R.string.nano_auth_activating)
            }
            val mainboardId = App.getDeviceId().trim()
            if (mainboardId.isEmpty()) {
                withContext(Dispatchers.Main) {
                    text.value = App.getContext().getString(R.string.nano_auth_mainboard_id_missing)
                }
                return@launch
            }

            val rawHandshake = CtlCommandsV2.readAllData(CtlCommandsV2.hi())
            val firmwareId = NanoAuthSupport.extractFirmwareId(rawHandshake)
            if (firmwareId.isEmpty()) {
                withContext(Dispatchers.Main) {
                    text.value = App.getContext().getString(
                        R.string.nano_auth_firmware_id_missing_with_raw,
                        rawHandshake
                    )
                }
                return@launch
            }

            val result = NanoApi.activateDevice(
                mainboardId = mainboardId,
                firmwareId = firmwareId,
                model = "KNA1",
            )
            withContext(Dispatchers.Main) {
                text.value = result.message
            }
        }
    }

    fun reconnect() {
        viewModelScope.launch(Dispatchers.IO) {
            App.getSerialHelper().close()
            App.getSerialHelper().open()
            withContext(Dispatchers.Main) {
                text.value = ("串口重连 success")
            }
        }
    }

    fun handshake() {
        viewModelScope.launch {
            text.value = ("握手中。。。")
            val version = withContext(Dispatchers.IO) {
                val hiResult = CtlCommandsV2.readAllData(CtlCommandsV2.hi())
                var version = ""
                if (hiResult.isNotEmpty()) {
                    val versionData = hiResult.split("ver:")
                    if (versionData.size > 1) {
                        version = versionData[1]
                    }
                }
                version
            }
            text.value = ("握手 success: $version")
        }
    }

    fun gpioRead() {
        viewModelScope.launch {
            text.value = ("gpio_read。。。")
            val result = withContext(Dispatchers.IO) {
                CtlCommandsV2.readAllData(CtlCommandsV2.gpioRead())
            }
            text.value =
                ("gpio_read success: $result")
        }
    }

    fun moveIn() {
        viewModelScope.launch {
            text.value = ("片仓移入中。。。")
            withContext(Dispatchers.IO) {
                val moveToSsResult =
                    CtlCommandsV2.readAllData(CtlCommandsV2.moveToSs(0, MOTOR_VELOCITY, MOTOR_DURATION_MS, 0))
                Timber.d("moveToSsResult: $moveToSsResult")

                // 等待成功
                CtlCommandsV2.waitMoveToSsStatusSuccess()
            }
            text.value = ("片仓移入 success")
        }
    }

    fun moveOut() {
        viewModelScope.launch {
            text.value = ("片仓移出中。。。")
            withContext(Dispatchers.IO) {
                val moveToSsResult =
                    CtlCommandsV2.readAllData(CtlCommandsV2.moveToSs(0, -MOTOR_VELOCITY, MOTOR_DURATION_MS, 1))
                Timber.d("moveToSsResult: $moveToSsResult")

                // 等待成功
                CtlCommandsV2.waitMoveToSsStatusSuccess()
            }
            text.value = ("片仓移出 success")
        }
    }

    fun poll() {
        viewModelScope.launch {
            text.value = ("poll。。。")
            val result = withContext(Dispatchers.IO) {
                CtlCommandsV2.readAllData(CtlCommandsV2.poll())
            }
            text.value =
                ("poll success: $result")
        }
    }

    fun absorb() {
        viewModelScope.launch {
            text.value = ("吸水中。。。")
            withContext(Dispatchers.IO) {
                val result = CtlCommandsV2.readAllData(CtlCommandsV2.absorb(240 * 1000))

                withContext(Dispatchers.Main) {
                    text.value =
                        ("吸水 $result")
                }

                CtlCommandsV2.waitAbsorbStatusSuccess()
            }
            text.value =
                ("吸水 success")
        }
    }

    fun scanCard() {
        viewModelScope.launch {
            text.value = ("扫描中。。。")
            withContext(Dispatchers.IO) {
                val getLDPwr =
                    CtlCommandsV2.readAllData(CtlCommandsV2.getLDPwr())

                withContext(Dispatchers.Main) {
                    text.value =
                        ("getLDPwr: $getLDPwr")
                }

                val setLDPwr =
                    CtlCommandsV2.readAllData(CtlCommandsV2.setLDPwr(-10))

                withContext(Dispatchers.Main) {
                    text.value =
                        ("setLDPwr: $setLDPwr")
                }

                val result = CtlCommandsV2.readAllData(CtlCommandsV2.scan(SCAN_VELOCITY, SCAN_DURATION_MS))

                withContext(Dispatchers.Main) {
                    text.value =
                        ("扫描 $result")
                }

                CtlCommandsV2.waitScanStatusSuccess()
            }
            text.value =
                ("扫描 success")
        }
    }

    fun cancel() {
        viewModelScope.launch {
            text.value = ("取消中。。。")
            withContext(Dispatchers.IO) {
                CtlCommandsV2.readAllData(CtlCommandsV2.cancel())
            }
            text.value =
                ("取消 success")
        }
    }

//    fun upgrade() {
//        viewModelScope.launch(Dispatchers.IO) {
//            val filePath: String = AppFileUtils.getHardWareApkPath()
//            val msgList = CtlCommandsV2.upgrade(0, filePath)
//            val total = msgList.size
//            var count = 0
//            var flag = true
//            if (msgList.isEmpty()) {
//                Timber.w("######不存在升级文件#######")
//                return@launch
//            }
//            Timber.w("#######${total}")
//            while (count < total) {
//                if (flag) {
//                    if (count != 0) {
//                        Timber.w("#######step2##${count}")
//                        Timber.w("#######step2##${App.gson.toJson(msgList[count])}")
//                    }
//                    App.getCtlSerialService()
//                        .send(
//                            msgList[count],
//                            object : SerialMessageCallbackAdapterV2<CtlSerialMessageV2>() {
//                                override suspend fun error(
//                                    sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                                    e: Exception?,
//                                    scope: CoroutineScope,
//                                ) {
//                                    // 对当前发送失败的切片进行重发
//                                    flag = true
//                                    Timber.w("#######ERROR${flag}")
//                                    Timber.w("#######ERROR${e?.message}")
//                                }
//
//                                override suspend fun delay(
//                                    feedback: CtlSerialMessageV2,
//                                    sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                                    scope: CoroutineScope,
//                                ) {
//                                    text.value = ("Delay:${feedback.paramData.toQueryString()}")
//                                    Timber.w("#######Delay${flag}")
//                                    Timber.w("#######Delay${feedback.paramData.toQueryString()}")
//                                }
//
//                                override suspend fun success(
//                                    feedback: CtlSerialMessageV2,
//                                    sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                                    scope: CoroutineScope,
//                                ) {
//                                    text.value = ("success:${feedback.paramData.toQueryString()}")
//                                    count++
//                                    Timber.w("#######success${count}")
//                                    Timber.w("#######success${total}")
//                                    flag = true
//                                }
//                            })
//                }
//                flag = false
//                Thread.sleep(300)
//            }
//        }
//    }

    fun readData() {
        viewModelScope.launch {
            text.value = ("读取成功，写入中。。。")

            val path = withContext(Dispatchers.IO) {
                val queryResult = CtlCommandsV2.readAllDataByteArray(CtlCommandsV2.queryData())

                val file = File(App.getContext().externalCacheDir, "data.bin")
                if (!file.parentFile?.exists()!!) {
                    file.parentFile?.mkdirs()
                }
                if (file.exists()) {
                    file.delete()
                }
                FileOutputStream(file).use { outputStream ->
                    outputStream.write(queryResult)
                }
                file.path
            }

            text.value =
                ("写入 success $path")
        }
    }

    fun sendResetCase() {
        viewModelScope.launch {
            text.value = ("仓片复位中。。。")

            withContext(Dispatchers.IO) {
                CtlCommandsV2.readAllData(CtlCommandsV2.homing())
                homingSuccess()
            }
        }
    }

    private fun homingSuccess() {
        CtlCommandsV2.processHomingStatus { homingSuccessCustomFunction(it) }
    }

    private fun homingSuccessCustomFunction(progressVal: Int) {
        viewModelScope.launch {
            if (progressVal < CtlConstantsV2.CMD_ACTION_HOMING_STATUS_COMPLETED) {
                withContext(Dispatchers.IO) {
                    homingSuccess()
                }
            } else {
                withContext(Dispatchers.IO) {
                    val moveToSsResult =
                        CtlCommandsV2.readAllData(CtlCommandsV2.moveToSs(0, -MOTOR_VELOCITY, MOTOR_DURATION_MS, 1))
                    Timber.w("moveToSsResult: $moveToSsResult")

                    // 等待成功
                    CtlCommandsV2.waitMoveToSsStatusSuccess()
                }

                text.value = ("仓片复位 success")
            }
        }
    }

    fun scanQRCode() {
        viewModelScope.launch {
            text.value = ("扫描二维码中。。。")
            val readQRResult = withContext(Dispatchers.IO) {
                CtlCommandsV2.readAllData(CtlCommandsV2.readQR())
            }
            text.value = ("扫描二维码 success $readQRResult")
        }
    }

    suspend fun readQrSuccess() {
        CtlCommandsV2.processReadQRStatus { scanQrSuccessCustomFunction(it) }
    }

    fun scanQrSuccessCustomFunction(qrCodeData: String) {
        viewModelScope.launch {
            if (qrCodeData.isEmpty()) {
                withContext(Dispatchers.IO) {
                    readQrSuccess()
                }
            } else {
                text.value = ("扫描二维码 success: $qrCodeData")
            }
        }
    }

    // TODO aabbcc
    fun upgrade() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // 启动主屏幕来清除所有顶层Activity
        App.getContext().startActivity(intent)
    }

    fun exit() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // 启动主屏幕来清除所有顶层Activity
        App.getContext().startActivity(intent)
    }

    fun powerOff() {
        AppSystemUtils.powerOffCtlBoard()
        text.value = ("关闭供电 success")
    }

    fun powerOn() {
        AppSystemUtils.powerOnCtlBoard()
        text.value = ("开启供电 success")
    }

    fun quickScan() {
        if (oneKeyTestRunning.value) {
            text.value = "一键测试执行中，请稍后"
            return
        }
        oneKeyJob?.cancel()
        chipConfirmDeferred = null
        oneKeyTestMessage.value = "开始快速扫描"
        oneKeyTestChartVisible.value = false
        oneKeyTestSlopeRegions.value = emptyList()
        oneKeyTestRunning.value = true
        oneKeyTestAwaitingConfirm.value = false

        oneKeyJob =
            viewModelScope.launch {
                try {
                    text.value = "快速扫描: 片仓移入中..."
                    moveInForOneKey()
                    text.value = "快速扫描: 扫描芯片中..."
                    val points = scanAndReadPointsForOneKey()
                    val chartData = buildOneKeyChartData(points)
                    oneKeyTestSlopeRegions.value = chartData.slopeRegions
                    oneKeyChartModelProducer.setEntries(chartData.entrySets)
                    oneKeyTestChartVisible.value = true
                    text.value = "快速扫描: 扫描结果图已显示"
                    //ejectCaseForOneKeyChart()
                    oneKeyTestMessage.value = "快速扫描完成"
                    text.value = "快速扫描完成"
                } catch (e: CancellationException) {
                    oneKeyTestMessage.value = "快速扫描已终止"
                    text.value = "快速扫描已终止"
                } catch (e: Exception) {
                    val message = e.message ?: "未知错误"
                    oneKeyTestMessage.value = message
                    text.value = "快速扫描失败: $message"
                } finally {
                    oneKeyTestRunning.value = false
                    oneKeyTestAwaitingConfirm.value = false
                    chipConfirmDeferred = null
                }
            }
    }

    fun startOneKeyTest() {
        if (oneKeyTestRunning.value) {
            return
        }
        oneKeyJob?.cancel()
        chipConfirmDeferred = null
        oneKeyTestSteps.value = defaultOneKeyTestSteps()
        oneKeyTestMessage.value = "开始一键测试"
        oneKeyTestChartVisible.value = false
        oneKeyTestSlopeRegions.value = emptyList()
        oneKeyTestRunning.value = true
        oneKeyTestAwaitingConfirm.value = false

        oneKeyJob =
            viewModelScope.launch {
                try {
                    runOneKeyStep(0) {
                        ensureNoChipInDevice()
                        resetCaseForOneKey()
                    }
                    waitChipInsertedStep()
                    runOneKeyStep(2) { moveInForOneKey() }
                    runOneKeyStep(3) { absorbForOneKey(milliseconds = 10 * 1000) }
                    runOneKeyStep(4) {
                        val points = scanAndReadPointsForOneKey()
                        val chartData = buildOneKeyChartData(points)
                        oneKeyTestSlopeRegions.value = chartData.slopeRegions
                        oneKeyChartModelProducer.setEntries(chartData.entrySets)
                    }
                    runOneKeyStep(5) {
                        oneKeyTestChartVisible.value = true
                        oneKeyTestMessage.value = "扫描结果图已显示，片仓弹出中..."
                        ejectCaseForOneKeyChart()
                    }
                    oneKeyTestMessage.value = "一键测试完成"
                    text.value = "一键测试完成"
                } catch (e: CancellationException) {
                    oneKeyTestMessage.value = "一键测试已终止"
                    text.value = "一键测试已终止"
                } catch (e: Exception) {
                    val message = e.message ?: "未知错误"
                    oneKeyTestMessage.value = message
                    text.value = "一键测试失败: $message"
                } finally {
                    oneKeyTestRunning.value = false
                    oneKeyTestAwaitingConfirm.value = false
                    chipConfirmDeferred = null
                }
            }
    }

    fun confirmChipInserted() {
        chipConfirmDeferred?.complete(Unit)
    }

    fun dismissOneKeyChart() {
        oneKeyTestChartVisible.value = false
    }

    fun cancelOneKeyTest() {
        val wasRunning = oneKeyTestRunning.value
        chipConfirmDeferred?.cancel()
        oneKeyJob?.cancel()
        oneKeyTestAwaitingConfirm.value = false
        oneKeyTestRunning.value = false
        if (wasRunning) {
            viewModelScope.launch(Dispatchers.IO) {
                CtlCommandsV2.readAllData(CtlCommandsV2.cancel())
            }
        }
    }

    fun screwMoveUp() {
        runScrewTestCommand("向上") {
            val result = CtlCommandsV2.readAllData(CtlCommandsV2.moveDuration(1, 60000, 1000))
            Timber.w("screw up result: $result")
            waitPollForScrewTest("向上", 3_000L) {
                it.contains(CtlConstantsV2.CMD_ACTION_MOVE_DURATION_STATUS_COMPLETED)
            }
        }
    }

    fun screwMoveDown() {
        runScrewTestCommand("向下") {
            val result = CtlCommandsV2.readAllData(CtlCommandsV2.moveToSs(1, -50000, 2500, 0))
            Timber.w("screw down result: $result")
            waitPollForScrewTest("向下", 5_000L) {
                it.contains(CtlConstantsV2.CMD_ACTION_MOVE_TO_SS_STATUS_COMPLETED)
            }
        }
    }

    fun screwMoveFront() {
        runScrewTestCommand("移出") {
            moveFrontForScrewTest()
        }
    }

    fun screwMoveBack() {
        runScrewTestCommand("移入") {
            moveBackForScrewTest()
        }
    }

    fun screwReset() {
        runScrewTestCommand("复位") {
            resetCaseForOneKey()
        }
    }

    fun screwCycleFiveTimes() {
        runScrewTestCommand("往复5次") {
            repeat(5) { index ->
                screwTestMessage.value = "往复5次: 第 ${index + 1} 次前进"
                moveFrontForScrewTest()
                screwTestMessage.value = "往复5次: 第 ${index + 1} 次后退"
                moveBackForScrewTest()
            }
        }
    }

    fun cancelScrewTest() {
        val wasRunning = screwTestRunning.value
        screwTestJob?.cancel()
        screwTestRunning.value = false
        screwTestMessage.value = if (wasRunning) "丝杆调试已取消" else ""
        viewModelScope.launch(Dispatchers.IO) {
            CtlCommandsV2.readAllData(CtlCommandsV2.cancel())
        }
    }

    private fun runScrewTestCommand(
        actionName: String,
        block: suspend () -> Unit,
    ) {
        if (screwTestRunning.value) {
            screwTestMessage.value = "正在执行，请稍后"
            return
        }
        screwTestJob?.cancel()
        screwTestJob =
            viewModelScope.launch {
                screwTestRunning.value = true
                screwTestMessage.value = "$actionName ing..."
                text.value = "丝杆调试 $actionName ing..."
                try {
                    withContext(Dispatchers.IO) {
                        block()
                    }
                    screwTestMessage.value = "$actionName 完成"
                    text.value = "丝杆调试 $actionName 完成"
                } catch (e: CancellationException) {
                    screwTestMessage.value = "$actionName 已取消"
                    text.value = "丝杆调试 $actionName 已取消"
                } catch (e: Exception) {
                    val message = e.message ?: "未知错误"
                    screwTestMessage.value = "$actionName 失败: $message"
                    text.value = "丝杆调试 $actionName 失败: $message"
                } finally {
                    screwTestRunning.value = false
                }
            }
    }

    private suspend fun runOneKeyStep(index: Int, block: suspend () -> Unit) {
        updateOneKeyStep(index, OneKeyStepStatus.Running)
        oneKeyTestMessage.value = "${oneKeyTestSteps.value[index].label}中..."
        try {
            block()
            updateOneKeyStep(index, OneKeyStepStatus.Success)
        } catch (e: Exception) {
            updateOneKeyStep(index, OneKeyStepStatus.Failure)
            throw e
        }
    }

    private suspend fun waitChipInsertedStep() {
        val index = 1
        updateOneKeyStep(index, OneKeyStepStatus.Waiting)
        oneKeyTestAwaitingConfirm.value = true
        oneKeyTestMessage.value = "请插入芯片后点击继续"
        chipConfirmDeferred = CompletableDeferred()
        try {
            chipConfirmDeferred?.await()
            updateOneKeyStep(index, OneKeyStepStatus.Success)
            oneKeyTestMessage.value = "已确认插入芯片"
        } catch (e: Exception) {
            updateOneKeyStep(index, OneKeyStepStatus.Failure)
            throw e
        } finally {
            oneKeyTestAwaitingConfirm.value = false
            chipConfirmDeferred = null
        }
    }

    private fun updateOneKeyStep(index: Int, status: OneKeyStepStatus) {
        oneKeyTestSteps.value =
            oneKeyTestSteps.value.mapIndexed { curIndex, step ->
                if (curIndex == index) {
                    step.copy(status = status)
                } else {
                    step
                }
            }
    }

    private suspend fun ensureNoChipInDevice() {
        withContext(Dispatchers.IO) {
            val gpioResult = CtlCommandsV2.readAllData(CtlCommandsV2.gpioRead())
            Timber.w("one key gpioResult: $gpioResult")
            if (gpioResult.isBlank()) {
                throw IOException("检测设备内芯片失败")
            }
            if (CtlCommandsV2.gpioReadHasCard(gpioResult)) {
                throw IOException("设备中检测到芯片，请先取出后重试")
            }
        }
    }

    private suspend fun resetCaseForOneKey() {
        withContext(Dispatchers.IO) {
            val homingResult = CtlCommandsV2.readAllData(CtlCommandsV2.homing())
            Timber.w("one key homingResult: $homingResult")
            waitPollForOneKey("片仓复位", 30_000L) { result ->
                CtlConstantsV2.HOMING_STATUS_MAP.any { (key, value) ->
                    value >= CtlConstantsV2.CMD_ACTION_HOMING_STATUS_COMPLETED &&
                        result.contains("s:$key")
                }
            }
            val moveToSsResult =
                CtlCommandsV2.readAllData(CtlCommandsV2.moveToSs(0, -80000, 10000, 1))
            Timber.w("one key reset moveToSsResult: $moveToSsResult")
            waitPollForOneKey("片仓复位移出", 15_000L) {
                it.contains(CtlConstantsV2.CMD_ACTION_MOVE_TO_SS_STATUS_COMPLETED)
            }
        }
    }

    private fun moveFrontForScrewTest() {
        val result = CtlCommandsV2.readAllData(CtlCommandsV2.moveToSs(0, -80000, 10000, 1))
        Timber.w("screw front result: $result")
        waitPollForScrewTest("移出", 15_000L) {
            it.contains(CtlConstantsV2.CMD_ACTION_MOVE_TO_SS_STATUS_COMPLETED)
        }
    }

    private fun moveBackForScrewTest() {
        val result = CtlCommandsV2.readAllData(CtlCommandsV2.moveToSs(0, 80000, 10000, 0))
        Timber.w("screw back result: $result")
        waitPollForScrewTest("移入", 15_000L) {
            it.contains(CtlConstantsV2.CMD_ACTION_MOVE_TO_SS_STATUS_COMPLETED)
        }
    }

    private fun waitPollForScrewTest(
        actionName: String,
        timeoutMillis: Long,
        isCompleted: (String) -> Boolean,
    ) {
        waitPollForOneKey(actionName, timeoutMillis, isCompleted)
    }

    private suspend fun ejectCaseForOneKeyChart() {
        withContext(Dispatchers.IO) {
            val moveToSsResult =
                CtlCommandsV2.readAllData(CtlCommandsV2.moveToSs(0, -MOTOR_VELOCITY, MOTOR_DURATION_MS, 1))
            Timber.w("one key chart eject moveToSsResult: $moveToSsResult")
            waitPollForOneKey("片仓弹出", 15_000L) {
                it.contains(CtlConstantsV2.CMD_ACTION_MOVE_TO_SS_STATUS_COMPLETED)
            }
        }
    }

    private suspend fun moveInForOneKey() {
        withContext(Dispatchers.IO) {
            val moveToSsResult =
                CtlCommandsV2.readAllData(CtlCommandsV2.moveToSs(0, MOTOR_VELOCITY, MOTOR_DURATION_MS, 0))
            Timber.w("one key moveInResult: $moveToSsResult")
            waitPollForOneKey("片仓移入", 15_000L) {
                it.contains(CtlConstantsV2.CMD_ACTION_MOVE_TO_SS_STATUS_COMPLETED)
            }
        }
    }

    private suspend fun absorbForOneKey(milliseconds: Int) {
        withContext(Dispatchers.IO) {
            val absorbResult = CtlCommandsV2.readAllData(CtlCommandsV2.absorb(milliseconds))
            Timber.w("one key absorbResult: $absorbResult")
            waitPollForOneKey("吸水10秒", milliseconds + 10_000L) {
                it.contains(CtlConstantsV2.CMD_ACTION_ABSORB_STATUS_COMPLETED)
            }
        }
    }

    private suspend fun scanAndReadPointsForOneKey(): List<CasePoint> {
        return withContext(Dispatchers.IO) {
            val getLDPwr = CtlCommandsV2.readAllData(CtlCommandsV2.getLDPwr())
            Timber.w("one key getLDPwr: $getLDPwr")
            val setLDPwr = CtlCommandsV2.readAllData(CtlCommandsV2.setLDPwr(SCAN_LD_PWR))
            Timber.w("one key setLDPwr: $setLDPwr")
            val scanResult = CtlCommandsV2.readAllData(CtlCommandsV2.scan(SCAN_VELOCITY, SCAN_DURATION_MS))
            var needReset = scanResult.contains(SCAN_ERROR_NEED_RESET)
            Timber.w("one key scanResult: $scanResult needReset=$needReset")

            waitPollForOneKey("扫描芯片", 30_000L) { result ->
                if (result.contains(SCAN_ERROR_NEED_RESET)) needReset = true
                result.contains(CtlConstantsV2.CMD_ACTION_SCAN_STATUS_COMPLETED)
            }
            if (needReset) {
                val homingResult = CtlCommandsV2.readAllData(CtlCommandsV2.homing())
                Timber.w("one key homingResult: $homingResult")
                waitPollForOneKey("片仓复位", 30_000L) { result ->
                    CtlConstantsV2.HOMING_STATUS_MAP.any { (key, value) ->
                        value >= CtlConstantsV2.CMD_ACTION_HOMING_STATUS_COMPLETED &&
                                result.contains("s:$key")
                    }
                }
                val moveToSsResult =
                    CtlCommandsV2.readAllData(CtlCommandsV2.moveToSs(0, MOTOR_VELOCITY, MOTOR_DURATION_MS, 0))
                Timber.w("one key moveInResult: $moveToSsResult")
                waitPollForOneKey("片仓移入", 15_000L) {
                    it.contains(CtlConstantsV2.CMD_ACTION_MOVE_TO_SS_STATUS_COMPLETED)
                }

                val rescannedResult = CtlCommandsV2.readAllData(CtlCommandsV2.scan(SCAN_VELOCITY, SCAN_DURATION_MS))
                val rescannedNeedsReset = rescannedResult.contains(SCAN_ERROR_NEED_RESET)
                Timber.w("one key rescanResult: $rescannedResult needReset=$rescannedNeedsReset")
                if (rescannedNeedsReset) {
                    throw IOException("复位后扫描仍然报错: $rescannedResult")
                }
                waitPollForOneKey("扫描芯片(复位后)", 30_000L) {
                    it.contains(CtlConstantsV2.CMD_ACTION_SCAN_STATUS_COMPLETED)
                }
            }

            val queryResult = CtlCommandsV2.readAllDataByteArray(CtlCommandsV2.queryData())
                ?: throw IOException("读取扫描bin失败")
            if (queryResult.isEmpty()) {
                throw IOException("读取扫描bin为空")
            }

            val file = File(App.getContext().externalCacheDir, "data.bin")
            file.parentFile?.takeIf { !it.exists() }?.mkdirs()
            if (file.exists()) {
                file.delete()
            }
            FileOutputStream(file).use { outputStream -> outputStream.write(queryResult) }
            Timber.w("one key bin file: ${file.path}, size: ${queryResult.size}")

            val scanData = AppCardUtils.parseData(queryResult)
                ?: throw IOException("解析扫描bin失败")
            Timber.w("one key rawData size: ${scanData.rawData.size}")
            Timber.w("one key rawData: ${App.gson.toJson(scanData.rawData)}")

            val points = scanData.rawData.mapIndexed { index, value ->
                CasePoint((index + 1).toDouble(), value.toDouble())
            }
            if (points.isEmpty()) {
                throw IOException("扫描bin未解析出曲线点")
            }
            Timber.w("one key chart points size: ${points.size}")
            oneKeyTestMessage.value = "扫描数据读取完成: ${points.size}点 ${file.path}"
            points
        }
    }

    private fun waitPollForOneKey(
        actionName: String,
        timeoutMillis: Long,
        isCompleted: (String) -> Boolean,
    ) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime <= timeoutMillis) {
            val result = CtlCommandsV2.readAllData(CtlCommandsV2.poll())
            Timber.w("one key $actionName pollResult: $result")
            if (result.startsWith(CtlConstantsV2.RESULT_ERROR_PREFIX)) {
                throw IOException("$actionName 失败: $result")
            }
            if (result.startsWith(CtlConstantsV2.RESULT_SUCCESS_PREFIX) && isCompleted(result)) {
                return
            }
            Thread.sleep(CtlCommandsV2.delayMs)
        }
        throw IOException("$actionName 超时")
    }

//    fun moveDatabase(
//        navController: NavController
//    ) {
//        viewModelScope.launch(Dispatchers.IO) {
//            // 移动数据到外部存储
//            val srcDir = File("/data/data/poct.device.app/databases/")
//            val destDir = File(AppFileUtils.getBaseFileDirPath() + "/database/")
//            moveDir(srcDir, destDir)
//            withContext(Dispatchers.Main) {
//                text.value = ("移动数据库完成！！")
//            }
//        }
//    }

    private fun moveDir(srcDir: File, dstDir: File) {
        if (!srcDir.exists() || !srcDir.isDirectory) {
            // 源目录不存在或不是目录
            return
        }
        val files = srcDir.listFiles()
        if (files != null) {
            for (file in files) {
                val newFile = File(dstDir, file.name)
                if (file.isFile) {
                    moveFile(file, newFile)
                } else if (file.isDirectory) {
                    moveDir(file, newFile)
                }
            }
        }
    }

    private fun moveFile(srcFile: File, dstFile: File) {
        try {
            if (!dstFile.exists()) {
                if (!dstFile.createNewFile()) {
                    // 创建目标文件失败
                    return
                }
            }
            val inputStream = FileInputStream(srcFile)
            val inChannel = inputStream.channel
            val outputStream = FileOutputStream(dstFile)
            val outChannel = outputStream.channel
            try {
                // 将源文件内容传输到目标文件
                inChannel!!.transferTo(0, inChannel.size(), outChannel)
            } finally {
                inChannel?.close()
                outChannel?.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

//    fun sendTest(time: Long) {
//        viewModelScope.launch(Dispatchers.IO) {
//            withContext(Dispatchers.Main) {
//                text.value =
//                    ("指令长度：" + CtlCommandsV2.testCmdRequest(1, 159).toByteBuf().writerIndex())
//            }
//            for (i in 0..9999) {
//                withContext(Dispatchers.Main) {
//                    text.value = ("发送次数：" + i)
//                }
//                App.getCtlSerialService().send(
//                    CtlCommandsV2.testCmdRequest(i, 159),
//                    object : SerialMessageCallbackAdapterV2<CtlSerialMessageV2>() {
//                        override suspend fun delay(
//                            feedback: CtlSerialMessageV2,
//                            sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                            scope: CoroutineScope,
//                        ) {
//                            text.value = ("Delay:${feedback.paramData.toQueryString()}")
//                        }
//
//                        override suspend fun success(
//                            feedback: CtlSerialMessageV2,
//                            sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                            scope: CoroutineScope,
//                        ) {
//                            text.value = ("success:${feedback.paramData.toQueryString()}")
//                        }
//                    })
//                // 50ms发送一次
//                Thread.sleep(time)
//            }
//        }
//    }

//    fun sendTest2(
//        navController: NavController
//    ) {
//        viewModelScope.launch(Dispatchers.IO) {
//            App.getCtlSerialService().send(
//                CtlCommandsV2.testCmdRequest(),
//                object : SerialMessageCallbackAdapterV2<CtlSerialMessageV2>() {
//                    override suspend fun delay(
//                        feedback: CtlSerialMessageV2,
//                        sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                        scope: CoroutineScope,
//                    ) {
//                        text.value = ("Delay:${feedback.paramData.toQueryString()}")
//                    }
//
//                    override suspend fun success(
//                        feedback: CtlSerialMessageV2,
//                        sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                        scope: CoroutineScope,
//                    ) {
//                        text.value = ("success:${feedback.paramData.toQueryString()}")
//                    }
//                })
//        }
//    }

//    fun updatePdf(
//        navController: NavController
//    ) {
//        var templateData = PdfTemplateDataSample()
//        templateData.applyDate = "2024年11月11日"
//        templateData.type = "AID"
//        templateData.femaleName = "女方姓名"
//        templateData.maleName = "男方姓名"
//        templateData.sampleCode = "样本编码"
//        templateData.sampleNo = "样本编号"
//        templateData.result = "匹配通过"
//        templateData.submitDate = "2024年11月19日"
//        templateData.submitter = "某某医生"
//        templateData.remark = "建议复查XXXXXXXXX"
//
//        val dataDir =
//            File(Environment.getExternalStorageDirectory().absolutePath + File.separator + "Android" + File.separator + "Nanovate_AI" + File.separator + "1_data_iot")
//        dataDir.mkdirs()
//        val output = File(dataDir.absolutePath + "/card_test.pdf")
//        Timber.w("当前开始转换PDF")
//        PdfUtils.replaceText(templateData, output)
//    }
}

private const val LASER_POWER_MIN = -100
private const val LASER_POWER_MAX = 0

private const val SCAN_LD_PWR = -25
private const val SCAN_VELOCITY = -16000
private const val SCAN_DURATION_MS = 14000
private const val MOTOR_VELOCITY = 88888
private const val MOTOR_DURATION_MS = 10000
private const val SCAN_ERROR_NEED_RESET = "!|scan:-3"

private fun defaultOneKeyTestSteps(): List<OneKeyTestStep> =
    listOf(
        OneKeyTestStep("片仓复位"),
        OneKeyTestStep("插入芯片"),
        OneKeyTestStep("片仓移入"),
        OneKeyTestStep("吸水10秒"),
        OneKeyTestStep("扫描芯片"),
        OneKeyTestStep("显示扫描结果图"),
    )

private fun String.toLaserPowerInput(): String {
    val filtered = buildString {
        this@toLaserPowerInput.trim().forEachIndexed { index, char ->
            if (char.isDigit() || (char == '-' && index == 0)) {
                append(char)
            }
        }
    }
    if (filtered == "-") {
        return filtered
    }
    return filtered.toIntOrNull()
        ?.coerceIn(LASER_POWER_MIN, LASER_POWER_MAX)
        ?.toString()
        ?: ""
}

@Preview
@Composable
fun SampleSerialPreview() {
    val navController = rememberNavController()
    val viewModel: SampleSerialViewModel = viewModel()
    LaunchedEffect(key1 = Unit) {
        viewModel.text.value =
            "afwefawfawafwefawfawafwefawfawafwefawfawafwefawfawafwefawfawafwefawfawafwefawfawafwefawfawafwefawfaw"
    }
    AppPreviewWrapper {

        SampleSerial(navController, viewModel)
    }
}
