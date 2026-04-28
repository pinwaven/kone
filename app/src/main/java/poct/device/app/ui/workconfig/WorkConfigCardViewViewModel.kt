package poct.device.app.ui.workconfig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import poct.device.app.AppParams
import poct.device.app.bean.CardConfigBean
import poct.device.app.state.ViewState

class WorkConfigCardViewViewModel : ViewModel() {
    // 视图状态
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    var stepState = MutableStateFlow(STEP_INFO)

    // 记录
    val bean = MutableStateFlow(CardConfigBean.Empty)

    fun onLoad() {
        viewState.value = ViewState.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            bean.value = AppParams.varCardConfigForPreview

            delay(10)
            viewState.value = ViewState.LoadSuccess()
        }
    }

    fun onInfo() {
        stepState.value = STEP_INFO
    }

    fun onTop() {
        stepState.value = STEP_TOP
    }

    fun onVar() {
        stepState.value = STEP_VAR
    }

    companion object {
        const val STEP_INFO = "info"
        const val STEP_TOP = "top"
        const val STEP_VAR = "var"
    }
}