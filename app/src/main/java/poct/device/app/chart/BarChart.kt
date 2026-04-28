package poct.device.app.chart

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import poct.device.app.App
import poct.device.app.R
import poct.device.app.theme.axisColor
import poct.device.app.theme.fontColor
import poct.device.app.utils.app.format
import timber.log.Timber

@Composable
fun WeBarChart(
    dataSource: List<ChartData>,
    modifier: Modifier = Modifier,
    height: Dp = 300.dp,
    barWidthRange: IntRange = 2..20,
    color: Color = MaterialTheme.colorScheme.primary.copy(0.8f),
    animationSpec: AnimationSpec<Float> = tween(durationMillis = 800),
    formatter: (Float) -> String = { it.format() }
) {
    // 数据最大值
    val maxValue = remember(dataSource) { dataSource.maxOfOrNull { it.value } ?: 1f }

    val textMeasurer = rememberTextMeasurer()
    // 刻度颜色
    val labelColor = MaterialTheme.colorScheme.primary
    // 每个数据项的动画实例
    val animatedHeights = remember(dataSource.size) { dataSource.map { Animatable(0f) } }

    val labelList = dataSource.map { it.label }

    val valueList = ArrayList<String>()
    val stepValue = maxValue / 8
    for (i in 0..8) {
        valueList.add(String.format("%.2f", i * stepValue))
    }


    Timber.w("labelList:${App.gson.toJson(labelList)}")
    // 数据变化后执行动画
    LaunchedEffect(dataSource) {
        animatedHeights.forEachIndexed { index, item ->
            launch {
                var targetValue = 0f
                if(maxValue != 0f) {
                    targetValue = dataSource[index].value / maxValue
                }
                item.animateTo(
                    targetValue = targetValue,
                    animationSpec = animationSpec
                )
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .height(height)
    ) {
        // 确定X轴与Y轴交界原点的位置
        val xInit = maxValue.toString().length
        val yInit = size.height - 20

        // 柱宽
        val barWidth = (size.width / (dataSource.size * 2f)).coerceIn(
            barWidthRange.first.dp.toPx(),
            barWidthRange.last.dp.toPx()
        )
        // 柱间距
        val barSpace = (size.width - barWidth * dataSource.size) / dataSource.size

        // 绘制标题
        val textLayoutResult = textMeasurer.measure(
            text = App.getContext().getString(R.string.report_detail_title),
            TextStyle(fontSize = 13.sp)
        )
        // 绘制标题
        drawText(
            textLayoutResult,
            fontColor,
            topLeft = Offset(
                x = 0f,
                y = -50f
            )
        )
        // 绘制X轴
        drawXAxis(
            labels = labelList,
            barWidth,
            barSpace,
            axisColor = axisColor,
            labelColor = fontColor,
            textMeasurer
        )

        // 绘制Y轴
        drawYAxis(
            yValues = valueList,
            axisColor = axisColor,
            labelColor = fontColor,
            textMeasurer
        )
        // 绘制柱状图
        drawBars(
            animatedHeights,
            dataSource,
            barWidth,
            barSpace,
            barColor = color,
            textMeasurer,
            formatter
        )
    }
}

private fun DrawScope.drawBars(
    animatedHeights: List<Animatable<Float, AnimationVector1D>>,
    dataSource: List<ChartData>,
    barWidth: Float,
    barSpace: Float,
    barColor: Color,
    textMeasurer: TextMeasurer,
    formatter: (Float) -> String
) {
    animatedHeights.forEachIndexed { index, item ->
        val barHeight = item.value * size.height
        val offsetX = index * (barWidth + barSpace) + barSpace / 2
        val offsetY = size.height - barHeight
        // 绘制柱子
        drawRect(
            color = dataSource[index].color ?: barColor,
            topLeft = Offset(offsetX, offsetY),
            size = Size(barWidth, barHeight)
        )
        // 绘制数值
        if (barWidth >= 10.dp.toPx()) {
            drawValueLabel(
                value = dataSource[index].value,
                offsetX,
                offsetY,
                barWidth,
                textMeasurer,
                formatter,
                barColor
            )
        }
    }
}

private fun DrawScope.drawValueLabel(
    value: Float,
    offsetX: Float,
    offsetY: Float,
    barWidth: Float,
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
            offsetX + barWidth / 2 - textLayoutResult.size.width / 2,
            offsetY - textLayoutResult.size.height - 5.dp.toPx()
        )
    )
}

internal fun DrawScope.drawXAxis(
    labels: List<String>,
    barWidth: Float,
    spaceWidth: Float,
    axisColor: Color,
    labelColor: Color,
    textMeasurer: TextMeasurer
) {
    // 绘制X轴
    drawLine(
        axisColor,
        Offset(x = 0f, size.height),
        Offset(x = size.width, size.height),
        strokeWidth = 1.dp.toPx()
    )

    // 最后一个刻度的结束位置，初始化为负值表示还未开始绘制
    var lastLabelEndX = -Float.MAX_VALUE
    // 刻度之间的最小间隔
    val labelPadding = 4.dp.toPx()

    labels.forEachIndexed { index, label ->
        val textLayoutResult = textMeasurer.measure(
            label,
            TextStyle(fontSize = 14.sp)
        )

        // 计算当前刻度的起始和结束位置
        val labelStartX = index * (barWidth + spaceWidth) + spaceWidth / 2 +
                barWidth / 2 - textLayoutResult.size.width / 2
        val labelEndX = labelStartX + textLayoutResult.size.width

        // 仅当当前刻度不与上一个刻度重叠时才绘制
        if (labelStartX >= lastLabelEndX + labelPadding) {
            drawText(
                textLayoutResult,
                labelColor,
                topLeft = Offset(
                    x = labelStartX,
                    y = size.height + 10.dp.toPx()
                )
            )
            // 更新最后一个刻度的结束位置
            lastLabelEndX = labelEndX
        }
    }
}

internal fun DrawScope.drawYAxis(
    yValues: List<String>,
    axisColor: Color,
    labelColor: Color,
    textMeasurer: TextMeasurer
) {
    // 绘制Y轴
    drawLine(
        axisColor,
        Offset(x = 0f, size.height),
        Offset(x = 0f, 0f),
        strokeWidth = 1.dp.toPx()
    )
    val yAxisStep = size.height / (yValues.size - 1)
    // 绘制Y轴刻度线
    yValues.forEachIndexed { index, label ->
        val y = yAxisStep * (yValues.size - index - 1)
        // 绘制虚线
//        drawLine(
//            start = Offset(0f, y),
//            end = Offset(size.width, y), // 线条长度可以根据需要调整
//            color = axisColor,
//        )
        drawLine(
            color = axisColor,
            start = Offset(0f, y),
            end = Offset(size.width, y), // 线条长度可以根据需要调整
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        )
        val textLayoutResult = textMeasurer.measure(
            label,
            TextStyle(fontSize = 10.sp)
        )
//        // 绘制标签
//        drawText(
//            textLayoutResult,
//            labelColor,
//            topLeft = Offset(
//                x = -((label.length + 1) * 10).toFloat(),
//                y = y - 10
//            )
//        )
    }
}
@Composable
fun DashedLine(
    start: Dp = 0.dp,
    end: Dp = 200.dp,
    color: Color = Color.Black,
    strokeWidth: Dp = 2.dp,
    dashWidth: Dp = 10.dp,
    dashGap: Dp = 5.dp
) {
    Canvas(modifier = Modifier.size(200.dp, 1.dp)) {
        val paint = Paint()
        paint.color = color
        paint.strokeWidth = strokeWidth.toPx()
        paint.strokeCap = Butt

        val path = Path()
        path.moveTo(start.toPx(), 0f)
        path.lineTo(end.toPx(), 0f)

        drawIntoCanvas { canvas ->
            paint.pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth.toPx(), dashGap.toPx()), 0f)
            canvas.drawPath(path, paint)
        }
    }
}