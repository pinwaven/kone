package poct.device.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.admin.DevicePolicyManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.Settings
import android.util.LruCache
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navigation
import androidx.print.PrintHelper
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import org.greenrobot.eventbus.NoSubscriberEvent
import org.greenrobot.eventbus.Subscribe
import poct.device.app.event.AppPdfPrintEvent
import poct.device.app.event.AppScannerEvent
import poct.device.app.event.AppWifiEvent
import poct.device.app.theme.AppFullScreenTheme
import poct.device.app.theme.primaryColor
import poct.device.app.ui.aftersale.AfterSaleMain
import poct.device.app.ui.aftersale.AfterSaleVersionRecord
import poct.device.app.ui.aftersale.AfterSaleVersionUpgrade
import poct.device.app.ui.countdown.CountdownMain
import poct.device.app.ui.home.HomeMain
import poct.device.app.ui.report.ReportDetail
import poct.device.app.ui.report.ReportEdit
import poct.device.app.ui.report.ReportMain
import poct.device.app.ui.report.ReportPDF
import poct.device.app.ui.sample.SampleSerial
import poct.device.app.ui.setting.SettingMain
import poct.device.app.ui.single.SingleLogin
import poct.device.app.ui.single.SingleSplash
import poct.device.app.ui.sysconfig.SysConfigDateTime
import poct.device.app.ui.sysconfig.SysConfigInner
import poct.device.app.ui.sysconfig.SysConfigLang
import poct.device.app.ui.sysconfig.SysConfigMain
import poct.device.app.ui.sysconfig.SysConfigPrinter
import poct.device.app.ui.sysconfig.SysConfigScanner
import poct.device.app.ui.sysconfig.SysConfigScannerList
import poct.device.app.ui.sysconfig.SysConfigSys
import poct.device.app.ui.sysconfig.SysConfigSysCombine
import poct.device.app.ui.sysconfig.SysConfigWlan
import poct.device.app.ui.sysfun.SysFunAdjust
import poct.device.app.ui.sysfun.SysFunInfo
import poct.device.app.ui.sysfun.SysFunMain
import poct.device.app.ui.sysfun.SysFunUser
import poct.device.app.ui.sysfun.SysFunUserSave
import poct.device.app.ui.work.WorkMain
import poct.device.app.ui.workconfig.WorkConfigCard
import poct.device.app.ui.workconfig.WorkConfigCardAdd
import poct.device.app.ui.workconfig.WorkConfigCardQrCode
import poct.device.app.ui.workconfig.WorkConfigCardView
import poct.device.app.ui.workconfig.WorkConfigMain
import poct.device.app.ui.workconfig.WorkConfigReport
import poct.device.app.utils.app.AppBatteryReceiverHelper
import poct.device.app.utils.app.AppEventUtils
import poct.device.app.utils.app.AppLangUtils
import timber.log.Timber
import java.io.File
import java.io.IOException

class MainActivity : ComponentActivity() {
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val LOCATION_BG_REQUEST_CODE = 1002
    private val NEARBY_WIFI_DEVICES_REQUEST_CODE = 1003

    private val deviceAdminLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            // 用户已授权设备管理员权限
            println("设备管理员已启用")
        } else {
            // 用户拒绝授权
            println("用户拒绝了设备管理员权限")
        }
    }

    // 添加电池接收器助手实例
    private val appBatteryReceiverHelper = AppBatteryReceiverHelper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppParams.curActivity = this

        setContent {
            AppLangUtils.useLanguage(this)
            AppFullScreenTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = primaryColor,
                ) {
                    MainNavHost()
                }
            }
            RequestPermissions()
        }

        requestPermissions()

        // 在onCreate中初始化电池显示
        AppBatteryReceiverHelper.initBatteryOnAppStart(this)

        enableStrategy()
    }

    override fun onResume() {
        registerWifiListener()
        registerBatteryListener()
        registerScannerListener()
        AppEventUtils.register(this)
        super.onResume()
        Timber.w("=========重新调用=========")
        AppParams.resumeStatus = true
        // 每次回到Activity时重新隐藏系统栏
        hideSystemBars()

        // 重新注册电池广播接收器
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        applicationContext.registerReceiver(
            appBatteryReceiverHelper.batteryStateReceiver,
            batteryFilter
        )
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // 拦截 Home/Recent 键
        return when (keyCode) {
            KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_APP_SWITCH -> true
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
    }

    override fun onDestroy() {
        super.onDestroy()

//        applicationContext.unregisterReceiver(wifiStateReceiver)
        // 修改：使用batteryReceiverHelper的接收器
        try {
            applicationContext.unregisterReceiver(appBatteryReceiverHelper.batteryStateReceiver)
        } catch (e: IllegalArgumentException) {
            // 可能已经注销
        }

        applicationContext.unregisterReceiver(bluetoothReceiver)
        AppEventUtils.unregister(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                println("权限已授权")
            } else {
                println("权限授权被拒绝")
            }
        }
    }

    @Subscribe
    fun handleNoSubscriberEvent(event: NoSubscriberEvent) {
        // 隐藏事件未处理的日志
    }

    @Subscribe
    @SuppressLint("MissingPermission")
    fun handleScannerEvent(event: AppPdfPrintEvent) {
        Timber.w("=========PdfPath:${event.pdfPath}")
        //实例化类
        val photoPrinter = PrintHelper(this)
        photoPrinter.scaleMode = PrintHelper.SCALE_MODE_FIT //设置填充的类型，填充的类型指的是在A4纸上打印时的填充类型，两种模式

        val bitmap = convertPdfToBitmap(File(event.pdfPath), 0) ?: return
        //打印
        photoPrinter.printBitmap("pdfReport", bitmap) //这里的第一个参数是打印的jobName
        Timber.w("========打印完成========")
    }

    @SuppressLint("BatteryLife")
    private fun enableStrategy() {
        // 授权设备管理员
        requestDeviceAdminPermission()

        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:$packageName".toUri()
        }
        startActivity(intent)

        // 设置屏幕常亮标志
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

//        if (!AppParams.devMock) {
        // 隐藏系统导航栏（沉浸式模式）
        hideSystemBars()

        // 屏蔽系统导航栏
        val it = Intent("com.systemui.navigationbar")
        it.putExtra("enable", false)
        it.setPackage("com.android.systemui")
        this@MainActivity.sendBroadcast(it)
//        } else {
//            displaySystemBars()
//
//            val it = Intent("com.systemui.navigationbar")
//            it.putExtra("enable", true)
//            it.setPackage("com.android.systemui")
//            this@MainActivity.sendBroadcast(it)
//        }
    }

    private fun requestDeviceAdminPermission() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, MyDeviceAdminReceiver::class.java)
        if (!dpm.isAdminActive(adminComponent)) {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "需要设备管理员权限以控制设备")
            }
            deviceAdminLauncher.launch(intent)
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun displaySystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, true)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            show(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }

    // 请求权限
    private fun requestPermissions() {
        var requiredPermissions = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                println("已有精确位置权限")
            }

            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                // 向用户解释为什么需要权限
                AlertDialog.Builder(this)
                    .setTitle("位置权限说明")
                    .setMessage("此功能需要访问您的位置以提供导航服务")
                    .setPositiveButton("确定") { _, _ ->
                        ActivityCompat.requestPermissions(
                            this,
                            requiredPermissions,
                            LOCATION_PERMISSION_REQUEST_CODE
                        )
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }

            else -> {
                // 直接请求
                ActivityCompat.requestPermissions(
                    this,
                    requiredPermissions,
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        }

        // 需要单独请求
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val bgPermission = Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ActivityCompat.requestPermissions(this, arrayOf(bgPermission), LOCATION_BG_REQUEST_CODE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val nwdPermission = Manifest.permission.NEARBY_WIFI_DEVICES
            ActivityCompat.requestPermissions(
                this, arrayOf(nwdPermission), NEARBY_WIFI_DEVICES_REQUEST_CODE
            )
        }
    }

    private fun convertPdfToBitmap(pdfFile: File?, pageIndex: Int): Bitmap? {
        try {
            val parcelFileDescriptor =
                ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(parcelFileDescriptor)
            if (pageIndex < 0 || pageIndex >= pdfRenderer.pageCount) {
                return null
            }
            val page = pdfRenderer.openPage(pageIndex)
            val bitmap = createBitmap(page.width, page.height)
            // 将页面渲染到Bitmap中
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            // 保存为png时操作保存Bitmap为PNG到指定路径
            // 注意：这里需要实现保存Bitmap到文件的逻辑，例如使用Bitmap.compress()方法
            page.close()
            pdfRenderer.close()
            parcelFileDescriptor.close()
            return bitmap
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    private fun registerBatteryListener() {
        // 使用BatteryReceiverHelper的接收器
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        applicationContext.registerReceiver(
            appBatteryReceiverHelper.batteryStateReceiver,
            intentFilter
        )
    }

    // 移除原有的batteryStateReceiver，使用BatteryReceiverHelper中的
    // private val batteryStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
    //     override fun onReceive(context: Context, intent: Intent) {
    //         val current = intent.extras!!.getInt(BatteryManager.EXTRA_LEVEL)// 获得当前电量
    //         val total = intent.extras!!.getInt(BatteryManager.EXTRA_SCALE)// 获得总电量
    //         val plugged = intent.extras!!.getInt(BatteryManager.EXTRA_PLUGGED, -1) != 0// 是否充电
    //         val percent = current * 100 / total
    //         Timber.w("batteryStateReceiver percent: %s", percent)
    //
    //         AppParams.battery =
    //             AppBatteryUtils.updateAndGetDisplayPercent(percent.toFloat()).toInt()
    //         Timber.w("batteryStateReceiver AppParams.battery: %s", AppParams.battery)
    //
    //         AppParams.batteryPlugged = plugged
    //         EventUtils.publishEvent(AppBatteryEvent(AppParams.battery, plugged))
    //     }
    // }

    private fun registerWifiListener() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION) // WiFi开关的状态
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION) // WiFi开关的状态
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) // 搜索Wifi扫描已完成，并且结果可用
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION) // 当前连接WiFi状态的变化,这个监听是指当前已经连接WiFi的断开与重连的状态
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)
        applicationContext.registerReceiver(wifiStateReceiver, intentFilter)
    }

    private val wifiStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                    val wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0)
                    val isWifiEnabled = wifiState == WifiManager.WIFI_STATE_ENABLED
                    AppParams.curWlan.connected = isWifiEnabled
                    AppParams.wlanEnabled = isWifiEnabled

                    // 如果WiFi被禁用，连接状态也应该为false
                    if (!isWifiEnabled) {
                        AppParams.curWlan.connected = false
                        AppParams.wlanEnabled = false
                    }
                }

                ConnectivityManager.CONNECTIVITY_ACTION -> {
                    val connectivityManager =
                        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val network = connectivityManager.activeNetwork
                    val capabilities = connectivityManager.getNetworkCapabilities(network)
                    val isWifiConnected =
                        capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

                    AppParams.curWlan.connected = isWifiConnected
                    AppParams.wlanEnabled = isWifiConnected
                }
            }

            // 发布统一的事件
            AppEventUtils.publishEvent(AppWifiEvent(context, intent))
        }
    }

    private fun registerScannerListener() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND) //获得扫描结果
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED) //绑定状态变化
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED) //开始扫描
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED) //扫描结束
        applicationContext.registerReceiver(bluetoothReceiver, intentFilter) //注册广播接收器
    }

    private val bluetoothReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            AppEventUtils.publishEvent(AppScannerEvent(context, intent))
        }
    }

    private val memoryCache = LruCache<String, Bitmap>(2048 / 8)

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            TRIM_MEMORY_RUNNING_MODERATE,
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                // 释放非必要资源
                clearCache()
                releaseUnusedResources()
            }

            TRIM_MEMORY_BACKGROUND,
            TRIM_MEMORY_MODERATE,
            TRIM_MEMORY_COMPLETE -> {
                // 系统内存紧张，主动清理
                memoryCache.evictAll()
                System.gc()
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        // 紧急内存释放
        memoryCache.evictAll()
        System.gc()
    }

    private fun clearCache() {
        // 清理缓存
        memoryCache.evictAll()
    }

    private fun releaseUnusedResources() {
        // 释放未使用的资源
        viewModelStore.clear()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RequestPermissions() {
//    var permissionList = listOf(
//        Manifest.permission.ACCESS_WIFI_STATE,
//        Manifest.permission.CHANGE_WIFI_STATE,
//        // Manifest.permission.WRITE_EXTERNAL_STORAGE,
//        // Manifest.permission.READ_EXTERNAL_STORAGE,
//        // Manifest.permission.BLUETOOTH,
//        // Manifest.permission.BLUETOOTH_ADMIN,
//    )

    // 定义 Permission State
//    val permissionState = rememberMultiplePermissionsState(permissionList)
//    var confirmVisible by remember { mutableStateOf(false) }
//    confirmVisible = !permissionState.allPermissionsGranted
//    AppConfirm(
//        visible = confirmVisible,
//        title = "Permission failed",
//        onConfirm = {
//            confirmVisible = false
//            permissionState.launchMultiplePermissionRequest()
//        },
//        onCancel = { confirmVisible = false },
//        confirmText = "Request Again"
//    )
    if (!Settings.canDrawOverlays(App.getContext())) {
        val activity = AppParams.curActivity
        val intent1 = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            ("package:" + App.getContext().packageName).toUri()
        )
        activity?.startActivityForResult(intent1, 1)
    }
}

/**
 * 注册所有路由和界面
 */
@Composable
fun MainNavHost() {
    val navController = rememberNavController()

    // 确保在 NavHost 之前设置 ViewModelStore
    val context = LocalContext.current
    LaunchedEffect(navController) {
        if (context is ComponentActivity) {
            navController.setViewModelStore(context.viewModelStore)
        }
    }

    NavHost(navController = navController, startDestination = RouteConfig.SINGLE) {
        // single路由注册
        singleNav(navController)
        // home路由注册
        homeNav(navController)
        // setting路由注册
        settingNav(navController)
        // work路由注册
        workNav(navController)
        // 报表路由注册
        reportNav(navController)
        // 售后路由注册
        afterSaleNav(navController)
        // 系统功能路由注册
        sysFunNav(navController)
        // 检测配置路由注册
        workConfigNav(navController)
        // 系统配置路由注册
        sysConfigNav(navController)
        // 例子配置路由注册
        sampleConfigNav(navController)
        // 倒计时路由注册
        countdownNav(navController)
    }
}

fun NavGraphBuilder.countdownNav(navController: NavController) {
    navigation(startDestination = RouteConfig.COUNTDOWN_MAIN, route = RouteConfig.COUNTDOWN) {
        composable(RouteConfig.COUNTDOWN_MAIN) { CountdownMain(navController) }
    }
}

fun NavGraphBuilder.sampleConfigNav(navController: NavController) {
    navigation(startDestination = RouteConfig.SAMPLE_SERIAL, route = RouteConfig.SAMPLE) {
        composable(RouteConfig.SAMPLE_SERIAL) { SampleSerial(navController = navController) }
    }
}

fun NavGraphBuilder.singleNav(navController: NavController) {
    navigation(startDestination = RouteConfig.SINGLE_SPLASH, route = RouteConfig.SINGLE) {
        composable(RouteConfig.SINGLE_SPLASH) { SingleSplash(navController = navController) }
        composable(RouteConfig.SINGLE_LOGIN) { SingleLogin(navController = navController) }
    }
}

fun NavGraphBuilder.homeNav(navController: NavController) {
    navigation(startDestination = RouteConfig.HOME_MAIN, route = RouteConfig.HOME) {
        composable(RouteConfig.HOME_MAIN) { HomeMain(navController = navController) }
    }
}

fun NavGraphBuilder.settingNav(navController: NavController) {
    navigation(startDestination = RouteConfig.SETTING_MAIN, route = RouteConfig.SETTING) {
        composable(RouteConfig.SETTING_MAIN) { SettingMain(navController = navController) }
    }
}

fun NavGraphBuilder.workNav(navController: NavController) {
    navigation(startDestination = RouteConfig.WORK_MAIN, route = RouteConfig.WORK) {
        composable(RouteConfig.WORK_MAIN) { WorkMain(navController = navController) }
    }
}

fun NavGraphBuilder.reportNav(navController: NavController) {
    navigation(startDestination = RouteConfig.REPORT_MAIN, route = RouteConfig.REPORT) {
        composable(RouteConfig.REPORT_MAIN) { ReportMain(navController = navController) }
        composable(RouteConfig.REPORT_PDF) { ReportPDF(navController = navController) }
        composable(RouteConfig.REPORT_DETAIL) { ReportDetail(navController = navController) }
        composable(RouteConfig.REPORT_EDIT) { ReportEdit(navController = navController) }
    }
}

fun NavGraphBuilder.afterSaleNav(navController: NavController) {
    navigation(startDestination = RouteConfig.AFTER_SALE_MAIN, route = RouteConfig.AFTER_SALE) {
        composable(RouteConfig.AFTER_SALE_MAIN) { AfterSaleMain(navController = navController) }
        composable(RouteConfig.AFTER_SALE_VERSION_UPGRADE) { AfterSaleVersionUpgrade(navController = navController) }
        composable(RouteConfig.AFTER_SALE_VERSION_RECORD) { AfterSaleVersionRecord(navController = navController) }
    }
}

fun NavGraphBuilder.sysFunNav(navController: NavController) {
    navigation(startDestination = RouteConfig.SYS_FUN_MAIN, route = RouteConfig.SYS_FUN) {
        composable(RouteConfig.SYS_FUN_MAIN) { SysFunMain(navController = navController) }
        composable(RouteConfig.SYS_FUN_INFO) { SysFunInfo(navController = navController) }
        composable(RouteConfig.SYS_FUN_USER) { SysFunUser(navController = navController) }
        composable(RouteConfig.SYS_FUN_USER_SAVE) { SysFunUserSave(navController = navController) }
        composable(RouteConfig.SYS_FUN_ADJUST) { SysFunAdjust(navController = navController) }
    }
}

fun NavGraphBuilder.workConfigNav(navController: NavController) {
    navigation(startDestination = RouteConfig.WORK_CONFIG_MAIN, route = RouteConfig.WORK_CONFIG) {
        composable(RouteConfig.WORK_CONFIG_MAIN) { WorkConfigMain(navController = navController) }
        composable(RouteConfig.WORK_CONFIG_CARD) { WorkConfigCard(navController = navController) }
        composable(RouteConfig.WORK_CONFIG_CARD_ADD) { WorkConfigCardAdd(navController = navController) }
        composable(RouteConfig.WORK_CONFIG_CARD_VIEW) { WorkConfigCardView(navController = navController) }
        composable(RouteConfig.WORK_CONFIG_CARD_QR) { WorkConfigCardQrCode(navController = navController) }
        composable(RouteConfig.WORK_CONFIG_REPORT) { WorkConfigReport(navController = navController) }
    }
}

fun NavGraphBuilder.sysConfigNav(navController: NavController) {
    navigation(startDestination = RouteConfig.SYS_CONFIG_MAIN, route = RouteConfig.SYS_CONFIG) {
        composable(RouteConfig.SYS_CONFIG_MAIN) { SysConfigMain(navController = navController) }
        composable(RouteConfig.SYS_CONFIG_SYS) { SysConfigSys(navController = navController) }
        composable(RouteConfig.SYS_CONFIG_SYS_COMBINE) { SysConfigSysCombine(navController = navController) }
        composable(RouteConfig.SYS_CONFIG_INNER) { SysConfigInner(navController = navController) }
        composable(RouteConfig.SYS_CONFIG_WLAN) { SysConfigWlan(navController = navController) }
        composable(RouteConfig.SYS_CONFIG_DATETIME) { SysConfigDateTime(navController = navController) }
        composable(RouteConfig.SYS_CONFIG_LANG) { SysConfigLang(navController = navController) }
        composable(RouteConfig.SYS_CONFIG_PRINTER) { SysConfigPrinter(navController = navController) }
        composable(RouteConfig.SYS_CONFIG_SCANNER) { SysConfigScanner(navController = navController) }
        composable(RouteConfig.SYS_CONFIG_SCANNER_LIST) { SysConfigScannerList(navController = navController) }
    }
}




