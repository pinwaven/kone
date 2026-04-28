package poct.device.app.ui.sysconfig

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.R
import poct.device.app.bean.ConfigOtherBean
import poct.device.app.bean.WlanBean
import poct.device.app.component.AppBottomBar
import poct.device.app.component.AppConfirmDanger
import poct.device.app.component.AppFieldWrapper
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppList
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppSwitch
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewWrapper
import poct.device.app.component.AppWlanConnect
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.theme.dangerColor
import poct.device.app.theme.primaryColor
import poct.device.app.utils.app.AppEventUtils


/**
 * 页面定义
 */
@Composable
fun SysConfigWlan(
    navController: NavController,
    viewModel: SysConfigWlanViewModel = viewModel(),
) {
    val viewState by viewModel.viewState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
//    val curWlan by viewModel.curWlan.collectAsState()
//    val wlanList by viewModel.wlanList.collectAsState()
    val config by viewModel.config.collectAsState()

    DisposableEffect(key1 = Unit) {
        AppEventUtils.register(viewModel)
        onDispose {
            AppEventUtils.unregister(viewModel)
        }
    }
    LaunchedEffect(key1 = viewState) {

    }
    LaunchedEffect(key1 = viewState) {
        if (viewState == ViewState.Default) {
            viewModel.onLoad()
        }
    }

    AppViewWrapper(
        viewState = viewState,
        onErrorClick = { navController.popBackStack() }) {
        AppScaffold(
            topBar = {
                AppTopBar(
                    navController = navController,
                    title = stringResource(id = R.string.sys_config_other_wlan),
                    backEnabled = true
                )
            },
            bottomBar = {
                AppBottomBar {
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(40.dp),
                        enabled = config.wlan == "y",
                        onClick = { viewModel.onLoad() },
                        text = stringResource(id = R.string.btn_label_refresh)
                    )
                }
            }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 15.dp, end = 15.dp, top = 15.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 15.dp)
                ) {
                    Spacer(modifier = Modifier.height(17.dp))
//                    SysConfigWlanHeader(
//                        config,
//                        curWlan,
//                        onConfigUpdated = { viewModel.onConfigUpdated(it) })
//                    Spacer(modifier = Modifier.height(24.dp))
//                    SysConfigWlanList(
//                        wlanList = wlanList,
//                        onDisconnectConfirm = { viewModel.onDisconnectConfirm(it) },
//                        onConnectConfirm = { viewModel.onConnectConfirm(it) }
//                    )
                }
            }
        }
    }

    SysConfigWlanInteraction(
        actionState = actionState,
        onDisconnect = { viewModel.onDisconnect() },
        onConnect = { wlanBean, pwd -> viewModel.onConnect(wlanBean, pwd) },
        onClearInteraction = { viewModel.onClearInteraction() }
    )
}

@Composable
fun SysConfigWlanInteraction(
    actionState: ActionState,
    onDisconnect: () -> Unit,
    onConnect: (WlanBean, String) -> Unit,
    onClearInteraction: () -> Unit,
) {
    if (actionState.event == SysConfigWlanViewModel.EVT_DISCON_CONFIRM) {
        AppConfirmDanger(
            visible = true,
            title = stringResource(id = R.string.disconnect_wlan_confirm),
            onCancel = { onClearInteraction() },
            onConfirm = { onDisconnect() }
        )
    } else if (actionState.event == SysConfigWlanViewModel.EVT_CON_CONFIRM) {
        val payload: WlanBean = actionState.payload as WlanBean
        AppWlanConnect(
            wlanBean = payload,
            visible = true,
            onCancel = { onClearInteraction() },
            onConnect = { wlanBean, pwd -> onConnect(wlanBean, pwd) }
        )
    }
}

@Composable
fun SysConfigWlanList(
    onDisconnectConfirm: (WlanBean) -> Unit,
    onConnectConfirm: (WlanBean) -> Unit,
    wlanList: List<WlanBean>,
) {
    Text(
        textAlign = TextAlign.Center,
        text = stringResource(id = R.string.sys_config_other_wlan_list),
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
    Spacer(modifier = Modifier.height(12.dp))
    AppList(
        modifier = Modifier.fillMaxSize(),
        records = wlanList
    ) { it ->
        AppFieldWrapper(text = it.ssid, labelWidth = 170.dp) { _ ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (it.hasPassword) {
                    Icon(
                        painter = painterResource(id = R.mipmap.wifi_icon_mm),
                        contentDescription = ""
                    )
                }

                Spacer(modifier = Modifier.width(15.dp))
                if (it.connected) {
                    AppFilledButton(
                        modifier = Modifier
                            .width(80.dp)
                            .height(30.dp),
                        onClick = { onDisconnectConfirm(it) },
                        textColor = primaryColor,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = dangerColor
                        ),
                        text = stringResource(id = R.string.btn_label_disconnect)
                    )
                } else {
                    AppFilledButton(
                        modifier = Modifier
                            .width(80.dp)
                            .height(30.dp),
                        onClick = { onConnectConfirm(it) },
                        text = stringResource(id = R.string.btn_label_connect)
                    )
                }
            }
        }
    }
}

@Composable
fun SysConfigWlanHeader(
    config: ConfigOtherBean,
    curWlan: WlanBean,
    onConfigUpdated: (ConfigOtherBean) -> Unit,
) {
    val format = stringResource(id = R.string.sys_config_other_wlan_cur)
    val disconnected = stringResource(id = R.string.sys_config_other_wlan_disconnected)
    val text =
        if (curWlan.ssid.isEmpty()) {
            String.format(format, disconnected)
        } else {
            val title = if (curWlan.connected) curWlan.ssid
            else disconnected
            String.format(format, title)
        }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 16.sp
        )

        AppSwitch(
            modifier = Modifier
                .width(60.dp)
                .height(30.dp),
            checked = config.wlan == "y"
        ) {
            onConfigUpdated(config.copy(wlan = if (it) "y" else "n"))
        }
    }
}


@Preview
@Composable
fun SysConfigWlanPreview() {
    val viewModel: SysConfigWlanViewModel = viewModel()
    LaunchedEffect(null) {
        viewModel.viewState.value = ViewState.LoadSuccess()
        viewModel.config.value = ConfigOtherBean.Empty
        val wlanList = ArrayList<WlanBean>()
        wlanList.add(WlanBean(ssid = "SZTM", connected = true))
        for (i in 1..10) {
            wlanList.add(WlanBean(ssid = "网络1245124545151518181185${i}", capabilities = "WPA"))
        }
//        viewModel.wlanList.value = wlanList
//        viewModel.actionState.value = ActionState(event = SysConfigWlanViewModel.EVT_DISCON_CONFIRM)
    }
    AppPreviewWrapper {
        Box(modifier = Modifier.fillMaxSize()) {
            SysConfigWlan(rememberNavController())
        }
    }
}