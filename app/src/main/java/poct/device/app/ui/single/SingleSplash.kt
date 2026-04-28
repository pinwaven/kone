package poct.device.app.ui.single

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import poct.device.app.R
import poct.device.app.RouteConfig
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppScaffold
import poct.device.app.serial.v2.ctl.CtlCommandsV2
import poct.device.app.theme.bgColor

/**
 * 页面定义
 */
@Composable
fun SingleSplash(navController: NavController, viewModel: SingleLoginViewModel = viewModel()) {
    LaunchedEffect(Unit) {
        delay(CtlCommandsV2.delayMs * 10)

        // TODO 简化信息
//        if (AppParams.devMock) {
//            viewModel.onLoginWithDefaultUser {
//                navController.navigate(route = RouteConfig.HOME)
//            }
//        } else {
//            navController.navigate(route = RouteConfig.SINGLE_LOGIN)
//        }

        navController.navigate(route = RouteConfig.HOME)
    }
    AppScaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor) // bgColor
        ) {
//            Image(
//                modifier = Modifier.fillMaxSize(),
//                painter = painterResource(id = R.drawable.splash_img),
//                contentDescription = ""
//            )

            Column {
                Spacer(modifier = Modifier.height(146.dp))
//                Image(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(54.dp),
//                    painter = painterResource(id = R.drawable.splash_logo),
//                    contentDescription = null
//                )
                Image(
//                    painter = painterResource(id = R.drawable.vh_logo),
                    painter = painterResource(id = R.drawable.vh_logo_v2),
//                    painter = painterResource(id = R.drawable.vh_logo_v3),
                    contentDescription = "",
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.Center)
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    textAlign = TextAlign.Center,
//                    color = filledFontColor,
                    fontSize = 16.sp,
                    text = stringResource(R.string.app_name)
                )
            }
        }
    }
}


@Preview(locale = "cn")
@Composable
fun PageSplashPreview() {
    val navController = rememberNavController()
    val viewModel: SingleLoginViewModel = viewModel()

    AppPreviewWrapper {
        SingleSplash(navController, viewModel)
    }
}