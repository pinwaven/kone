package poct.device.app.ui.sample

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import poct.device.app.R
import poct.device.app.bean.PdfBean
import poct.device.app.component.AppPDFView
import poct.device.app.theme.primaryColor
import poct.device.app.utils.app.AppFileUtils
import poct.device.app.utils.app.AppPdfUtils


/**
 * 页面定义
 */
@Composable
fun PdfSample(navController: NavController) {
    var pdfLoaded by remember { mutableIntStateOf(0) }
    val path = AppFileUtils.getInnerRoot()
    val context = LocalContext.current
    LaunchedEffect(key1 = Unit) {
        context.assets.open("report_template/logo.png").use {
            val logo = BitmapFactory.decodeStream(it)
            AppPdfUtils.generatePdf(
                PdfBean(
                    logo = logo,
                    outPath = "$path/dlx-test.pdf",
                    data = listOf(
                        listOf("XXX", "xxxx", ""),
                        listOf("XXX", "xxxx", ""),
                        listOf("XXX", "xxxx", ""),
                        listOf("XXX", "xxxx", ""),
                    )
                )
            )
        }
        pdfLoaded = 1
    }

    if (pdfLoaded == 1) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(primaryColor)
        ) {
            AppPDFView(
                modifier = Modifier
                    .width(274.dp)
                    .height(391.dp),
                pdfPath = "$path/dlx-test.pdf"
            )
        }
    }
}


/**
 * 内容主体
 */
@Composable
fun PdfSampleBody(navController: NavController) {
    val context = LocalContext.current
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(top = 200.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Image(
                modifier = Modifier
                    .width(360.dp)
                    .height(54.dp),
                painter = painterResource(id = R.mipmap.logo),
                contentScale = ContentScale.Crop,
                contentDescription = null
            )
        }
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 50.dp),
            textAlign = TextAlign.Center,
            color = primaryColor,
            text = context.getString(R.string.app_name)
        )
    }

}

