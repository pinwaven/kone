package poct.device.app.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import poct.device.app.theme.axisColor
import poct.device.app.utils.app.format


@Composable
fun WeLineChart(
    dataSources: List<LineChartData>,
    scrollable: Boolean = false,
    height: Dp = 300.dp,
    color: Color = MaterialTheme.colorScheme.primary.copy(0.8f),
    animationSpec: AnimationSpec<Float> = tween(durationMillis = 800),
    formatter: (Float) -> String = { it.format() }
) {
    val textMeasurer = rememberTextMeasurer()
    val labelColor = MaterialTheme.colorScheme.onSecondary
    val containerColor = MaterialTheme.colorScheme.onBackground

    // 计算所有数据源中的最大值
    val maxValue = remember(dataSources) {
        dataSources.flatMap { it.points }.maxOfOrNull { it.value } ?: 1f
    }

    val valueList = ArrayList<String>()
    val stepValue = maxValue / 8
    for (i in 0..8) {
        valueList.add(String.format("%.2f", i * stepValue))
    }
    val labelList = dataSources.firstOrNull()?.points?.map { it.label } ?: emptyList()

    // 检查是否存在横向滚动条
    val modifier = if (scrollable) {
        if (dataSources.isNotEmpty() && dataSources[0].points.isNotEmpty()) {
            Modifier.width((LocalConfiguration.current.screenWidthDp / 16 * dataSources[0].points.size).dp)
        } else {
            Modifier
        }
    }else{
        Modifier
    }


    // 为每个数据点创建动画实例
    val animatedValuesList = remember(dataSources.size) {
        dataSources.map { dataSource ->
            dataSource.points.map { Animatable(0f) }
        }
    }

    // 数据变化后执行动画
    LaunchedEffect(dataSources) {
        animatedValuesList.forEachIndexed { dataSourceIndex, animatedValues ->
            animatedValues.forEachIndexed { index, item ->
                launch {
                    var targetValue = 0f
                    if(maxValue != 0f) {
                        targetValue = dataSources[dataSourceIndex].points[index].value / maxValue
                    }
                    item.animateTo(
                        targetValue = targetValue,
                        animationSpec = animationSpec
                    )
                }
            }
        }
    }

    Box(
        modifier = if (scrollable) {
            Modifier.horizontalScroll(rememberScrollState())
        } else {
            Modifier
        }
    ) {
        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = 10.dp)
                .height(height)
        ) {
            // 绘制Y轴
            drawYAxis(
                yValues = valueList,
                axisColor = axisColor,
                labelColor = Color.Black,
                textMeasurer
            )
            // 绘制X轴
            drawXAxis(
                labels = labelList,
                size.width / labelList.size,
                0f,
                axisColor = axisColor,
                labelColor = Color.Black,
                textMeasurer
            )

            // 为每个数据源绘制折线和数据点
            dataSources.forEachIndexed { index, dataSource ->
                drawLines(
                    animatedValuesList[index],
                    dataSource.points,
                    lineColor = dataSource.color,
                    containerColor = containerColor,
                    textMeasurer,
                    formatter
                )
            }
        }
    }
}

private fun DrawScope.drawLines(
    animatedValues: List<Animatable<Float, AnimationVector1D>>,
    dataSource: List<ChartData>,
    lineColor: Color,
    containerColor: Color,
    textMeasurer: TextMeasurer,
    formatter: (Float) -> String
) {
    val pointWidth = 1.5.dp.toPx()
    val pointSpace = (size.width - pointWidth * dataSource.size) / dataSource.size

    animatedValues.draw(pointWidth, pointSpace, size.height) { currentPoint, previousPoint, index ->
        // 绘制数值标签
        if (pointSpace >= 10.dp.toPx()) {
            drawValueLabel(
                value = dataSource[index].value,
                currentPoint.x,
                currentPoint.y,
                textMeasurer,
                formatter,
                lineColor
            )
        }
        // 连接数据点
        previousPoint?.let {
            drawLine(
                color = lineColor,
                start = it,
                end = currentPoint,
                strokeWidth = pointWidth
            )
        }
    }

    // 绘制数据点
//    animatedValues.draw(pointWidth, pointSpace, size.height) { currentPoint, _, _ ->
//        drawCircle(color = lineColor, radius = 3.dp.toPx(), center = currentPoint)
//        drawCircle(color = containerColor, radius = 1.5.dp.toPx(), center = currentPoint)
//    }
}

private fun List<Animatable<Float, AnimationVector1D>>.draw(
    pointWidth: Float,
    pointSpace: Float,
    height: Float,
    block: (currentPoint: Offset, previousPoint: Offset?, index: Int) -> Unit
) {
    var previousPoint: Offset? = null
    this.forEachIndexed { index, item ->
        val x = index * (pointWidth + pointSpace) + pointSpace / 2
        val y = height - (item.value * height)
        val currentPoint = Offset(x, y)
        block(currentPoint, previousPoint, index)
        previousPoint = currentPoint
    }
}

private fun DrawScope.drawValueLabel(
    value: Float,
    offsetX: Float,
    offsetY: Float,
    textMeasurer: TextMeasurer,
    valueFormatter: (Float) -> String,
    textColor: Color
) {
    val valueText = valueFormatter(value)
    val textLayoutResult = textMeasurer.measure(
        valueText,
        TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold)
    )
    drawText(
        textLayoutResult,
        textColor,
        Offset(
            offsetX - textLayoutResult.size.width / 2,
            offsetY - textLayoutResult.size.height - 5.dp.toPx()
        )
    )
}