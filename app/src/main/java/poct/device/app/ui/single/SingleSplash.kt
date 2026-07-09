package poct.device.app.ui.single

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
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
            Image(
                painter = painterResource(id = R.drawable.startup),
                contentDescription = "",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
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