package poct.device.app.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.RouteConfig
import poct.device.app.component.AppAlert
import poct.device.app.component.AppAutoScrollPager
import poct.device.app.component.AppPowerButton
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppSettingButton
import poct.device.app.component.AppTopBar
import poct.device.app.theme.bgColor
import timber.log.Timber

/**
 * 页面定义
 */
@Composable
fun HomeMain(
    navController: NavController,
    viewModel: MainMainViewModel = viewModel()
) {
    AppScaffold(
        topBar = {
            AppTopBar(
                navController = navController,
                title = LocalContext.current.getString(R.string.home),
                homeEnabled = false,
                loginInfoEnabled = false
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
        ) {
            Column {
                HomeMainBody(navController, viewModel)
            }
            AppPowerButton(start = 12.dp, bottom = 40.dp)
            AppSettingButton(
                navController = navController,
                end = 26.dp,
                bottom = 40.dp
            )
        }
    }
}

/**
 * 内容主体
 */
@Composable
fun HomeMainBody(
    navController: NavController,
    viewModel: MainMainViewModel
) {
    val user = AppParams.curUser
    Timber.w("===${App.gson.toJson(user)}")

    val images = listOf(
        // TODO 简化信息
//        R.drawable.one,
//        R.drawable.two,
//        R.drawable.three,
//        R.drawable.four,
        R.drawable.five,
//        R.drawable.six,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
//                start = 12.dp, end = 12.dp,
                top = 12.dp, bottom = 12.dp
            )
    ) {
        // TODO 简化信息
//        Spacer(
//            modifier = Modifier
//                .fillMaxWidth()
//                .height(25.dp) // 42
//        )
        // Image(painter = painterResource(id = R.mipmap.logo), contentDescription = "")
//        Image(
//            painter = painterResource(id = R.mipmap.vh_logo), contentDescription = "",
//            modifier = Modifier
//                .fillMaxWidth()
//                .wrapContentSize(Alignment.Center)
//        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.65f),
            horizontalArrangement = Arrangement.Center
        ) {
//            Text(color = Color.Red, text = "Hello world!")
            // 轮播图区域
            AppAutoScrollPager(
                images = images,
                modifier = Modifier
                    .fillMaxSize(),
                autoScrollInterval = 7000
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(top = 12.dp, bottom = 49.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            val wifiConnected by viewModel.wifiConnected.collectAsState()
            LaunchedEffect(wifiConnected) {
                if (wifiConnected) {
                    Timber.w("HomeMain isNetworkAccessible ${viewModel.isNetworkAccessible()}")
                }
            }

            // 样本检测
            var workPreVisible by remember { mutableStateOf(false) }
            HomeMainEntry(
                painterResource(id = R.mipmap.home_btn),
                onClick = {
                    if (AppParams.initState) {
                        App.getSerialHelper().reconnect()
                        navController.navigate(RouteConfig.WORK)

                        // TODO countdown
//                        navController.navigate(RouteConfig.COUNTDOWN)
                    } else {
                        workPreVisible = true
                    }
                }
            )
            HomeWorkPre(
                visible = workPreVisible,
                onClose = { workPreVisible = false },
                onOk = {
                    workPreVisible = false

                    // TODO 优化体验感
//                    navController.navigate(RouteConfig.WORK)
                }
            )
            AppAlert(
                visible = !wifiConnected,
                title = stringResource(id = R.string.wlan_pre_connect),
                content = stringResource(id = R.string.wlan_not_connect),
                onOk = { navController.navigate(RouteConfig.SETTING_MAIN) }
            )

            // TODO 简化信息
//                // 样本报告
//                HomeMainEntry(
//                    painterResource(id = R.mipmap.ybbg_img),
//                    onClick = { navController.navigate(RouteConfig.REPORT) }
//                )
        }
    }
}

@Composable
private fun HomeMainEntry(painter: Painter, onClick: () -> Unit) {
    Image(
        painter = painter,
        modifier = Modifier
            .padding(start = 22.dp, end = 22.dp)
            .clickable { onClick() },
        contentDescription = "",
        contentScale = ContentScale.FillBounds
    )
}

@Preview
@Composable
fun HomeMainPreview() {
    AppPreviewWrapper {
        HomeMain(rememberNavController())
    }
}