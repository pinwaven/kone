package poct.device.app

import android.annotation.SuppressLint
import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import poct.device.app.bean.CardConfigBean
import poct.device.app.bean.CaseBean
import poct.device.app.bean.PrinterInfo
import poct.device.app.bean.ScannerInfo
import poct.device.app.bean.WlanBean
import poct.device.app.entity.User
import poct.device.app.state.RuntimeModeState
import java.util.concurrent.ConcurrentHashMap

@SuppressLint("StaticFieldLeak")
object AppParams {
    val runtimeModeState = RuntimeModeState()

    // 模拟硬件
    val devMock = false

    //val devMock = true
    var testCount: Int = 0

    // 重复测试
    val removeReport = true

    /**
     * 初始化状态
     */
    var initState = false

    /**
     * 控制板断电恢复信号 — 值变化时首页重置到初始状态
     */
    val ctlBoardResetEvent = MutableStateFlow(0L)

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

    /**
     * 主板欠压重启保护：主流程因未确认主板可用而锁定
     */
    val boardPowerBlocked = MutableStateFlow(false)
    val boardPowerAgingWarn = MutableStateFlow(false)

    fun setBoardPowerBlocked(blocked: Boolean, agingWarn: Boolean) {
        boardPowerBlocked.value = blocked
        boardPowerAgingWarn.value = blocked && agingWarn
    }

    /**
     * 锁定提示弹窗可被用户关掉（不再强制常驻），但锁定状态本身不受影响。
     * 用户尝试真正开始检测时调用本方法重新弹出提示，拦下继续操作。
     */
    val boardPowerBlockReassertEvent = MutableStateFlow(0L)

    fun reassertBoardPowerBlock() {
        boardPowerBlockReassertEvent.value = System.currentTimeMillis()
    }

    /**
     * Nano AI backend (Aliyun FC). Hardcoded — not user-configurable. The
     * operator only sets the per-device serial number in Settings.
     */
    const val NANO_BASE_URL  = "https://nano.fros.cc"
    const val NANO_API_TOKEN = "tokenData-gh9bc7917115bid72c68c8c4693g"

    fun kinoActivationToken(): String = BuildConfig.KINO_ACTIVATION_TOKEN
}
