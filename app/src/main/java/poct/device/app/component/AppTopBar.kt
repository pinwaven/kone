package poct.device.app.component

import android.view.View
import android.widget.TextClock
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableStateFlow
import org.greenrobot.eventbus.Subscribe
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.RouteConfig
import poct.device.app.entity.User
import poct.device.app.event.AppBatteryEvent
import poct.device.app.event.AppWifiEvent
import poct.device.app.theme.dangerColor
import poct.device.app.theme.filledFontColor
import poct.device.app.theme.topBarBgColor
import poct.device.app.theme.topBarColor
import poct.device.app.theme.warningColor
import poct.device.app.utils.app.AppEventUtils
import timber.log.Timber

@Composable
fun AppTopBar(
    navController: NavController,
    title: String,
    // 返回主页按钮
    homeEnabled: Boolean = false,
    // 登录信息
    loginInfoEnabled: Boolean = false,
    // 返回按钮
    backEnabled: Boolean = false,
    // 详情按钮
    detailEnable: String = "-",
    onHome: () -> Unit = { navController.navigate(RouteConfig.HOME_MAIN) },
    onBack: () -> Unit = { navController.popBackStack() },
    onDetail: () -> Unit = {},
    trailingContent: (@Composable () -> Unit)? = null,
    viewModel: AppTopBarViewModel = viewModel(),
) {
    val wifiConnected by viewModel.wifiConnected.collectAsState()
    val battery by viewModel.battery.collectAsState()
    val batteryPlugged by viewModel.batteryPlugged.collectAsState()
    DisposableEffect(key1 = Unit) {
        AppEventUtils.register(viewModel)
        onDispose {
            AppEventUtils.unregister(viewModel)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(topBarBgColor)
    ) {
        AppTopBarStateBlock(
            wifiConnected = wifiConnected,
            battery = battery,
            batteryPlugged = batteryPlugged,
        )
        AppTopBarTitleBlock(
            title, homeEnabled, loginInfoEnabled, backEnabled, detailEnable,
            onNavigate = { navController.navigate(it) },
            onLogout = { viewModel.logout(it) },
            onHome = { onHome() },
            onBack = { onBack() },
            onDetail = { onDetail() },
            trailingContent = trailingContent,
        )
    }
}

@Composable
private fun AppTopBarStateBlock(
    wifiConnected: Boolean,
    battery: Int,
    batteryPlugged: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .padding(vertical = 0.dp, horizontal = 10.dp),
//        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
//        AppTopBarDateTime()
        AppTopBarMonitor(wifiConnected, battery, batteryPlugged)
//        AppTopBarMonitor(battery, batteryPlugged)
    }
}

@Composable
fun AppTopBarMonitor(
    wifiConnected: Boolean,
    battery: Int,
    batteryPlugged: Boolean
) {
    Row(
        modifier = Modifier
            .width(108.dp)
            .fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically,
//        horizontalArrangement = Arrangement.End
    ) {
//        val shfwColor = dangerColor
//        Icon(
//            painter = painterResource(id = R.mipmap.shfw_icon),
//            contentDescription = "",
//            tint = shfwColor
//        )
//        Spacer(modifier = Modifier.width(4.dp))
        val wifiColor =
            if (wifiConnected) filledFontColor
            else dangerColor
        val wifiResId =
            if (wifiConnected) R.mipmap.wifi_icon
            else R.mipmap.wifi_no_icon
        Icon(
            painter = painterResource(id = wifiResId),
            contentDescription = "",
            tint = wifiColor
        )
        Spacer(modifier = Modifier.width(4.dp))
        val dcResId: Int = if (batteryPlugged) {
            R.mipmap.ele_d_icon05
        } else if (battery < 5) {
            R.mipmap.ele_icon05
        } else if (battery <= 20) {
            R.mipmap.ele_icon04
        } else if (battery <= 50) {
            R.mipmap.ele_icon03
        } else if (battery <= 75) {
            R.mipmap.ele_icon02
        } else {
            R.mipmap.ele_icon01
        }
        val dcColor: Color = if (battery < 0) {
            dangerColor
        } else if (battery <= 20) {
            warningColor
        } else {
            filledFontColor
        }
        Icon(
            painter = painterResource(id = dcResId),
            contentDescription = "",
            tint = dcColor
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = "${battery}%",
            fontSize = 10.sp,
            textAlign = TextAlign.Right,
            color = topBarColor,
        )
    }
}

@Composable
private fun AppTopBarTitleBlock(
    title: String,
    homeEnabled: Boolean = false,
    loginInfoEnabled: Boolean = false,
    // 返回按钮
    backEnabled: Boolean = false,
    detailEnable: String,
    onNavigate: (path: String) -> Unit,
    onLogout: (() -> Unit) -> Unit,
    onHome: () -> Unit,
    onBack: () -> Unit,
    onDetail: () -> Unit,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .width(108.dp)
        ) {
            // 左边操作
            if (homeEnabled) {
                AppTopBarLeftAction(
                    icon = painterResource(id = R.mipmap.home_icon),
                    text = stringResource(R.string.home),
                    onClick = {
                        onHome()
                    }
                )
            }
            if (backEnabled) {
                AppTopBarLeftAction(
                    icon = painterResource(id = R.mipmap.fh_icon),
                    text = stringResource(R.string.back),
                    onClick = { onBack() }
                )
            }

            // TODO 简化信息
//            if (!AppParams.devMock && loginInfoEnabled) {
            if (loginInfoEnabled) {
                var logoutVisible by remember { mutableStateOf(false) }
                AppTopBarLeftAction(
                    icon = painterResource(id = R.mipmap.avatar_in_bar),
                    text = AppParams.curUser.username,
                    onClick = {
                        logoutVisible = true
                    }
                )
                AppConfirm(
                    visible = logoutVisible,
                    title = stringResource(id = R.string.login_logout),
                    content = stringResource(id = R.string.login_logout_confirm),
                    confirmText = stringResource(id = R.string.btn_label_logout),
                    onCancel = { logoutVisible = false },
                    onConfirm = {
                        onLogout {
                            logoutVisible = false
                            onNavigate(RouteConfig.SINGLE_LOGIN)
                        }
                    }
                )
            }
        }

        // 标题
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .width(140.dp)
        ) {
            AppTopBarTitleBlock(title)
        }

        // 右边操作
        Row(
            modifier = Modifier.fillMaxHeight(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
//            AppTopBarDateTime()
            Spacer(modifier = Modifier.width(40.dp))
            if (detailEnable == "norm") {
                AppTopBarRightAction(
                    text = stringResource(R.string.norm),
                    onClick = { onDetail() }
                )
            }
            if (detailEnable == "major") {
                AppTopBarRightAction(
                    text = stringResource(R.string.major),
                    onClick = {
                        onDetail()
                    }
                )
            }
            if (trailingContent != null) {
                trailingContent()
            }
        }
    }
}

@Composable
private fun AppTopBarLeftAction(
    icon: Painter,
    onClick: () -> Unit,
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .padding(start = 16.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = topBarColor
        )
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = text,
            fontSize = 12.sp,
            textAlign = TextAlign.Right,
            color = topBarColor,
        )
    }
}

@Composable
private fun AppTopBarTitleBlock(title: String) {
    Row(
        modifier = Modifier
            .fillMaxHeight(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // TODO 简化信息
//        Text(
//            fontSize = 20.sp,
//            textAlign = TextAlign.Center,
//            color = topBarColor,
//            text = title
//        )
        Image(
//            painter = painterResource(id = R.drawable.vh_logo),
            painter = painterResource(id = R.drawable.vh_logo_v2),
//            painter = painterResource(id = R.drawable.vh_logo_v3),
            contentDescription = "",
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.Center)
        )
    }
}

@Composable
private fun AppTopBarRightAction(
    onClick: () -> Unit,
    text: String,
) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .padding(start = 16.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.padding(start = 4.dp),
            text = text,
            fontSize = 12.sp,
            textAlign = TextAlign.Right,
            color = topBarColor,
        )
    }
}

@Composable
private fun AppTopBarDateTime() {
    // 时钟
    AndroidView(
        factory = { context ->
            TextClock(context).apply {
                // 24小时
                format12Hour = null
                format24Hour = "MM月dd日 HH:mm"
                textAlignment = View.TEXT_ALIGNMENT_TEXT_END
                textSize = 10F
                setTextColor(resources.getColor(R.color.topBarColor, null))
            }
        },
    )
}

class AppTopBarViewModel : ViewModel() {
    val wifiConnected = MutableStateFlow(AppParams.wlanEnabled)
    val battery = MutableStateFlow(AppParams.battery)
    val batteryPlugged = MutableStateFlow(AppParams.batteryPlugged)

    fun logout(callback: () -> Unit = {}) {
        AppParams.curUser = User.Empty
        callback()
    }

    @Subscribe
    fun handleWifiEvent(event: AppWifiEvent) {
        Timber.w("======curWlan%s", AppParams.curWlan)
        wifiConnected.value = AppParams.curWlan.connected
    }

    @Subscribe
    fun handleBatteryEvent(event: AppBatteryEvent) {
        battery.value = AppParams.battery
        batteryPlugged.value = AppParams.batteryPlugged
    }
}

@Preview
@Composable
fun AppTopBarPreview(viewModel: AppTopBarViewModel = viewModel()) {
    val navController = rememberNavController()
    LaunchedEffect(key1 = Unit) {
        viewModel.battery.value = 90
        viewModel.batteryPlugged.value = false
//        viewModel.wifiConnected.value = true
    }
    AppPreviewWrapper {
        AppTopBar(
            navController = navController,
            viewModel = viewModel,
            title = "sample",
            homeEnabled = false,
            loginInfoEnabled = false,
            backEnabled = true,
            detailEnable = "norm",
        )
    }
}

