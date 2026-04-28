package poct.device.app.ui.sysconfig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import poct.device.app.App
import poct.device.app.bean.ConfigOtherBean
import poct.device.app.bean.WlanBean
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import timber.log.Timber

class SysConfigWlanViewModel() : ViewModel() {
    // 视图状态
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    val actionState = MutableStateFlow(ActionState.Default)

    // 当前可用连接
//    val wlanList = MutableStateFlow<List<WlanBean>>(Collections.emptyList())
//    val curWlan = MutableStateFlow(WlanBean.Empty)

    val config = MutableStateFlow(ConfigOtherBean.Empty)

    fun onLoad() {
//        viewState.value = ViewState.LoadingOver()
//        viewModelScope.launch(Dispatchers.IO) {
//            config.value = ConfigOtherBean(wlan = if (AppParams.wlanEnabled) "y" else "n")
//            if (config.value.wlan != "y") {
//                viewState.value = ViewState.LoadSuccess()
//                return@launch
//            }
//            scanWlanList()
//        }
    }

    fun onConfigUpdated(newConfig: ConfigOtherBean) {
        Timber.d("wlan modify")
        // 这里并不保存，在广播回调里更改
        viewModelScope.launch(Dispatchers.IO) {
            if (newConfig.wlan == "y") {
                // 打开
                App.getWifiManager().setWifiEnabled(true)
            } else {
                // 关闭wifi
                App.getWifiManager().setWifiEnabled(false)
            }
        }
    }

//    private suspend fun scanWlanList() {
//        // 在广播回调里处理
//        AppWifiManager.openWifi(App.getWifiManager())
//        AppWifiManager.startScanWifi(App.getWifiManager())
//        delay(10000)
//        if (viewState.value is ViewState.LoadingOver) {
//            viewState.value = ViewState.LoadError(App.getContext().getString(R.string.msg_timeout))
//        }
//    }

    fun onClearInteraction() {
        actionState.value = ActionState.Default
    }

    fun onDisconnectConfirm(wlanBean: WlanBean) {
        actionState.value = ActionState(event = EVT_DISCON_CONFIRM, payload = wlanBean)
    }

    fun onDisconnect() {
//        viewModelScope.launch(Dispatchers.IO) {
//            AppWifiManager.disconnectNetwork(App.getWifiManager(), curWlan.value)
//            onClearInteraction()
//            scanWlanList()
//        }
    }

    fun onConnectConfirm(wlanBean: WlanBean) {
        actionState.value = ActionState(event = EVT_CON_CONFIRM, payload = wlanBean)
    }

    fun onConnect(wlanBean: WlanBean, pwd: String) {
//        viewModelScope.launch(Dispatchers.IO) {
//            AppWifiManager.connectWifi(
//                App.getWifiManager(),
//                wlanBean.ssid,
//                pwd,
//                wlanBean.passwordType
//            )
//            onClearInteraction()
//            scanWlanList()
//        }
    }

//    @Subscribe
//    fun handleWifiEvent(event: AppWifiEvent) {
//        Timber.d("wlan event")
//        val intent = event.intent
//        viewModelScope.launch(Dispatchers.IO) {
//            val curWlanInfo = AppWifiManager.getCurWifi(App.getWifiManager())
//            curWlan.value = curWlanInfo
//            AppParams.curWlan = curWlanInfo
//            wlanList.value = AppWifiManager.getWifiList(App.getWifiManager())
//                .map {
//                    it.connected = (curWlanInfo.connected && it.ssid == curWlanInfo.ssid)
//                    return@map it
//                }
//                .sortedWith { bean1, bean2 ->
//                    if (bean1.connected) {
//                        return@sortedWith -1
//                    }
//                    if (bean2.connected) {
//                        return@sortedWith 1
//                    }
//                    val strengthDx = bean2.signalStrength - bean1.signalStrength
//                    if (strengthDx == 0) {
//                        return@sortedWith bean1.ssid.lowercase()
//                            .compareTo(bean2.ssid.lowercase())
//                    }
//                    return@sortedWith strengthDx
//                }
//            Timber.w("=====intent%s", intent.action)
//            if (WifiManager.WIFI_STATE_CHANGED_ACTION == intent.action) {
//                Timber.d("wlan state change")
//                val wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0)
//                if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
//                    val bean = config.value.copy(wlan = "y")
//                    config.value = bean
//                    SysConfigService.saveBean(ConfigOtherBean.PREFIX, bean)
//                    scanWlanList()
//                } else {
//                    val bean = config.value.copy(wlan = "n")
//                    config.value = bean
//                    SysConfigService.saveBean(ConfigOtherBean.PREFIX, bean)
//                    wlanList.value = Collections.emptyList()
//                    curWlan.value = WlanBean.Empty
//                }
//            } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION == intent.action) {
//                viewState.value = ViewState.LoadingOver()
//
//                viewState.value = ViewState.LoadSuccess()
//            } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION == intent.action) {
//                val intExtra = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 0)
//                if (intExtra == WifiManager.ERROR_AUTHENTICATING) {
//                    //密码错误
//                    // 在子线程中调用Looper.prepare()和Looper.loop()
//                    Looper.prepare()
//                    AppToastUtil.shortShow(App.getContext().getString(R.string.msg_wrong_password))
//                    Looper.loop() // 保持当前线程运行，直到调用Looper.quit()
//                }
//            } else if (WifiManager.NETWORK_STATE_CHANGED_ACTION == intent.action) {
//                val nf = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
//                if (nf != null) {
//                    if (nf.detailedState == NetworkInfo.DetailedState.BLOCKED) {
//                        //密码错误
//                        Looper.prepare()
//                        AppToastUtil.shortShow(
//                            App.getContext().getString(R.string.msg_wrong_password)
//                        )
//                        Looper.loop() // 保持当前线程运行，直到调用Looper.quit()
//                    }
//                }
//            }
//        }
//    }

    companion object {
        const val EVT_DISCON_CONFIRM = "dscConfirm"
        const val EVT_CON_CONFIRM = "conConfirm"
    }
}