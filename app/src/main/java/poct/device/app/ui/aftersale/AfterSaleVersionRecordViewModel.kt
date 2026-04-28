package poct.device.app.ui.aftersale

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.szyh.comm.CommService
import info.szyh.comm.bean.CommEnsUpgradeBean
import info.szyh.comm.constant.CommEnsTaskType
import info.szyh.comm.utils.CommDateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import poct.device.app.App
import poct.device.app.R
import poct.device.app.bean.VersionBean
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.utils.app.AppUpgradeUtils
import timber.log.Timber
import java.util.Collections

class AfterSaleVersionRecordViewModel : ViewModel() {
    // 视图状态
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    val actionState = MutableStateFlow(ActionState.Default)

    // 记录列表
    val records = MutableStateFlow<List<VersionBean>>(Collections.emptyList())

    fun onLoad() {
        viewState.value = ViewState.LoadingOver()
        viewModelScope.launch(Dispatchers.IO) {
            records.value =
                CommService.instance().getTaskByType(CommEnsTaskType.UPGRADE).map { task ->
                    val software: CommEnsUpgradeBean? =
                        task.upgradeList.filter { it.type == CommEnsUpgradeBean.TYPE_APP }
                            .getOrNull(0)
                    val hardware: CommEnsUpgradeBean? =
                        task.upgradeList.filter { it.type == CommEnsUpgradeBean.TYPE_SYS }
                            .getOrNull(0)
                    return@map VersionBean(
                        id = task.taskId,
                        software = software?.targetVersion ?: "",
                        hardware = hardware?.targetVersion ?: "",
                        softwareRemark = software?.appRemark ?: "",
                        hardwareRemark = hardware?.sysRemark ?: "",
                        lapseTime = try {
                            CommDateUtils.formatGmtDate(CommDateUtils.pauseDateTime(task.lapseTime))
                        } catch (e: Exception) {
                            Timber.tag("upgrade").e(e.message ?: "unknown reason")
                            ""
                        },
                        handleTime = try {
                            CommDateUtils.formatGmtDate(CommDateUtils.pauseDateTime(task.time))
                        } catch (e: Exception) {
                            Timber.tag("upgrade").e(e.message ?: "unknown reason")
                            ""
                        },
                        state = task.status
                    )
                }
            delay(300)
            viewState.value = ViewState.LoadSuccess()
        }
    }

    fun onClearInteraction() {
        actionState.value = ActionState.Default
    }

    fun onUpgrade(bean: VersionBean) {
        actionState.value =
            ActionState(
                EVT_UPGRADE_ING,
                App.getContext().getString(R.string.upgrading_version),
                payload = bean
            )
        viewModelScope.launch(Dispatchers.IO) {
            delay(300)
            AppUpgradeUtils.upgrade(bean.version)
            actionState.value = ActionState(
                event = AfterSaleVersionUpgradeViewModel.EVT_UPGRADE,
                msg = App.getContext().getString(
                    R.string.upgrade_version_success
                )
            )
        }
    }

    fun onHandleVersion(bean: VersionBean) {
        actionState.value = ActionState(event = EVT_HANDLE, payload = bean)
    }

    fun onViewVersion(bean: VersionBean) {
        actionState.value = ActionState(event = EVT_DETAIL, payload = bean)
    }

    companion object {
        /**
         * 查看处理升级
         */
        const val EVT_DETAIL = "detail"

        /**
         * 查看处理升级
         */
        const val EVT_HANDLE = "handle"

        /**
         * 执行升级
         */
        const val EVT_UPGRADE_ING = "upgradeIng"

        /**
         * 执行升级
         */
        const val EVT_UPGRADE_DONE = "upgradeDone"
    }

}