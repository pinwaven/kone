package poct.device.app.ui.report

import android.content.pm.ActivityInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import cn.hutool.core.util.RandomUtil.randomFloat
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.bean.CaseBean
import poct.device.app.chart.ChartData
import poct.device.app.chart.rememberChartStyle
import poct.device.app.chart.rememberMarker
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewWrapper
import poct.device.app.state.ViewState
import poct.device.app.theme.bgColor
import poct.device.app.theme.filledFontColor
import timber.log.Timber

/**
 * 页面定义
 */
@Composable
fun ReportDetail(navController: NavController, viewModel: ReportDetailViewModel = viewModel()) {
    val viewState by viewModel.viewState.collectAsState()
    val lineState by viewModel.lineState.collectAsState()
    val curBean by viewModel.bean.collectAsState()
    val labelList by viewModel.valueLabelList.collectAsState()
    val columnList by viewModel.columnDataList.collectAsState()
    val lineList by viewModel.lineDataList.collectAsState()

    Timber.w("当前柱状图数据：${App.gson.toJson(columnList)}")
    DisposableEffect(key1 = Unit) {
        AppParams.curActivity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        onDispose {
//            AppParams.curActivity?.requestedOrientation =
//                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }
    LaunchedEffect(viewState) {
        if (viewState == ViewState.Default) {
            viewModel.onLoad()
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
                    title = stringResource(id = R.string.report_detail).format(curBean.caseId),
                    backEnabled = true,
                    detailEnable = lineState,
                    onBack = {
                        AppParams.curActivity?.requestedOrientation =
                            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                        if (navController.previousBackStackEntry != null) {
                            navController.previousBackStackEntry!!.savedStateHandle["updateFlag"] =
                                "false"
                        }
                        navController.popBackStack()
                    },
                    onDetail = {
                        viewModel.onUpdateLineState()
                    }
                )
            }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .padding(15.dp),
                color = bgColor,
                shape = RoundedCornerShape(4.dp)
            ) {
                if (lineState == ReportDetailViewModel.STEP_MAJOR) {
                    val marker = rememberMarker()
                    ProvideChartStyle(rememberChartStyle(listOf(filledFontColor), 8f)) {
                        Chart(
                            modifier = Modifier
                                .fillMaxSize()
                                .fillMaxWidth()
                                .padding(10.dp),
                            chart = lineChart(spacing = 3f.dp),
                            isZoomEnabled = true,
                            chartModelProducer = viewModel.chartModelProducer,
                            startAxis = rememberStartAxis(),
                            bottomAxis = rememberBottomAxis(
                                guideline = null,
                                itemPlacer = remember {
                                    AxisItemPlacer.Horizontal.default(spacing = 20)
                                },
                            ),
                            marker = marker,
                            runInitialAnimation = false,
                        )
                    }
//                    Column(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(start = 64.dp, top = 20.dp, bottom = 32.dp,  end = 20.dp),
//                        horizontalAlignment = Alignment.CenterHorizontally,
//                        verticalArrangement = Arrangement.spacedBy(20.dp)
//                    ) {
//                        WeLineChart(
//                            dataSources = lineList,
//                            scrollable = true,
//                            color = filledFontColor,
//                        ) {
//                            it.format()
//                        }
//                    }
                } else {
                    val marker = rememberMarker(indicatorFlag = false)
                    val bottomAxisValueFormatter =
                        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { x, _ -> labelList[x.toInt() % labelList.size] }
                    var wide = 4f
                    var markerMap = mapOf(
                        1f to marker,
                        4f to marker,
                        7f to marker,
                        10f to marker,
                        13f to marker
                    )
                    if (curBean.type == CaseBean.TYPE_4LJ) {
                        wide = 8f
                        markerMap = mapOf(
                            1f to marker,
                            4f to marker,
                            7f to marker,
                            10f to marker,
                            13f to marker
                        )
                    } else if (curBean.type == CaseBean.TYPE_IGE) {
                        markerMap = mapOf(4f to marker, 10f to marker)
                    } else if (curBean.type == CaseBean.TYPE_CRP) {
                        markerMap = mapOf(4f to marker, 7f to marker, 13f to marker)
                    } else if (curBean.type == CaseBean.TYPE_SF) {
                        markerMap = mapOf(1f to marker, 4f to marker, 10f to marker, 13f to marker)
                    } else if (curBean.type == CaseBean.TYPE_3LJ) {
                        wide = 8f
                        markerMap = mapOf(
                            1f to marker,
                            4f to marker,
                            7f to marker,
                            10f to marker
                        )
                    } else if (curBean.type == CaseBean.TYPE_2LJ_A) {
                        wide = 8f
                        markerMap = mapOf(
                            1f to marker,
                            4f to marker,
                            7f to marker,
                            10f to marker
                        )
                    } else if (curBean.type == CaseBean.TYPE_2LJ_B) {
                        wide = 8f
                        markerMap = mapOf(
                            1f to marker,
                            4f to marker,
                            7f to marker,
                            10f to marker
                        )
                    }

                    ProvideChartStyle(rememberChartStyle(listOf(filledFontColor), wide)) {
                        Chart(
                            modifier = Modifier
                                .fillMaxSize()
                                .fillMaxWidth()
                                .padding(10.dp),
                            chart = lineChart(
                                persistentMarkers = remember(marker) { markerMap },
                                spacing = 32f.dp,
                            ),
                            isZoomEnabled = false,
                            chartModelProducer = viewModel.chartModelProducer,
                            startAxis = rememberStartAxis(),
                            bottomAxis = rememberBottomAxis(
                                guideline = null,
                                itemPlacer = remember {
                                    AxisItemPlacer.Horizontal.default(
                                        spacing = 3,
                                        offset = 10
                                    )
                                },
                                valueFormatter = bottomAxisValueFormatter
                            ),
                            //marker = marker,
                            runInitialAnimation = true,
                        )
                    }
//                    Column(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .padding(start = 20.dp, top = 32.dp, bottom = 32.dp,  end = 20.dp),
//                        horizontalAlignment = Alignment.CenterHorizontally,
//                        verticalArrangement = Arrangement.spacedBy(20.dp)
//                    ) {
//                        Box(
//                            modifier = Modifier
//                        ) {
//                            WeBarChart(
//                                columnList,
//                                color = filledFontColor,
//                                barWidthRange = 2..30,
//                                modifier = Modifier
//                            )
//                        }
//                    }
                }
            }
        }
    }
}

/**
 * 内容主体
 */
@Composable
fun ReportDetailBody() {
    Image(
        modifier = Modifier
            .width(330.dp)
            .height(462.dp),
        painter = painterResource(id = R.mipmap.fryb_gif),
        contentScale = ContentScale.FillBounds,
        contentDescription = ""
    )
}

@Preview
@Composable
fun ReportDetailPreview(
    navController: NavController = rememberNavController(),
    viewModel: ReportDetailViewModel = viewModel(),
) {
    LaunchedEffect(null) {
        viewModel.viewState.value = ViewState.LoadSuccess()
    }
    AppPreviewWrapper {
        ReportDetail(navController)
    }
}

private fun buildData(size: Int = 6): List<ChartData> {
    return MutableList(size) { index ->
        val value = randomFloat(0f, 10000f)
        ChartData(value = value, label = "${index + 1}月")
    }
}