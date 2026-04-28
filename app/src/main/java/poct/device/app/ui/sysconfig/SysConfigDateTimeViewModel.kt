package poct.device.app.ui.sysconfig

import android.icu.util.TimeZone
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import poct.device.app.App
import poct.device.app.R
import poct.device.app.bean.ConfigDateTimeBean
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.utils.app.AppLocalDateUtils
import poct.device.app.utils.app.AppSystemUtils
import poct.device.app.utils.app.AppTimeUtils

class SysConfigDateTimeViewModel : ViewModel() {
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    val actionState = MutableStateFlow(ActionState.Default)

    val mode = MutableStateFlow("view")

    val bean = MutableStateFlow(ConfigDateTimeBean.Empty)

    fun onLoad() {
        viewState.value = ViewState.LoadingOver()
        viewModelScope.launch(Dispatchers.IO) {
            val timeZone = TimeZone.getDefault()
            bean.value = ConfigDateTimeBean(
                timeSync = "n",
                timeZone = timeZone.id,
            )
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
    fun onBeanUpdate(newBean: ConfigDateTimeBean) {
        bean.value = newBean
    }

//    fun onDateTimeUpdate(dateTime: LocalDateTime) {
//        viewModelScope.launch(Dispatchers.IO) {
//            AppTimeUtils.setDateTime(dateTime)
//            bean.value = bean.value.copy(time = AppLocalDateUtils.formatDateTime(LocalDateTime.now()))
//        }
//    }
//
//    fun onTimeSyncUpdate(flag: String) {
//        viewModelScope.launch(Dispatchers.IO) {
//            AppTimeUtils.setAutoTimeZone(App.getContext(), if (flag == "y") 1 else 0)
//            bean.value = bean.value.copy(timeSync = flag)
//        }
//    }
//
//    fun onTimeZoneUpdate(zone: String) {
//        viewModelScope.launch(Dispatchers.IO) {
//            AppTimeUtils.setTimeZone(zone)
//        }
//    }

    fun onModifyPre() {
        mode.value = "modify"
    }

    fun onBack() {
        mode.value = "view"
    }

    fun onSaveConfirm() {
        actionState.value = ActionState(event = EVT_SAVE_CONFIRM)
    }

    fun onSave() {
        actionState.value = ActionState(
            event = EVT_LOADING,
            msg = App.getContext().getString(R.string.action_saving)
        )
        viewModelScope.launch(Dispatchers.IO) {
            // TODO
            AppTimeUtils.setDateTime(AppLocalDateUtils.parseDateTime(bean.value.time))
            AppTimeUtils.setAutoTimeZone(bean.value.timeSync == "y")
            AppTimeUtils.setTimeZone(bean.value.timeZone)
            delay(5000)
            mode.value = "view"
            onClearInteraction()
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