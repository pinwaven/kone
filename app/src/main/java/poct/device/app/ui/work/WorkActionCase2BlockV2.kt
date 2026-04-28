package poct.device.app.ui.work

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import poct.device.app.App
import poct.device.app.R
import poct.device.app.bean.card.CardConfig
import poct.device.app.component.AppVideoPlayer
import poct.device.app.theme.bgColor


@Composable
fun WorkMainActionCase2BlockV2(
    cardConfigBean: State<CardConfig>,
    isFy0Finished: State<Boolean>,
) {
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
                var rawUri =
                    "android.resource://${App.getContext().packageName}/${R.raw.mv2}"
                if (cardConfigBean.value.ft0 >= 1) {
                    if (isFy0Finished.value) {
                        rawUri =
                            "android.resource://${App.getContext().packageName}/${R.raw.mv5}"
                    } else {
                        rawUri =
                            "android.resource://${App.getContext().packageName}/${R.raw.mv4}"
                    }
                }
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

@Preview
@Composable
fun WorkMainActionCase2BlockV2Preview() {
    val viewModel: WorkMainViewModel = viewModel()
    val cardConfigBean = viewModel.curCardConfig.collectAsState()
    val isFy0Finished = viewModel.isFy0Finished.collectAsState()
    WorkMainActionCase2BlockV2(
        cardConfigBean, isFy0Finished
    )
}