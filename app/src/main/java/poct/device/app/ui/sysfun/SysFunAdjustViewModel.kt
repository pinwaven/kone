package poct.device.app.ui.sysfun

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import poct.device.app.App
import poct.device.app.R
import poct.device.app.bean.ConfigAdjustBean
import poct.device.app.entity.service.SysConfigService
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import timber.log.Timber

class SysFunAdjustViewModel : ViewModel() {
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    val actionState = MutableStateFlow(ActionState.Default)

    val mode = MutableStateFlow("view")
    val checkState = MutableStateFlow("start")

    val bean = MutableStateFlow(ConfigAdjustBean.Empty)

    fun onLoad() {
        viewState.value = ViewState.LoadingOver()
        viewModelScope.launch(Dispatchers.IO) {
            val configBean =
                SysConfigService.findBean(ConfigAdjustBean.PREFIX, ConfigAdjustBean::class)
            bean.value = configBean
            Timber.w("=====查询BEAN=====${App.gson.toJson(bean.value)}")
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

    fun onBeanUpdate(newBean: ConfigAdjustBean) {
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
            event = EVT_LOADING,
            msg = App.getContext().getString(R.string.action_saving)
        )
        viewModelScope.launch(Dispatchers.IO) {
//            App.getCtlSerialService().send(
//                CtlCommandsV2.adjustConfig(bean.value),
//                object : SerialMessageCallbackAdapterV2<CtlSerialMessageV2>() {
//                    override suspend fun error(
//                        sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                        e: Exception?,
//                        scope: CoroutineScope,
//                    ) {
//                        // TODO 故障
//                        actionState.value = ActionState(
//                            event = EVT_ERROR,
//                            msg = App.getContext().getString(R.string.sys_fun_sbjz_config_error)
//                        )
//                        onClearInteraction()
//                    }
//
//                    override suspend fun success(
//                        feedback: CtlSerialMessageV2,
//                        sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                        scope: CoroutineScope,
//                    ) {
//                        SysConfigService.saveBean(ConfigAdjustBean.PREFIX, bean.value)
//                        actionState.value = ActionState(EVT_SAVE_DONE)
//                    }
//                }
//            )
        }
        mode.value = "view"
    }

    fun onStartCheck() {
        checkState.value = "stop"
        viewModelScope.launch(Dispatchers.IO) {
//            App.getCtlSerialService().send(
//                CtlCommandsV2.startAdjust(),
//                object : SerialMessageCallbackAdapterV2<CtlSerialMessageV2>() {
//                    override suspend fun delay(
//                        feedback: CtlSerialMessageV2,
//                        sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                        scope: CoroutineScope,
//                    ) {
//                        Timber.d("start adjust delay")
//                        actionState.value = ActionState(event = EVT_START)
//                        // 激光强度信息
//                        val laserStrength =
//                            feedback.paramData.getParameter(CtlConstantsV2.PARAM_LASER)
//                        Timber.w("=====激光强度信息=====${laserStrength}")
//                        if (laserStrength != null) {
//                            val curBean = bean.value
//                            curBean.jgName = laserStrength
//                            bean.value = curBean
//                        }
//                        actionState.value = ActionState(event = EVT_END)
//                    }
//
//                    override suspend fun error(
//                        sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                        e: Exception?,
//                        scope: CoroutineScope,
//                    ) {
//                        // TODO 硬件故障
//                        actionState.value = ActionState(
//                            event = EVT_ERROR,
//                            msg = App.getContext().getString(R.string.sys_fun_sbjz_start_error)
//                        )
//                        onClearInteraction()
//                    }
//
//                    override suspend fun success(
//                        feedback: CtlSerialMessageV2,
//                        sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                        scope: CoroutineScope,
//                    ) {
//                        Timber.d("start adjust done")
//                        actionState.value = ActionState.Default
//                        Timber.w("=====成功时保存=====${App.gson.toJson(bean.value)}")
//                        onSave()
//                    }
//                })
        }
    }

    fun onStopCheck() {
        checkState.value = "start"
        viewModelScope.launch(Dispatchers.IO) {
//            App.getCtlSerialService().send(
//                CtlCommandsV2.stopAdjust(),
//                object : SerialMessageCallbackAdapterV2<CtlSerialMessageV2>() {
//                    override suspend fun delay(
//                        feedback: CtlSerialMessageV2,
//                        sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                        scope: CoroutineScope,
//                    ) {
//                        Timber.d("stop adjust delay")
//                    }
//
//                    override suspend fun error(
//                        sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                        e: Exception?,
//                        scope: CoroutineScope,
//                    ) {
//                        // TODO 硬件故障
//                        actionState.value = ActionState(
//                            event = EVT_ERROR,
//                            msg = App.getContext().getString(R.string.sys_fun_sbjz_stop_error)
//                        )
//                        onClearInteraction()
//                    }
//
//                    override suspend fun success(
//                        feedback: CtlSerialMessageV2,
//                        sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                        scope: CoroutineScope,
//                    ) {
//                        Timber.d("stop adjust done")
//                    }
//                })
        }
    }

    companion object {
        const val EVT_EXIT = "exit"
        const val EVT_LOADING = "loading"
        const val EVT_ERROR = "error"
        const val EVT_SAVE_DONE = "saveDone"
        const val EVT_START = "start"
        const val EVT_END = "end"
    }

}