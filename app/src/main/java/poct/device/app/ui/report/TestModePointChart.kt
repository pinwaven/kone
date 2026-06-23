package poct.device.app.ui.report

import android.content.pm.ActivityInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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
import com.patrykandpatrick.vico.core.marker.MarkerLabelFormatter
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.RouteConfig
import poct.device.app.chart.rememberMarker
import poct.device.app.chart.rememberTestModePointChartStyle
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewWrapper
import poct.device.app.state.ViewState
import poct.device.app.theme.bgColor
import poct.device.app.theme.filledFontColor
import poct.device.app.theme.fontColor
import java.util.Locale

@Composable
fun TestModePointChart(
        navController: NavController,
        viewModel: TestModePointChartViewModel = viewModel(),
) {
    val viewState by viewModel.viewState.collectAsState()
    val curBean by viewModel.bean.collectAsState()
    val slopeRegions by viewModel.slopeRegions.collectAsState()

    DisposableEffect(key1 = Unit) {
        AppParams.curActivity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        onDispose {}
    }

    LaunchedEffect(viewState) {
        if (viewState == ViewState.Default) {
            viewModel.onLoad()
        }
    }

    AppViewWrapper(viewState = viewState, onErrorClick = { navController.popBackStack() }) {
        AppScaffold(
                topBar = {
                    AppTopBar(
                            navController = navController,
                            title = stringResource(id = R.string.report_detail).format(curBean.caseId),
                            homeEnabled = true,
                            detailEnable = ReportDetailViewModel.STEP_SHOW,
                            onHome = {
                                viewModel.onHome {
                                    AppParams.curActivity?.requestedOrientation =
                                            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                    navController.navigate(RouteConfig.HOME_MAIN) {
                                        popUpTo(RouteConfig.HOME_MAIN) {
                                            inclusive = true
                                        }
                                    }
                                }
                            },
                    )
                }
        ) {
            Surface(
                    modifier =
                            Modifier.fillMaxSize()
                                    .fillMaxHeight()
                                    .fillMaxWidth()
                                    .padding(15.dp),
                    color = bgColor,
                    shape = RoundedCornerShape(4.dp)
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
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (slopeRegions.isNotEmpty()) {
                            Row(
                                    modifier =
                                            Modifier.align(Alignment.TopCenter)
                                                    .padding(top = 8.dp)
                                                    .background(
                                                            color = bgColor.copy(alpha = 0.86f),
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
                                modifier =
                                        Modifier.fillMaxSize()
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                chart = lineChart(spacing = 1.dp),
                                isZoomEnabled = false,
                                chartScrollSpec = rememberChartScrollSpec(isScrollEnabled = false),
                                chartModelProducer = viewModel.chartModelProducer,
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
        }
    }
}
