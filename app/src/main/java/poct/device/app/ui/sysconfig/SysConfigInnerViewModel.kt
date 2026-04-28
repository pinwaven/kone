package poct.device.app.ui.sysconfig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import poct.device.app.App
import poct.device.app.R
import poct.device.app.bean.ConfigInnerBean
import poct.device.app.entity.service.SysConfigService
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState

class SysConfigInnerViewModel : ViewModel() {
    // 视图状态
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    val actionState = MutableStateFlow(ActionState.Default)

    val mode = MutableStateFlow("view")

    val bean = MutableStateFlow(ConfigInnerBean.Empty)

    fun onLoad() {
        viewState.value = ViewState.LoadingOver()
        viewModelScope.launch(Dispatchers.IO) {
            bean.value = SysConfigService.findBean(ConfigInnerBean.PREFIX, ConfigInnerBean::class)
            viewState.value = ViewState.LoadSuccess()
        }
    }

    fun onClearInteraction() {
        actionState.value = ActionState.Default
    }
    // 退出确认
    fun onBackConfirm() {
        actionState.value = ActionState(event= EVT_EXIT)
    }
    fun onBeanUpdate(newBean: ConfigInnerBean) {
        bean.value = newBean
    }

    fun onModifyPre() {
        mode.value = "modify"
    }

    fun onBack() {
        mode.value = "view"
    }

    fun onSave() {
        actionState.value =
            ActionState(
                event = SysConfigSysViewModel.EVT_LOADING,
                msg = App.getContext().getString(R.string.action_saving)
            )
        viewModelScope.launch(Dispatchers.IO) {
            SysConfigService.saveBean(ConfigInnerBean.PREFIX, bean.value)
            actionState.value = ActionState(SysConfigSysViewModel.EVT_SAVE_DONE)
        }
        mode.value = "view"
    }

    companion object {
        const val EVT_EXIT = "exit"
        const val EVT_LOADING = "loading"
        const val EVT_SAVE_DONE = "saveDone"
    }
}