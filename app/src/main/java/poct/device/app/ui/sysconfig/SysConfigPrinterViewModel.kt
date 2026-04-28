package poct.device.app.ui.sysconfig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import poct.device.app.App
import poct.device.app.R
import poct.device.app.bean.PrinterInfo
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState

class SysConfigPrinterViewModel : ViewModel() {
    // 视图状态
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    val actionState = MutableStateFlow(ActionState.Default)

    val printer = MutableStateFlow<PrinterInfo>(PrinterInfo.Empty)

    // 字段
    val name = MutableStateFlow("")


    fun onLoad() {
        viewState.value = ViewState.LoadSuccess()
    }

    fun onSave() {
        viewState.value = ViewState.Loading(App.getContext().getString(R.string.action_saving))
        viewModelScope.launch {
            delay(3000)
            viewState.value = ViewState.LoadSuccess()
        }
    }
}