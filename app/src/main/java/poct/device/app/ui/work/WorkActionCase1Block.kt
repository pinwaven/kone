package poct.device.app.ui.work


import androidx.compose.foundation.Image
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.ImageDecoderDecoder
import poct.device.app.R
import poct.device.app.theme.bg3Color
import poct.device.app.theme.fontColor

@Composable
fun WorkActionCase1Block(
    viewModel: WorkMainViewModel = viewModel(),
) {
    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(horizontal = 15.dp),
            shape = RoundedCornerShape(8.dp),
            color = bg3Color
        ) {
            // 标题
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(id = R.string.work_case_put_chip),
                    fontSize = 16.sp,
                    color = fontColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(15.dp))
                val imageLoader = ImageLoader.Builder(LocalContext.current)
                    .components { add(ImageDecoderDecoder.Factory()) }
                    .build()
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(247.dp),
                    painter = rememberAsyncImagePainter(
                        R.drawable.ani_chip_in,
                        imageLoader = imageLoader
                    ),
                    contentScale = ContentScale.FillBounds,
                    contentDescription = ""
                )
                Spacer(modifier = Modifier.height(15.dp))
                Text(
                    text = stringResource(id = R.string.work_case_put_chip_tip),
                    fontSize = 14.sp,
                    color = fontColor,
                )
            }
        }
    }
}

@Preview
@Composable
fun WorkActionCase1BlockPreview() {
    WorkActionCase1Block()
}