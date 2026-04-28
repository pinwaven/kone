package poct.device.app.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.szyh.common4.android.EventUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import poct.device.app.AppParams
import poct.device.app.bean.CaseBean
import poct.device.app.event.AppPdfPrintEvent
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState


class ReportPDFViewModel : ViewModel() {
    // 视图状态
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    val actionState = MutableStateFlow(ActionState.Default)

    val record = MutableStateFlow(CaseBean.Empty)

    fun onLoad() {
        viewState.value = ViewState.LoadingOver()
        viewModelScope.launch(Dispatchers.IO) {
            record.value = AppParams.varReport
            viewState.value = ViewState.LoadSuccess()
        }
    }

    fun onClearInteraction() {
        actionState.value = ActionState.Default
    }

    fun onLisConfirm() {
        actionState.value = ActionState(EVT_LIS_CONFIRM)
    }

    fun onPrintConfirm() {
        actionState.value = ActionState(EVT_PRINT_CONFIRM)
    }

    fun onPrint(pdfPath: String) {
//        if(!AppParams.wlanEnabled) {
//            actionState.value = ActionState(EVT_REPORT_ERROR, App.getContext().getString(R.string.wlan_not_connect))
//            return
//        }
        onClearInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            EventUtils.publishEvent(AppPdfPrintEvent(pdfPath))
            actionState.value = ActionState(EVT_PRINT_CONFIRM_AFTER)
        }
    }

    companion object {
        const val EVT_LIS_CONFIRM = "lisConfirm"
        const val EVT_PRINT_CONFIRM = "printConfirm"
        const val EVT_PRINT_CONFIRM_AFTER = "printConfirmAfter"
        const val EVT_REPORT_ERROR = "evtError"
    }
}