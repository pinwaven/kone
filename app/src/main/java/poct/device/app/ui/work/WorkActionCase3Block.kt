package poct.device.app.ui.work

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.ImageDecoderDecoder
import poct.device.app.App
import poct.device.app.R
import poct.device.app.bean.CaseBean
import poct.device.app.component.AppVideoPlayer
import poct.device.app.theme.bg3Color
import poct.device.app.theme.fontColor


@Composable
fun WorkMainActionCase3Block(
    bean: State<CaseBean>,
    waitVisible: Boolean,
    viewModel: WorkMainViewModel = viewModel(),
) {
    val alpha by animateFloatAsState(
        targetValue = if (waitVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = ""
    )

    Box(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 15.dp),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize(),
            shape = RoundedCornerShape(8.dp),
            color = bg3Color
        ) {
            // 标题
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(15.dp))
                if (bean.value.type == CaseBean.TYPE_CRP || bean.value.type == CaseBean.TYPE_SF) {
                    Text(
                        text = stringResource(id = R.string.work_case_put_case_diluent),
                        fontSize = 16.sp,
                        color = fontColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(15.dp))
                } else {
//                    Text(
//                        text = stringResource(id = R.string.work_case_put_case),
//                        fontSize = 16.sp,
//                        color = fontColor,
//                        fontWeight = FontWeight.Bold
//                    )
//                    Text(
//                        text = stringResource(id = R.string.work_sample_case),
//                        fontSize = 16.sp,
//                        color = fontColor,
//                        fontWeight = FontWeight.Bold
//                    )
                }
                val imageLoader = ImageLoader.Builder(LocalContext.current)
                    .components { add(ImageDecoderDecoder.Factory()) }
                    .build()
                if (bean.value.type == CaseBean.TYPE_CRP || bean.value.type == CaseBean.TYPE_SF) {
                    Image(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(247.dp),
                        painter = rememberAsyncImagePainter(
                            R.drawable.ani_case_in_blue,
                            imageLoader = imageLoader
                        ),
                        contentScale = ContentScale.FillBounds,
                        contentDescription = ""
                    )
                } else {
//                    Image(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .height(247.dp),
//                        painter = rememberAsyncImagePainter(
//                            R.drawable.ani_case_in_red,
//                            imageLoader = imageLoader
//                        ),
//                        contentScale = ContentScale.FillBounds,
//                        contentDescription = ""
//                    )

                    val rawUri =
                        "android.resource://${App.getContext().packageName}/${R.raw.mv1}"
                    AppVideoPlayer(
//                        video = bean.value.cardInfo.cardBatch.guideVideo!!.toUri(),
                        video = rawUri.toUri(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(315.dp)
                    )
                }
                if (bean.value.type == CaseBean.TYPE_CRP || bean.value.type == CaseBean.TYPE_SF) {
                    Spacer(modifier = Modifier.height(15.dp))
                    Text(
                        text = stringResource(id = R.string.work_case_put_case_buffer_tip),
                        fontSize = 14.sp,
                        color = fontColor,
                    )
                } else {
//                    Text(
//                        text = stringResource(id = R.string.work_case_put_case_tip),
//                        fontSize = 14.sp,
//                        color = fontColor,
//                    )
//                    Text(
//                        text = stringResource(id = R.string.work_case_put_case_tip2),
//                        fontSize = 14.sp,
//                        color = fontColor,
//                        modifier = Modifier.padding(start = 12.dp, end = 12.dp)
//                    )
                }
            }
        }
        if (bean.value.type == CaseBean.TYPE_CRP || bean.value.type == CaseBean.TYPE_SF) {
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
}

@Preview
@Composable
fun WorkMainActionCase3BlockPreview() {
    val viewModel: WorkMainViewModel = viewModel()
    val bean = viewModel.bean.collectAsState()
    var waitVisible by remember { mutableStateOf(true) }
    WorkMainActionCase3Block(bean, waitVisible)
}