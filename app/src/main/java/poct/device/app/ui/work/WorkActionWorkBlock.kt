package poct.device.app.ui.work


import android.annotation.SuppressLint
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import poct.device.app.App
import poct.device.app.R
import poct.device.app.bean.card.CardConfig
import poct.device.app.component.AppVideoPlayer
import poct.device.app.theme.bgColor
import poct.device.app.theme.filledFontColor
import poct.device.app.theme.primaryColor
import poct.device.app.utils.app.AppDictUtils
import timber.log.Timber
import java.text.DecimalFormat
import kotlin.math.ceil
import kotlin.math.max

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun WorkMainActionWorkBlock(
    cardConfigBean: State<CardConfig>,
    progress: State<Float>,
    showFy0Time: State<Boolean>,
    showTotalTime: State<Boolean>,
    checkStep: State<Int>,
    showTime: State<Boolean>,
) {
    // 秒
    var totalFy0Time by remember { mutableIntStateOf(cardConfigBean.value.ft0) }
    // 秒
    var totalTime by remember {
        mutableIntStateOf(cardConfigBean.value.ft1 + cardConfigBean.value.xt1)
    }

    val fy0Minutes by remember(totalFy0Time) {
        derivedStateOf {
            max(1.0, ceil(totalFy0Time / 60.0))
        }
    }
    val totalMinutes by remember(totalTime) {
        derivedStateOf {
            max(1.0, ceil(totalTime / 60.0))
        }
    }

    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(horizontal = 15.dp),
            shape = RoundedCornerShape(8.dp),
            color = bgColor
        ) {
            // 标题
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(14.dp))
                if (showTime.value) {
                    Text(
                        text = stringResource(id = R.string.work_ing),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(15.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(247.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(204.dp)
                                    .height(156.dp)
                            ) {
                                Image(
                                    modifier = Modifier
                                        .width(204.dp)
                                        .height(156.dp),
                                    painter = painterResource(id = R.mipmap.img_jindu),
                                    contentDescription = "",
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                /*@FloatRange(from = 0.0, to = 1.0)*/
                                progress = { 1F },
                                modifier = Modifier.size(96.dp),
                                strokeWidth = 16.dp,
                                strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                                color = primaryColor
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                /*@FloatRange(from = 0.0, to = 1.0)*/
                                progress = { progress.value / 100F },
                                modifier = Modifier.size(96.dp),
                                strokeWidth = 16.dp,
                                strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                                color = filledFontColor
                            )
                        }
                        val df = remember { DecimalFormat("#") }
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "${df.format(progress.value)}%", fontSize = 24.sp)
                        }
                    }
                    Text(
                        text = AppDictUtils.label(
                            AppDictUtils.checkStepMap(LocalContext.current),
                            checkStep.value
                        ), fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(15.dp))
                    Timber.w("当前进展：${progress.value}")
//                Timber.w("当前时间进度：${progressTime.value}")

                    if (showFy0Time.value) {
                        LaunchedEffect(totalFy0Time) {
                            if (totalFy0Time > 0) {
                                delay(1000)
                                totalFy0Time--
                            }
                        }
                        Text(
                            text = App.getContext().getString(R.string.work_ing_time1)
                                .format(DecimalFormat("#").format(fy0Minutes)),
                            fontSize = 14.sp
                        )
                    }

                    if (showTotalTime.value) {
                        LaunchedEffect(totalTime) {
                            if (totalTime > 0) {
                                delay(1000)
                                totalTime--
                            }
                        }
                        Text(
                            text = App.getContext().getString(R.string.work_ing_time1)
                                .format(DecimalFormat("#").format(totalMinutes)),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    val rawUri =
                        "android.resource://${App.getContext().packageName}/${R.raw.mv4}"
                    AppVideoPlayer(
                        video = rawUri.toUri(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(315.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun WorkMainActionWorkBlockPreview() {
    val viewModel: WorkMainViewModel = viewModel()
    val cardConfigBean = viewModel.curCardConfig.collectAsState()
    val progress = viewModel.progress.collectAsState()
    val showFy0Time = viewModel.showFy0Time.collectAsState()
    val showTotalTime = viewModel.showTotalTime.collectAsState()
    val checkStep = viewModel.checkStep.collectAsState()
    val showTime = viewModel.showTime.collectAsState()
    WorkMainActionWorkBlock(
        cardConfigBean, progress,
        showFy0Time, showTotalTime, checkStep,
        showTime
    )
}