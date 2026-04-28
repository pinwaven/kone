package poct.device.app.component

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.createBitmap
import poct.device.app.R
import poct.device.app.theme.bgColor
import poct.device.app.theme.dangerColor
import poct.device.app.theme.filledFontColor
import java.io.File


/**
 */
@Composable
fun AppPDFView(
    modifier: Modifier,
    pdfPath: String,
) {
    var prevVisible by remember { mutableStateOf(false) }
    var bitmap: Bitmap? by remember { mutableStateOf(null) }
    LaunchedEffect(key1 = pdfPath) {
        try {
            val pdfRenderer = PdfRenderer(
                ParcelFileDescriptor.open(
                    File(pdfPath),
                    ParcelFileDescriptor.MODE_READ_ONLY
                )
            )

            pdfRenderer.openPage(0).use {
                bitmap = createBitmap(it.width, it.height)
                it.render(bitmap!!, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            }
        } catch (e: Exception) {
            bitmap = null
        }
    }
    bitmap?.apply {
        Box(modifier = modifier) {
            Image(
                modifier = Modifier.fillMaxSize(),
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = ""
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(5.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                AppFilledButton(
                    text = "查看大图",
                    fontSize = 13.sp,
                    modifier = Modifier
                        .width(80.dp)
                        .height(24.dp),
                    contentPadding = PaddingValues(0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF984B),
                        contentColor = filledFontColor,
                    ),
                    left = {
                        Icon(
                            modifier = Modifier.padding(end = 4.dp),
                            painter = painterResource(id = R.mipmap.icon_bg_fd),
                            contentDescription = ""
                        )
                    },
                    shape = RoundedCornerShape(2.dp),
                    onClick = {
                        prevVisible = true
                    })
            }
        }
        if (prevVisible) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            Dialog(
                onDismissRequest = { prevVisible = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(bgColor)
                ) {
                    Image(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                translationX = offset.x
                                translationY = offset.y
                            }
                            .pointerInput("transform") {
                                // 此处为自定义手势检测，主要是增加返回值判断是否需要消费
                                detectTransformGestures { _, pan, zoom, _ ->
                                    scale = (zoom * scale).coerceAtLeast(1f)
                                    scale = if (scale > 5f) 5f else scale
                                    offset += pan * scale
                                }
                            }
                            .pointerInput("tap") {
                                detectTapGestures(
                                    onDoubleTap = {
                                        scale = 1f
                                        offset = Offset.Zero
                                    }
                                )
                            },
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = ""
                    )
                    // 关闭图标
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, end = 10.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Icon(
                            modifier = Modifier.clickable { prevVisible = false },
                            painter = painterResource(id = R.mipmap.pop_icon_guanbi),
                            tint = dangerColor,
                            contentDescription = ""
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AppPDFViewPreview() {
//    val path = AppFileUtils.DATA_FOLDER

    // 构建当前组建时的测试代码
//    var pdfLoaded by remember { mutableIntStateOf(0) }
//    val path = App.getContext().filesDir.path
//    LaunchedEffect(key1 = Unit) {
//        val file = File("$path/dlx-test.pdf")
//        if (file.exists()) {
//            pdfLoaded = 1
//            return@LaunchedEffect
//        }
//
//        val inputStream = App.getContext().assets.open("test.pdf")
//        val bytes: ByteArray
//        inputStream.use {
//            bytes = it.readBytes()
//        }
//        FileOutputStream("$path/dlx-test.pdf").use {
//            it.write(bytes)
//        }
//        pdfLoaded = 1
//    }
//
//    if (pdfLoaded == 1) {
//        AppPDFView(modifier = Modifier.fillMaxSize(), pdfPath = "$path/dlx-test.pdf")
//    }
}
