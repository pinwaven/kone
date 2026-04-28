package poct.device.app.ui.sample

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.R
import poct.device.app.component.AppPDFView
import poct.device.app.theme.primaryColor
import poct.device.app.utils.app.AppFileUtils
import java.io.File
import java.io.FileOutputStream


/**
 * 页面定义
 */
@Composable
fun SampleGenPdf1(navController: NavController) {
    var pdfLoaded by remember { mutableIntStateOf(0) }
    val path = AppFileUtils.getInnerRoot()
    val context = LocalContext.current
    LaunchedEffect(key1 = Unit) {
        // declaring width and height
        // for our PDF file. A4纸适配
        val pageWidth = 596
        val pageHeight = 842

        // creating a bitmap variable
        // for storing our images
        lateinit var bgBitmap: Bitmap

        // creating an object variable
        // for our PDF document.
        val pdfDocument = PdfDocument()
        // two variables for paint "paint" is used
        // for drawing shapes and we will use "title"
        // for adding text in our PDF file.
        val paint = Paint()

        // on below line we are initializing our bitmap and scaled bitmap.
        val bgStream = context.assets.open("report_template/report_bg.png")
        bgStream.use {
//            bgBitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(it), pageWidth, pageHeight, false)
            bgBitmap = BitmapFactory.decodeStream(it)
        }

        // we are adding page info to our PDF file
        // in which we will be passing our pageWidth,
        // pageHeight and number of pages and after that
        // we are calling it to create our PDF.
        val myPageInfo: PdfDocument.PageInfo? =
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()

        // below line is used for setting
        // start page for our PDF file.
        val myPage: PdfDocument.Page = pdfDocument.startPage(myPageInfo)

        // creating a variable for canvas
        // from our page of PDF.
        val canvas: Canvas = myPage.canvas
        // below line is used to draw our image on our PDF file.
        // the first parameter of our drawbitmap method is
        // our bitmap
        // second parameter is position from left
        // third parameter is position from top and last
        // one is our variable for paint.
        canvas.drawBitmap(bgBitmap, 0F, 0F, paint)

        // below line is used for adding typeface for
        // our text which we will be adding in our PDF file.
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL))

        // below line is used for setting text size
        // which we will be displaying in our PDF file.
        paint.textSize = 15F

        // below line is sued for setting color
        // of our text inside our PDF file.
        paint.setColor(ContextCompat.getColor(context, R.color.purple_200))

        // below line is used to draw text in our PDF file.
        // the first parameter is our text, second parameter
        // is position from start, third parameter is position from top
        // and then we are passing our variable of paint which is title.
        canvas.drawText("A portal for IT professionals.", 209F, 100F, paint)
        canvas.drawText("Geeks for Geeks", 209F, 80F, paint)
        paint.setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL))
        paint.setColor(context.getColor(R.color.purple_200))
        paint.textSize = 15F

        // below line is used for setting
        // our text to center of PDF.
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("This is sample document which we have created.", 396F, 560F, paint)

        // after adding all attributes to our
        // PDF file we will be finishing our page.
        pdfDocument.finishPage(myPage)

        // below line is used to set the name of
        // our PDF file and its path.
        val file = File("$path/dlx-test.pdf")

        try {
            // after creating a file name we will
            // write our PDF file to that location.
            pdfDocument.writeTo(FileOutputStream(file))

            // on below line we are displaying a toast message as PDF file generated..
            Toast.makeText(context, "PDF file generated..", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // below line is used
            // to handle error
            e.printStackTrace()

            // on below line we are displaying a toast message as fail to generate PDF
            Toast.makeText(context, "Fail to generate PDF file..", Toast.LENGTH_SHORT)
                .show()
        }
        // after storing our pdf to that
        // location we are closing our PDF file.
        pdfDocument.close()

        pdfLoaded = 0
    }

    if (pdfLoaded == 1) {
        AppPDFView(modifier = Modifier.fillMaxSize(), pdfPath = "$path/a.pdf")
    }


//    Box(modifier = Modifier.fillMaxSize()) {
//
//    }
//    var bitmap: Bitmap? = null
//    var bitmap: Bitmap? by remember { mutableStateOf(null) }
//    LaunchedEffect(key1 = pdfLoaded) {
//        if (pdfLoaded == 1) {
//            val pdfRenderer = PdfRenderer(
//                ParcelFileDescriptor.open(
//                    File("$path/dlx-test.pdf"),
//                    ParcelFileDescriptor.MODE_READ_ONLY
//                )
//            )
//
//
//            pdfRenderer.openPage(0).use {
//                bitmap = Bitmap.createBitmap(it.width, it.height, Bitmap.Config.ARGB_8888)
//                it.render(bitmap!!, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
//                pdfLoaded = 2
//            }
//        }
//    }
//    if (pdfLoaded == 2) {
//        Image(
//            modifier = Modifier
//                .fillMaxSize()
//                .background(bgColor),
//            bitmap = bitmap!!.asImageBitmap(),
//            contentDescription = ""
//        )
//    }


}


/**
 * 内容主体
 */
@Composable
fun SampleGenPdf1Body(navController: NavController) {
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

@Preview
@Composable
fun SampleGenPdf1Preview() {
    SampleGenPdf1(rememberNavController())
}