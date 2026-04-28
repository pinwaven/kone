package poct.device.app.ui.setting

import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.RouteConfig
import poct.device.app.bean.ConfigInfoV2Bean
import poct.device.app.state.ViewState
import poct.device.app.thirdparty.SbEdgeFunc

class SettingMainViewModel : ViewModel() {
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    val workPreVisible = MutableStateFlow(false)
    val confirmVisible = MutableStateFlow(false)
    val onConfirmAction = MutableStateFlow<(String) -> Unit>({ _ -> })

    fun onLoad() {
        viewState.value = ViewState.LoadSuccess()
    }

    fun onWorkConfigCard(navController: NavController) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isDeviceRegister()) {
                withContext(Dispatchers.Main) {
                    confirmVisible.value = true
                    onConfirmAction.value = { pwd ->
                        viewModelScope.launch(Dispatchers.IO) {
                            if (checkDevicePwd(pwd)) {
                                withContext(Dispatchers.Main) { // 切回主线程
                                    confirmVisible.value = false
                                    navController.navigate(RouteConfig.WORK_CONFIG_CARD)
                                }
                            }
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) { // 切回主线程
                    confirmVisible.value = false
                    navController.navigate(RouteConfig.WORK_CONFIG_CARD)
                }
            }
        }
    }

    fun onHoming() {
        viewModelScope.launch(Dispatchers.IO) {
            if (isDeviceRegister()) {
                withContext(Dispatchers.Main) {
                    confirmVisible.value = true
                    onConfirmAction.value = { pwd ->
                        viewModelScope.launch(Dispatchers.IO) {
                            if (checkDevicePwd(pwd)) {
                                withContext(Dispatchers.Main) {
                                    workPreVisible.value = true
                                    confirmVisible.value = false
                                }
                            }
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    workPreVisible.value = true
                    confirmVisible.value = false
                }
            }
        }
    }

    fun onAdvancedOpt(navController: NavController) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isDeviceRegister()) {
                withContext(Dispatchers.Main) {
                    confirmVisible.value = true
                    onConfirmAction.value = { pwd ->
                        viewModelScope.launch(Dispatchers.IO) {
                            if (checkDevicePwd(pwd)) {
                                withContext(Dispatchers.Main) { // 切回主线程
                                    confirmVisible.value = false
                                    navController.navigate(RouteConfig.SAMPLE_SERIAL)
                                }
                            }
                        }
                    }
                }
            } else {
                withContext(Dispatchers.Main) { // 切回主线程
                    confirmVisible.value = false
                    navController.navigate(RouteConfig.SAMPLE_SERIAL)
                }
            }
        }
    }

    fun sysWifiConfig() {
        viewModelScope.launch(Dispatchers.IO) {
            val activity = AppParams.curActivity
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            activity?.startActivityForResult(intent, 1)
        }
    }

    suspend fun isDeviceRegister(): Boolean {
        viewState.value = ViewState.LoadingOver("正在检查设备信息…")
        val deviceConfigInfo: ConfigInfoV2Bean = withContext(Dispatchers.IO) {
            SbEdgeFunc.getDeviceConfig(App.getDeviceId())
        }
        viewState.value = ViewState.LoadSuccess()
        return deviceConfigInfo.code != SbEdgeFunc.EMPTY_VAL
    }

    suspend fun checkDevicePwd(pwd: String): Boolean {
        viewState.value = ViewState.LoadingOver("验证中…")
        val result = withContext(Dispatchers.IO) {
            SbEdgeFunc.checkDevicePwd(App.getDeviceId(), pwd)
        }

//        var result = false
//        if (pwd != AppParams.curUser.pwd) {
//            withContext(Dispatchers.Main) { // 切回主线程
//                AppToastUtil.shortShow("密码错误")
//            }
//        } else {
//            result = true
//        }
        viewState.value = ViewState.LoadSuccess()
        return result
    }
}