package poct.device.app.ui.sample

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController


/**
 * 页面定义
 */
@Composable
fun SampleSplash(navController: NavController) {


}

//@Composable
//fun SampleImageFromAssets(navController: NavController) {
//    val reportPng = LocalContext.current
//        .assets
//        .open("report.png")
//    val bitmap = BitmapFactory.decodeStream(reportPng)
//
//    Image(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(bgColor),
//        bitmap = bitmap.asImageBitmap(),
//        contentDescription = ""
//    )
//
//}

@Preview
@Composable
fun SampleSplashPreview() {
    SampleSplash(rememberNavController())
}