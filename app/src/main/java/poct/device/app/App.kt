package poct.device.app

import android.app.Application
import android.hardware.usb.UsbManager
import android.net.wifi.WifiManager
import android.os.SystemClock
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import poct.device.app.bean.ConfigInfoBean
import poct.device.app.bean.ConfigInfoV2Bean
import poct.device.app.bean.ConfigSysBean
import poct.device.app.entity.service.SysConfigService
import poct.device.app.serial.v2.SerialHelperV2
import poct.device.app.serial.v2.ctl.CtlCommandsV2
import poct.device.app.thirdparty.NanoApi
import poct.device.app.thirdparty.NanoAuthStore
import poct.device.app.thirdparty.model.nano.NanoAuthSupport
import poct.device.app.utils.app.AppBatteryReceiverHelper
import poct.device.app.utils.app.AppLangUtils
import poct.device.app.utils.app.AppSystemUtils
import poct.device.app.utils.app.BoardPowerGuard
import poct.device.app.utils.app.DeviceIdUtils
import timber.log.Timber

/**
 * 主板上电尝试的触发来源，用于日志区分。
 */
enum class BoardPowerAttemptReason {
    APP_START,
    CHARGER_PLUGGED,
    SCREEN_ON_RECOVERY,
}

/**
 * *
 *
 *
 * @desc：application
 */
class App : Application() {
    companion object {
        private var context: App? = null
        private var wifiManager: WifiManager? = null
        private var usbManager: UsbManager? = null
        private var serialHelper: SerialHelperV2? = null

        val gson = GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create()

        fun getDeviceId(): String {
            return DeviceIdUtils.getSn(getContext())
        }

        fun getSerialHelper(): SerialHelperV2 {
            return serialHelper!!
        }

        fun getContext(): App {
            return context!!
        }

        fun getDatabase(): AppDatabase {
            return AppDatabase.getDatabase(getContext())
        }

        fun getWifiManager(): WifiManager {
            return (wifiManager ?: synchronized(this) {
                val instance = getContext().getSystemService(WIFI_SERVICE)
                wifiManager = instance as WifiManager?
                instance
            }) as WifiManager
        }

        fun getUsbManager(): UsbManager {
            return (usbManager ?: synchronized(this) {
                val instance = getContext().getSystemService(USB_SERVICE)
                usbManager = instance as UsbManager?
                instance
            }) as UsbManager
        }
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val boardPowerAttemptMutex = Mutex()
    private var lastBoardPowerAttemptAt = 0L

    override fun onCreate() {
        super.onCreate()

        context = this
        // 多语言
        AppLangUtils.init(this)
        // 日志
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }

        PDFBoxResourceLoader.init(this)

        appScope.launch {
            val snapshot = AppBatteryReceiverHelper.readRawBatteryOnce(this@App)
            val decision = BoardPowerGuard.decide(snapshot.percent, snapshot.plugged)
            when (decision) {
                is BoardPowerGuard.Decision.BlockNeedCharger -> {
                    Timber.w(
                        "board power guard: blocking board power-on at startup, agingWarn=%s",
                        decision.agingWarn
                    )
                    AppParams.setBoardPowerBlocked(blocked = true, agingWarn = decision.agingWarn)
                }
                BoardPowerGuard.Decision.ProceedNormal -> {
                    attemptBoardPowerOn(BoardPowerAttemptReason.APP_START)
                }
            }

            // 物联网连接（不依赖主板，独立于 guard 决策执行）
            val configInfo =
                SysConfigService.findBean(ConfigInfoBean.PREFIX, ConfigInfoBean::class)
            Timber.d(configInfo.code)
            // TODO 物联网先不调试
            //            Timber.d("日志测试")
            //            val prefix =
            //                Environment.getExternalStorageDirectory().absolutePath + File.separator + "Android" + File.separator + "Nanovate_AI"
            //            CommService.instance().startIot("192.168.1.16", 7300, 9100, "TM-YG01-00046", prefix)
        }
    }

    /**
     * 给主板上电、打开串口、握手确认的唯一入口。所有触发场景（开机、充电器插入重试、
     * 屏幕唤醒恢复）都必须走这里，不能再散落直接调用 powerOnCtlBoard()/openSerialPort()。
     *
     * 用 mutex + 2 秒防抖避免插拔充电器抖动导致短时间内重复上电尝试。
     */
    fun attemptBoardPowerOn(reason: BoardPowerAttemptReason) {
        appScope.launch {
            boardPowerAttemptMutex.withLock {
                val now = SystemClock.elapsedRealtime()
                if (now - lastBoardPowerAttemptAt < BoardPowerGuard.RETRY_DEBOUNCE_MS) {
                    Timber.w("skip board power attempt: debounced, reason=%s", reason)
                    return@withLock
                }
                lastBoardPowerAttemptAt = now

                val snapshot = AppBatteryReceiverHelper.readRawBatteryOnce(this@App)
                if (!BoardPowerGuard.markPending(snapshot.percent)) {
                    Timber.e("board power guard markPending failed, proceed without guard")
                }

                openCtlBoardBlocking()
                // 等待供电完成
                delay(2000)
                openSerialPort()
                // power 板上电后第一次请求可能会CRC报错，先poll一次
                CtlCommandsV2.readAllData(CtlCommandsV2.poll())

                val hiResult = CtlCommandsV2.readAllData(CtlCommandsV2.hi())
                if (!BoardPowerGuard.isValidHiResult(hiResult)) {
                    Timber.w("board power handshake failed, reason=%s, hiResult=%s", reason, hiResult)
                    // 握手失败但进程未崩溃：当前session也要锁主流程，不能让流程
                    // 在主板未确认可用的情况下继续跑；等下次重试握手成功再解锁
                    AppParams.setBoardPowerBlocked(blocked = true, agingWarn = false)
                    return@withLock
                }

                // 启动时静默上报软件版本与固件版本
                reportVersionsSilently(hiResult)

                launch {
                    delay(BoardPowerGuard.STABLE_DELAY_MS)
                    BoardPowerGuard.markConfirmedStable()
                    AppParams.setBoardPowerBlocked(blocked = false, agingWarn = false)
                    Timber.w("board power confirmed stable, reason=%s", reason)
                }
            }
        }
    }

    private suspend fun openCtlBoardBlocking() {
        AppSystemUtils.powerOffCtlBoard()
        delay(500)
        AppSystemUtils.powerOnCtlBoard()
    }

    /**
     * 启动时静默上报软件版本与固件版本。
     *
     * 尽力而为：无 UI 提示，失败仅记录日志。仅在 Nano 流程下执行
     * （此时才配置了上报接口）。
     *
     * hiResult 由调用方（attemptBoardPowerOn）握手成功后传入，避免重复发一次
     * hi() 串口指令。
     */
    private suspend fun reportVersionsSilently(hiResult: String) {
        try {
            val sysConfig = SysConfigService.findBean(ConfigSysBean.PREFIX, ConfigSysBean::class)
            if (!ConfigSysBean.isNanoFlow(sysConfig.flow)) {
                Timber.d("skip silent version report: not nano flow")
                return
            }

            val firmwareVersion = NanoAuthSupport.extractFirmwareVersion(hiResult)
            val firmwareId = NanoAuthSupport.extractFirmwareId(hiResult)

            // 刷新本地缓存的固件版本
            if (firmwareVersion.isNotBlank()) {
                val configBean =
                    SysConfigService.findBean(ConfigInfoBean.PREFIX, ConfigInfoV2Bean::class)
                SysConfigService.saveBean(
                    ConfigInfoBean.PREFIX,
                    configBean.copy(hardware = firmwareVersion)
                )
            }
            // 板上电后刷新本地缓存的 firmware_id，供后续 invalid_comm_token 自动
            // 重新 /activate 时使用，不用等人工去工厂测试页手点激活
            NanoAuthStore.updateFirmwareId(firmwareId)

            // 网络上报后台执行，不阻塞启动
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = NanoApi.uploadLocalMachineInfo(firmwareVersion = firmwareVersion)
                    Timber.d(
                        "silent version report: software=%s firmware=%s ok=%s skipped=%s msg=%s",
                        BuildConfig.VERSION_NAME,
                        firmwareVersion,
                        result.ok,
                        result.skipped,
                        result.message
                    )
                } catch (e: Exception) {
                    Timber.w(e, "silent version report upload failed")
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "silent version report failed")
        }
    }

    override fun onTerminate() {
        super.onTerminate()

        serialHelper!!.close()
    }

    fun openSerialPort() {
        serialHelper = object : SerialHelperV2("/dev/ttyS1", 230400) {}
        serialHelper!!.stopBits = 1
        serialHelper!!.dataBits = 8
        serialHelper!!.parity = 0
        serialHelper!!.flowCon = 0
        serialHelper!!.close()
        serialHelper!!.open()
    }

    fun closeSerialPort() {
        serialHelper?.close()
        Timber.w("serial port closed")
    }

    // release 包无三方崩溃上报服务接入，仅把 warn 及以上日志转发到 logcat，
    // 方便现场用 adb logcat 排查（verbose/debug/info 级别丢弃，避免过于嘈杂）
    private class CrashReportingTree : Timber.Tree() {
        override fun isLoggable(tag: String?, priority: Int): Boolean {
            return priority >= android.util.Log.WARN
        }

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            if (!isLoggable(tag, priority)) {
                return
            }
            android.util.Log.println(priority, tag ?: "App", message)
            if (t != null) {
                android.util.Log.println(priority, tag ?: "App", android.util.Log.getStackTraceString(t))
            }
        }
    }
}
