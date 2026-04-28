package poct.device.app.ui.sample

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
import coil.decode.ImageDecoderDecoder
import poct.device.app.R

class GifSample {
    @Composable
    fun Sample () {
        val imageLoader = ImageLoader.Builder(LocalContext.current)
            .components { add(ImageDecoderDecoder.Factory()) }
            .build()
        Image(
            modifier = Modifier
                .width(330.dp)
                .height(462.dp),
            painter = rememberAsyncImagePainter(
                R.drawable.work_sample,
                imageLoader = imageLoader
            ),
            contentScale = ContentScale.FillBounds,
            contentDescription = ""
        )
    }
}