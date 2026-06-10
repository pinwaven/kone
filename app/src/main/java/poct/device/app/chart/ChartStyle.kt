/*
 * Copyright 2023 by Patryk Goworowski and Patrick Michalik.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package poct.device.app.chart

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.component.shapeComponent
import com.patrykandpatrick.vico.compose.component.shape.shader.fromBrush
import com.patrykandpatrick.vico.compose.style.ChartStyle
import com.patrykandpatrick.vico.core.DefaultAlpha
import com.patrykandpatrick.vico.core.DefaultColors
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShaders

@Composable
fun rememberChartStyle(columnChartColors: List<Color>, lineChartColors: List<Color>, wide: Float): ChartStyle {
    val isSystemInDarkTheme = isSystemInDarkTheme()
    return remember(columnChartColors, lineChartColors, isSystemInDarkTheme) {
        val defaultColors = if (isSystemInDarkTheme) DefaultColors.Dark else DefaultColors.Light
        ChartStyle(
            ChartStyle.Axis(
                axisLabelColor = Color(defaultColors.axisLabelColor),
                axisGuidelineColor = Color(defaultColors.axisGuidelineColor),
                axisLineColor = Color(defaultColors.axisLineColor),
            ),
            ChartStyle.ColumnChart(
                columnChartColors.map { columnChartColor ->
                    LineComponent(
                        color = columnChartColor.toArgb(),
                        thicknessDp = wide,
                        shape = Shapes.roundedCornerShape(20),
                    )
                },
            ),
            ChartStyle.LineChart(
                lineChartColors.map { lineChartColor ->
                    LineChart.LineSpec(
                        lineColor = lineChartColor.toArgb(),
                        lineBackgroundShader = DynamicShaders.fromBrush(
                            Brush.verticalGradient(
                                listOf(
                                    lineChartColor.copy(DefaultAlpha.LINE_BACKGROUND_SHADER_START),
                                    lineChartColor.copy(DefaultAlpha.LINE_BACKGROUND_SHADER_END),
                                ),
                            ),
                        ),
                    )
                },
            ),
            ChartStyle.Marker(indicatorSize= 16.dp),
            Color(defaultColors.elevationOverlayColor),
        )
    }
}

@Composable
fun rememberChartStyle(chartColors: List<Color>, wide: Float) =
    rememberChartStyle(columnChartColors = chartColors, lineChartColors = chartColors, wide)

@Composable
fun rememberTestModePointChartStyle(
    lineChartColors: List<Color>,
    pointSeriesStartIndex: Int,
    wide: Float,
): ChartStyle {
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val pointComponents =
        lineChartColors.map { color ->
            shapeComponent(Shapes.pillShape, color)
        }
    return remember(lineChartColors, pointSeriesStartIndex, wide, isSystemInDarkTheme, pointComponents) {
        val defaultColors = if (isSystemInDarkTheme) DefaultColors.Dark else DefaultColors.Light
        ChartStyle(
            ChartStyle.Axis(
                axisLabelColor = Color(defaultColors.axisLabelColor),
                axisGuidelineColor = Color(defaultColors.axisGuidelineColor),
                axisLineColor = Color(defaultColors.axisLineColor),
            ),
            ChartStyle.ColumnChart(
                lineChartColors.map { columnChartColor ->
                    LineComponent(
                        color = columnChartColor.toArgb(),
                        thicknessDp = wide,
                        shape = Shapes.roundedCornerShape(20),
                    )
                },
            ),
            ChartStyle.LineChart(
                lineChartColors.mapIndexed { index, lineChartColor ->
                    val isPointSeries = index >= pointSeriesStartIndex
                    LineChart.LineSpec(
                        lineColor = lineChartColor.toArgb(),
                        lineThicknessDp = if (isPointSeries) 0f else wide,
                        lineBackgroundShader =
                            if (isPointSeries) {
                                null
                            } else {
                                DynamicShaders.fromBrush(
                                    Brush.verticalGradient(
                                        listOf(
                                            lineChartColor.copy(DefaultAlpha.LINE_BACKGROUND_SHADER_START),
                                            lineChartColor.copy(DefaultAlpha.LINE_BACKGROUND_SHADER_END),
                                        ),
                                    ),
                                )
                            },
                        point = if (isPointSeries) pointComponents[index] else null,
                        pointSizeDp = if (isPointSeries) 12f else 0f,
                    )
                },
            ),
            ChartStyle.Marker(indicatorSize = 16.dp),
            Color(defaultColors.elevationOverlayColor),
        )
    }
}
