package poct.device.app.ui.sysfun

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import poct.device.app.App
import poct.device.app.R
import poct.device.app.bean.ConfigInfoBean
import poct.device.app.bean.ConfigInfoV2Bean
import poct.device.app.entity.service.SysConfigService
import poct.device.app.serial.v2.ctl.CtlCommandsV2
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.thirdparty.SbEdgeFunc
import poct.device.app.ui.sysconfig.SysConfigSysViewModel

class SysFunInfoViewModel : ViewModel() {
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    val actionState = MutableStateFlow(ActionState.Default)

    val mode = MutableStateFlow("view")

    val bean = MutableStateFlow(ConfigInfoBean.Empty)

    // TODO 简化信息
    var beanV2 = MutableStateFlow(ConfigInfoV2Bean.Empty)

    fun onLoad() {
        viewState.value = ViewState.LoadingOver()
        viewModelScope.launch {
            val hiResult = withContext(Dispatchers.IO) {
                CtlCommandsV2.readAllData(CtlCommandsV2.hi())
            }
            if (hiResult.isNotEmpty()) {
                val versionData = hiResult.split("ver:")
                if (versionData.size > 1) {
                    val versions = versionData[1]
                    val versionArray = versionData[1].split("~")
                    var version = versions
                    if (versionArray.size > 1) {
                        version = versionArray[0]
                    }

                    var configInfo = withContext(Dispatchers.IO) {
                        SysConfigService.findBean(
                            ConfigInfoBean.PREFIX,
                            ConfigInfoBean::class
                        )
                    }
                    configInfo.hardware = version

                    configInfo = withContext(Dispatchers.IO) {
                        SysConfigService.reportVersion(version)
                    }
                    bean.value = configInfo
                    viewState.value = ViewState.LoadSuccess()
                }
            }
        }
    }

    fun onLoadV2() {
        viewModelScope.launch(Dispatchers.IO) {
            viewState.value = ViewState.LoadingOver()

            val configBean =
                SysConfigService.findBean(ConfigInfoBean.PREFIX, ConfigInfoV2Bean::class)

            if (configBean.hasData()) {
                beanV2.value = configBean
            } else {
                // 获取设备ID
                val deviceId: String = App.getDeviceId()
                beanV2.value = SbEdgeFunc.getDeviceConfig(deviceId)
                if (!beanV2.value.hasData()) {
                    actionState.value = ActionState(event = EVT_CONTACT_ADMIN)
                } else {
                    SysConfigService.saveBean(ConfigInfoBean.PREFIX, beanV2.value)
                }
            }

            viewState.value = ViewState.LoadSuccess()
        }
    }

    fun onClearInteraction() {
        actionState.value = ActionState.Default
    }

    // 退出确认
    fun onBackConfirm() {
        actionState.value = ActionState(event = EVT_EXIT)
    }

    fun onBeanUpdate(newBean: ConfigInfoBean) {
        bean.value = newBean
    }

    fun onModifyPre() {
        mode.value = "modify"
    }

    fun onBack() {
        mode.value = "view"
    }

    fun onSave() {
        actionState.value = ActionState(
            event = SysConfigSysViewModel.EVT_LOADING,
            msg = App.getContext().getString(R.string.action_saving)
        )
        viewModelScope.launch(Dispatchers.IO) {
//            App.getCtlSerialService().send(
//                CtlCommandsV2.setSysInfo(bean.value.code),
//                object : SerialMessageCallbackAdapterV2<CtlSerialMessageV2>() {
//                    override suspend fun error(
//                        sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                        e: Exception?,
//                        scope: CoroutineScope,
//                    ) {
//                        // TODO 故障
//                        actionState.value = ActionState(SysConfigSysViewModel.EVT_SAVE_DONE)
//                        withContext(Dispatchers.Main) {
//                            AppToastUtil.shortShow("Error")
//                        }
//                    }
//
//                    override suspend fun success(
//                        feedback: CtlSerialMessageV2,
//                        sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                        scope: CoroutineScope,
//                    ) {
//                        SysConfigService.saveBean(ConfigInfoBean.PREFIX, bean.value)
//                        actionState.value = ActionState(SysConfigSysViewModel.EVT_SAVE_DONE)
//                    }
//                }
//            )
        }
        mode.value = "view"
    }

    companion object {
        const val EVT_EXIT = "exit"
        const val EVT_LOADING = "loading"
        const val EVT_SAVE_DONE = "saveDone"
        const val EVT_CONTACT_ADMIN = "contactAdmin"
    }

}