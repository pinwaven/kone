package poct.device.app

import android.annotation.SuppressLint
import android.app.Activity
import poct.device.app.bean.CardConfigBean
import poct.device.app.bean.CaseBean
import poct.device.app.bean.PrinterInfo
import poct.device.app.bean.ScannerInfo
import poct.device.app.bean.WlanBean
import poct.device.app.entity.User
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("StaticFieldLeak")
object AppParams {
    // 模拟硬件
    val devMock = true

    //val devMock = true
    var testCount: Int = 0

    // 重复测试
    val removeReport = true

    /**
     * 初始化状态
     */
    var initState = false

    var battery: Int = 0
    var batteryPlugged: Boolean = false

    var wlanEnabled: Boolean = false
    var curWlan: WlanBean = WlanBean.Empty

    /**
     * 当前打印机
     */
    var curPrinter = PrinterInfo.Empty

    /**
     * 当前扫描枪
     */
    var curScanner = ScannerInfo.Empty


    /**
     * 当前activity
     */
    var curActivity: Activity? = null

    /**
     * 字典缓存
     */
    val dictMap = ConcurrentHashMap<String, Any>()

    /**
     * 当前用户
     */
    var curUser = User.Empty


    /**
     * 当前报告变量，用于样本报告页面
     */
    var varReport = CaseBean.Empty

    /**
     * 用户管理-用户变量
     */
    var varUser = User.Empty
    var varUserMode = "add"

    var varCardConfig = CardConfigBean.Empty
    var varCardConfigForPreview = CardConfigBean.Empty
    var varCardConfigMode = "add"
    var varCardConfigViewMode = "preview"

    var resumeStatus = false
}