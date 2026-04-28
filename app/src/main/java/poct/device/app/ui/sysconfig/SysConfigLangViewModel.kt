package poct.device.app.ui.sysconfig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import poct.device.app.App
import poct.device.app.R
import poct.device.app.bean.ConfigOtherBean
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.utils.app.AppLangUtils
import poct.device.app.utils.app.AppSystemUtils

class SysConfigLangViewModel : ViewModel() {
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    val actionState = MutableStateFlow(ActionState.Default)

    val mode = MutableStateFlow("view")

    val bean = MutableStateFlow(ConfigOtherBean.Empty)

    fun onLoad() {
        viewState.value = ViewState.LoadingOver()
        viewModelScope.launch(Dispatchers.IO) {
            bean.value = ConfigOtherBean(lang = AppLangUtils.getLanguage(App.getContext()))
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

    fun onBeanUpdate(newBean: ConfigOtherBean) {
        bean.value = newBean
    }

    fun onModifyPre() {
        mode.value = "modify"
    }

    fun onBack() {
        mode.value = "view"
    }

    fun onSaveConfirm() {
        if (bean.value.lang == AppLangUtils.getLanguage(App.getContext())) {
            onClearInteraction()
        } else {
            actionState.value = ActionState(EVT_SAVE_CONFIRM)
        }
    }

    fun onSave() {
        actionState.value = ActionState(
            event = EVT_LOADING,
            msg = App.getContext().getString(R.string.action_saving)
        )

        viewModelScope.launch(Dispatchers.IO) {
            AppLangUtils.setLanguage(App.getContext(), bean.value.lang)
            delay(5000)
            withContext(Dispatchers.Main) {
                AppSystemUtils.restartApp()
            }
        }
    }

    companion object {
        const val EVT_EXIT = "exit"
        const val EVT_LOADING = "loading"
        const val EVT_SAVE_CONFIRM = "saveConfirm"
    }
}