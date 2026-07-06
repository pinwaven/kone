package poct.device.app

import android.app.Application
import android.hardware.usb.UsbManager
import android.net.wifi.WifiManager
import com.google.gson.FieldNamingPolicy
import com.google.gson.GsonBuilder
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import poct.device.app.bean.ConfigInfoBean
import poct.device.app.bean.ConfigSysBean
import poct.device.app.entity.service.SysConfigService
import poct.device.app.serial.v2.SerialHelperV2
import poct.device.app.serial.v2.ctl.CtlCommandsV2
import poct.device.app.utils.app.AppLangUtils
import poct.device.app.utils.app.AppSystemUtils
import poct.device.app.utils.app.DeviceIdUtils
import timber.log.Timber

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
        openCtlBoard()
        startupService()
    }

    /**
     * 打开控制板电源
     */
    private fun openCtlBoard() {
        Thread {
            CoroutineScope(Dispatchers.IO).launch {
                AppSystemUtils.powerOffCtlBoard()
                delay(500)
                AppSystemUtils.powerOnCtlBoard()
            }
        }.start()
    }

    /**
     * 启动相关服务
     */
    private fun startupService() {
        Thread {
            CoroutineScope(Dispatchers.IO).launch {
                // 恢复传感器检测开关
                val configSys =
                    SysConfigService.findBean(ConfigSysBean.PREFIX, ConfigSysBean::class)
                AppParams.runtimeModeState.setSensorDetectionEnabled(
                    ConfigSysBean.isSensorDetectionEnabled(configSys.sensorDetection)
                )

                // TODO 简化信息
                // 串口服务
//                ctlService = CtlSerialServiceV2()
//                ctlService!!.start()

                // 等待供电完成
                delay(2000)
                openSerialPort()
                // power 板上电后第一次请求可能会CRC报错，先poll一次
                CtlCommandsV2.readAllData(CtlCommandsV2.poll())

                // 物联网连接
                val configInfo =
                    SysConfigService.findBean(ConfigInfoBean.PREFIX, ConfigInfoBean::class)
                Timber.d(configInfo.code)
                // TODO 物联网先不调试
                //            Timber.d("日志测试")
                //            val prefix =
                //                Environment.getExternalStorageDirectory().absolutePath + File.separator + "Android" + File.separator + "Nanovate_AI"
                //            CommService.instance().startIot("192.168.1.16", 7300, 9100, "TM-YG01-00046", prefix)
            }
        }.start()
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

        val hiResult = CtlCommandsV2.readAllData(CtlCommandsV2.hi())
        Timber.w("hiResult: $hiResult")
    }

    fun closeSerialPort() {
        serialHelper?.close()
        Timber.w("serial port closed")
    }

    private class CrashReportingTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {}
    }
}
