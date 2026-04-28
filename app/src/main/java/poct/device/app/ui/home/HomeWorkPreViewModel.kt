package poct.device.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.bean.ConfigSysBean
import poct.device.app.entity.User
import poct.device.app.entity.service.SysConfigService
import poct.device.app.serial.v2.ctl.CtlCommandsV2
import poct.device.app.serial.v2.ctl.CtlConstantsV2
import timber.log.Timber

class HomeWorkPreViewModel : ViewModel() {
    // 提示
    val tipVisible = MutableStateFlow(false)

    // 步骤
    val step = MutableStateFlow(1)

    // 进度 0-100
    val progress = MutableStateFlow(0)

    fun onReset() {
        step.value = 1
        progress.value = 0
    }

    // 步骤1：试剂确认
    fun onStep1Confirmed() {
        viewModelScope.launch {
            val sysConfig = withContext(Dispatchers.IO) {
                SysConfigService.findBean(ConfigSysBean.PREFIX, ConfigSysBean::class)
            }
            if (sysConfig.scan == "y" || sysConfig.scan.isEmpty()
                || AppParams.curUser.role != User.ROLE_DEV
            ) {
                // 检查是否有试剂卡遗留
                val hasCard = withContext(Dispatchers.IO) {
                    val gpioReadResult = CtlCommandsV2.readAllData(CtlCommandsV2.gpioRead())
                    Timber.d("gpioReadResult: $gpioReadResult")
                    CtlCommandsV2.gpioReadHasCard(gpioReadResult)
                }

                if (hasCard) {
                    tipVisible.value = true
                    return@launch
                }
            }
            step.value++
        }
    }

    // 步骤2：开始初始化
    fun onInitStarted() {
//        if (AppParams.devMock) {
//            step.value++
//            progress.value = 100
//            step.value++
//        } else {
        step.value++

        viewModelScope.launch {
            delay(500)

            val sysConfig = withContext(Dispatchers.IO) {
                SysConfigService.findBean(ConfigSysBean.PREFIX, ConfigSysBean::class)
            }
            if (sysConfig.scan == "y" || sysConfig.scan.isEmpty()
                || AppParams.curUser.role != User.ROLE_DEV
            ) {
                // 检查是否有试剂卡遗留
                val hasCard = withContext(Dispatchers.IO) {
                    val gpioReadResult = CtlCommandsV2.readAllData(CtlCommandsV2.gpioRead())
                    Timber.d("gpioReadResult: $gpioReadResult")
                    CtlCommandsV2.gpioReadHasCard(gpioReadResult)
                }

                if (hasCard) {
                    step.value--
                    step.value--
                    return@launch
                }
            }

            withContext(Dispatchers.IO) {
                CtlCommandsV2.readAllData(CtlCommandsV2.homing())
            }
            homingSuccess()

            App.getSerialHelper().reconnect()
        }
//        }
    }

    private fun homingSuccess() {
        CtlCommandsV2.processHomingStatus { homingSuccessCustomFunction(it) }
    }

    private fun homingSuccessCustomFunction(progressVal: Int) {
        viewModelScope.launch {
            progress.value = progressVal

            if (progressVal < CtlConstantsV2.CMD_ACTION_HOMING_STATUS_COMPLETED) {
                withContext(Dispatchers.IO) {
                    homingSuccess()
                }
            } else {
                // 负弹出，正进入
                withContext(Dispatchers.IO) {
                    val moveToSsResult =
                        CtlCommandsV2.readAllData(CtlCommandsV2.moveToSs(0, -88888, 10000, 1))
                    Timber.d("moveToSsResult: $moveToSsResult")

                    // 等待成功
                    CtlCommandsV2.waitMoveToSsStatusSuccess()

                    progress.value = 95

                    val moveDurationResult =
                        CtlCommandsV2.readAllData(CtlCommandsV2.moveDuration(0, 88888, 900))
                    Timber.w("moveDurationResult: $moveDurationResult")

                    // 等待成功
                    CtlCommandsV2.waitMoveDurationStatusSuccess()
                }

                progress.value = 100
                AppParams.initState = true
                step.value++
            }
        }
    }
}