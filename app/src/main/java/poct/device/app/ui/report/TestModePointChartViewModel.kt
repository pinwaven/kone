package poct.device.app.ui.report

import android.content.pm.ActivityInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.bean.CaseBean
import poct.device.app.serial.v2.ctl.CtlCommandsV2
import poct.device.app.state.ViewState
import timber.log.Timber

class TestModePointChartViewModel : ViewModel() {
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    val bean = MutableStateFlow(CaseBean.Empty)
    val slopeRegions = MutableStateFlow<List<TestModeSlopeRegion>>(emptyList())
    val chartModelProducer = ChartEntryModelProducer()

    fun onLoad() {
        viewState.value = ViewState.LoadingOver()
        AppParams.curActivity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        val reportBean = AppParams.varReport
        bean.value = reportBean

        val points = TestModePointChartData.parsePoints(reportBean.workPoints)
        val regions = TestModePointChartData.findSlopeRegions(points)
        slopeRegions.value = regions
        chartModelProducer.setEntries(TestModePointChartData.toEntrySets(points, regions))
        viewState.value = ViewState.LoadSuccess()
    }

    fun onHome(callback: () -> Unit) {
        viewState.value = ViewState.LoadingOver(App.getContext().getString(R.string.work_ing_reset))
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // CtlCommandsV2.isWaitScanStatusSuccessCancel = true
                // CtlCommandsV2.isWaitAbsorbStatusSuccessCancel = true

                val cancelResult = CtlCommandsV2.readAllData(CtlCommandsV2.cancel())
                Timber.w("testMode chart home cancelResult: $cancelResult")

                val moveToSsResult =
                        CtlCommandsV2.readAllData(CtlCommandsV2.moveOut())
                Timber.w("testMode chart home moveToSsResult: $moveToSsResult")
                CtlCommandsV2.waitMoveToSsStatusSuccess()

                val moveDurationResult =
                        CtlCommandsV2.readAllData(CtlCommandsV2.closeDoor())
                Timber.w("testMode chart home moveDurationResult: $moveDurationResult")
                CtlCommandsV2.waitMoveDurationStatusSuccess()

                App.getSerialHelper().reconnect()
            }
            callback()
        }
    }
}
