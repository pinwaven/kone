package poct.device.app.ui.work


import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import poct.device.app.theme.fontColor

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun WorkMainActionWorkBlockV2(
    cardConfigBean: State<CardConfig>,
    onCutOff2TimeFinished: () -> Unit = {}
) {
    // 秒
    var cutOff2Time by remember { mutableDoubleStateOf(cardConfigBean.value.cutOff2) }

    @SuppressLint("DefaultLocale")
    fun formatTime(secondsTotal: Int): String {
        val minutes = secondsTotal / 60
        val seconds = secondsTotal % 60
        return String.format("%02d:%02d", minutes, seconds)
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
                val rawUri =
                    "android.resource://${App.getContext().packageName}/${R.raw.mv3}"
                AppVideoPlayer(
                    video = rawUri.toUri(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(315.dp)
                )

                if (cutOff2Time > 0) {
                    LaunchedEffect(cutOff2Time) {
                        if (cutOff2Time > 0) {
                            delay(1000)
                            cutOff2Time--
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatTime(cutOff2Time.toInt()),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = fontColor,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.work_case_cut_off2_wait),
                        fontSize = 14.sp,
                        color = fontColor,
                    )
                } else {
                    onCutOff2TimeFinished()
                }
            }
        }
    }
}

@Preview
@Composable
fun WorkMainActionWorkBlockV2Preview() {
    val viewModel: WorkMainViewModel = viewModel()
    val cardConfigBean = viewModel.curCardConfig.collectAsState()
    WorkMainActionWorkBlockV2(cardConfigBean)
}