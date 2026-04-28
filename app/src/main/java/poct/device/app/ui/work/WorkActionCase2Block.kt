package poct.device.app.ui.work

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import poct.device.app.App
import poct.device.app.R
import poct.device.app.theme.bgColor
import poct.device.app.theme.fontColor
import poct.device.app.theme.inactive2Color
import poct.device.app.theme.primaryColor
import java.text.DecimalFormat


@Composable
fun WorkMainActionCase2Block(
    waitProgress: State<Float>,
    waitTotal: State<Float>,
    waitVisible: Boolean,
    viewModel: WorkMainViewModel = viewModel(),
) {
    var value = waitTotal.value - waitTotal.value * waitProgress.value / 100
    val alpha  by animateFloatAsState(
        targetValue = if(waitVisible && value < 30) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = ""
    )

    Box (
        Modifier
            .fillMaxWidth()
            .height(400.dp)
            .padding(horizontal = 15.dp),
    ){
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            shape = RoundedCornerShape(8.dp),
            color = bgColor
        ) {
            // 标题
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(id = R.string.work_case_wait),
                    fontSize = 16.sp,
                    color = fontColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(15.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            modifier = Modifier
                                .width(204.dp)
                                .height(156.dp),
                            painter = painterResource(id = R.mipmap.img_jindu),
                            contentDescription = "",
                        )
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
                            color = inactive2Color
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            /*@FloatRange(from = 0.0, to = 1.0)*/
                            progress = { waitProgress.value / 100F },
                            modifier = Modifier.size(96.dp),
                            strokeWidth = 16.dp,
                            strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap
                        )
                    }
                    val df = remember { DecimalFormat("#") }
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(text = "${df.format(waitProgress.value)}%", fontSize = 24.sp)
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))
                val df = remember { DecimalFormat("#") }
                Text(text = df.format(waitTotal.value - waitTotal.value * waitProgress.value / 100), fontSize = 60.sp, color = primaryColor)
                Spacer(modifier = Modifier.height(5.dp))
                Text(text = App.getContext().getString(R.string.work_case_waiting).format(df.format(waitTotal.value - waitTotal.value * waitProgress.value / 100)), fontSize = 18.sp, color = fontColor)
            }
        }
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp),
            painter = painterResource(id = R.mipmap.tip_img),
            alpha = alpha,
            contentDescription = "",
        )
    }

}