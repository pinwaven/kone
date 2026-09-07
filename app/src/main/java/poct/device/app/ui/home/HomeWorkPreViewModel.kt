package poct.device.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import timber.log.Timber

class HomeWorkPreViewModel : ViewModel() {
    // 提示
    val tipVisible = MutableStateFlow(false)

    // 步骤
    val step = MutableStateFlow(1)

    // 进度 0-100
    val progress = MutableStateFlow(0)

    private var initJob: Job? = null

    fun onReset() {
        step.value = 1
        progress.value = 0
    }

    // 退出初始化：停止轮询并向硬件发送取消指令
    fun onExit() {
        initJob?.cancel()
        initJob = null
        viewModelScope.launch(Dispatchers.IO) {
            val cancelResult = CtlCommandsV2.readAllData(CtlCommandsV2.cancel())
            Timber.w("init exit cancelResult: $cancelResult")
        }
        onReset()
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

        initJob = viewModelScope.launch {
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

            if (!runInitSequence()) {
                // 归零/移动多次重试仍失败：回到起始步骤，允许用户重试初始化
                Timber.e("init mechanical sequence failed after retries; resetting")
                onReset()
                return@launch
            }

            progress.value = 100
            AppParams.initState = true
            step.value++

            App.getSerialHelperOrNull()?.reconnect()
        }
    }

    /**
     * 初始化机械动作序列：归零 -> 弹出 -> 关门，每步带归零重试。
     * 任一步多次重试仍失败返回 false，由调用方重置并让用户重试。
     */
    private suspend fun runInitSequence(): Boolean = withContext(Dispatchers.IO) {
        if (!CtlCommandsV2.homingWithRetry { progress.value = it }) return@withContext false

        // 负弹出，正进入
        if (!CtlCommandsV2.moveOutWithRetry()) return@withContext false
        progress.value = 95

        CtlCommandsV2.closeDoorWithRetry()
    }
}