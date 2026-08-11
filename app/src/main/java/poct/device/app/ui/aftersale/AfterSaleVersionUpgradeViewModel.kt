package poct.device.app.ui.aftersale

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.szyh.comm.CommService
import info.szyh.comm.CommSocketConstants
import info.szyh.comm.CommSocketMessageRecEvent
import info.szyh.comm.constant.CommEnsTaskType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import poct.device.app.App
import poct.device.app.R
import poct.device.app.bean.ConfigInfoBean
import poct.device.app.bean.ConfigInfoV2Bean
import poct.device.app.bean.ConfigSysBean
import poct.device.app.bean.VersionBean
import poct.device.app.bean.VersionUpgradeInfo
import poct.device.app.entity.service.SysConfigService
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.thirdparty.NanoApi
import poct.device.app.thirdparty.SbEdgeFunc
import poct.device.app.ui.sysfun.SysFunInfoViewModel.Companion.EVT_CONTACT_ADMIN
import poct.device.app.utils.app.AppFileUtils
import poct.device.app.utils.app.AppUpgradeUtils
import poct.device.app.utils.app.VersionUtils
import poct.device.app.utils.common.SmartDnsResolver
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class AfterSaleVersionUpgradeViewModel : ViewModel() {
    private val APK_URL = "https://poct-upgrade.virtualhealth.cn/apk/%s.apk"

    private val smartDns = SmartDnsResolver()

    // 视图状态
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    val actionState = MutableStateFlow(ActionState.Default)

    // 记录列表
    val record = MutableStateFlow(VersionUpgradeInfo.Empty)

    var version = MutableStateFlow(VersionBean.Empty)

    fun onLoad() {
        viewState.value = ViewState.LoadingOver()
        viewModelScope.launch(Dispatchers.IO) {
            // TODO 简化信息
//            val config = SysConfigService.findBean(ConfigInfoBean.PREFIX, ConfigInfoBean::class)

//            try {
//                val upgradeTask = CommService.instance().getWaitTask(CommEnsTaskType.UPGRADE)
//                record.value = VersionUpgradeInfo(
//                    count = upgradeTask.upgradeList.size,
//                    sysInfo = config
//                )
//            } catch (e: Exception) {
//                Timber.tag("record").e(e.message ?: "")
//                record.value = VersionUpgradeInfo(
//                    count = 0,
//                    sysInfo = config
//                )
//            }
//            delay(300)

            var configBean =
                SysConfigService.findBean(ConfigInfoBean.PREFIX, ConfigInfoV2Bean::class)

            if (configBean.hasData()) {
                record.value = VersionUpgradeInfo(
                    count = 0,
                    sysInfo = configBean
                )
            } else {
                // 获取设备ID
                val deviceId: String = App.getDeviceId()
                configBean = SbEdgeFunc.getDeviceConfig(deviceId)
                record.value = VersionUpgradeInfo(
                    count = 0,
                    sysInfo = configBean
                )
                if (!configBean.hasData()) {
                    actionState.value = ActionState(event = EVT_CONTACT_ADMIN)
                } else {
                    SysConfigService.saveBean(ConfigInfoBean.PREFIX, configBean)
                }
            }
            version.value = VersionBean(configBean.software, configBean.hardware)
            viewState.value = ViewState.LoadSuccess()
        }
    }

    fun onClearInteraction() {
        actionState.value = ActionState.Default
    }

    private var isDownloading = false

    fun onCheckVersion() {
        if (isDownloading) {
            return // 防止重复点击
        }

        actionState.value = ActionState(
            event = EVT_CHECKING,
            msg = App.getContext().getString(R.string.checking_version)
        )

        viewModelScope.launch {
            isDownloading = true

//            try {
//                val upgradeTask = CommService.instance().getWaitTask(CommEnsTaskType.UPGRADE)
//                record.value = VersionUpgradeInfo(count = upgradeTask.upgradeList.size)
//            } catch (e: Exception) {
//                // do nothing
//            }
//            delay(1000)

            try {
                val currentConfigBean = withContext(Dispatchers.IO) {
                    SysConfigService.findBean(ConfigInfoBean.PREFIX, ConfigInfoV2Bean::class)
                }

                val sysConfig = withContext(Dispatchers.IO) {
                    SysConfigService.findBean(ConfigSysBean.PREFIX, ConfigSysBean::class)
                }

                val remoteConfigBean: ConfigInfoV2Bean
                val downloadApkUrl: String

                val pending = NanoApi.pendingUpgrade
                if (pending != null) {
                    NanoApi.pendingUpgrade = null
                    remoteConfigBean = currentConfigBean.copy(software = pending.version)
                    downloadApkUrl = pending.url
                } else if (ConfigSysBean.isNanoFlow(sysConfig.flow)) {
                    val upgrade = NanoApi.checkUpgrade()
                    if (upgrade == null) {
                        throw Exception("Nano upgrade check failed")
                    }
                    remoteConfigBean = currentConfigBean.copy(software = upgrade.version)
                    downloadApkUrl = upgrade.url
                } else {
                    val deviceId: String = App.getDeviceId()
                    remoteConfigBean = SbEdgeFunc.getDeviceConfig(deviceId)
                    downloadApkUrl = String.format(APK_URL, remoteConfigBean.software)
                }

                Timber.w("currentConfig %s", currentConfigBean.software)
                Timber.w("remoteConfig %s", remoteConfigBean.software)

                if (VersionUtils.isLessThan(
                        currentConfigBean.software,
                        remoteConfigBean.software
                    )
                ) {
                    // 下载APK文件
                    val isOk = downloadAndInstallApkSync(
                        url = downloadApkUrl,
                        versionCode = remoteConfigBean.software,
                        remoteConfigBean = remoteConfigBean  // 传递配置
                    )

                    if (isOk) {
                        record.value = VersionUpgradeInfo(sysInfo = remoteConfigBean)
                        // 系统安装界面已接管，清除交互状态；用户取消安装后可重新检查
                        actionState.value = ActionState.Default
                    }
                } else {
                    actionState.value =
                        ActionState(
                            msg = App.getContext().getString(R.string.after_sale_upgrade_latest),
                            event = EVT_CHECK_LATEST
                        )
                }
            } catch (e: Exception) {
                Timber.e(e, "检查版本失败")
                actionState.value = ActionState(
                    msg = App.getContext().getString(R.string.after_sale_upgrade_failed),
                    event = EVT_ERROR
                )
            } finally {
                isDownloading = false
            }
        }
    }

    fun onUDiskUpgrade() {
        val packageManager: PackageManager = App.getContext().packageManager
        val installedApplications =
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        var applicationInfo: ApplicationInfo? = null
        for (i in installedApplications.indices) {
            val appInfo = installedApplications[i]
            val packageName = appInfo.packageName
            if (packageName.startsWith("com.mediatek.filemanager")) {
                applicationInfo = appInfo
                break
            }
        }
        if (applicationInfo != null) {
            val intent = packageManager.getLaunchIntentForPackage(applicationInfo.packageName)
            if (intent != null) {
                actionState.value = ActionState(event = AfterSaleViewModel.SALE_SUN_BUT)
                // 打开远程软件
                App.getContext().startActivity(intent)
            }
        } else {
            Timber.w("无法打开文件管理系统！")
        }
        //actionState.value = ActionState.Default
    }

    fun installApk(context: Context, apkUri: Uri) {
        actionState.value = ActionState(
            event = EVT_LOADING,
            msg = App.getContext().getString(R.string.after_sale_upgrade_loading)
        )
        if (!apkUri.toString().endsWith("Nanovate_AI.apk")) {
            actionState.value = ActionState(
                event = EVT_ERROR,
                msg = App.getContext().getString(R.string.after_sale_upgrade_file_check)
            )
            return
        }
        val apkPath = AppFileUtils.getUpgradeApkPath()
        val apkFile = File(apkPath)
        if (!apkFile.parentFile?.exists()!!) {
            apkFile.parentFile?.mkdirs()
        }
        if (apkFile.exists()) {
            apkFile.delete()
        }
        // 将移动APK文件到内部升级目录
        context.contentResolver.openInputStream(apkUri)?.use {
            FileOutputStream(apkFile).use { outputStream ->
                it.copyTo(outputStream)
            }
        }
        // 生成一个升级启动文件
        val markPath = AppFileUtils.getUpgradeApkMarkPath()
        val upgradeFile = File(markPath)
        if (!upgradeFile.exists()) {
            upgradeFile.createNewFile()
        }
    }

    fun onUpgrade() {
        viewState.value =
            ViewState.LoadingOver(App.getContext().getString(R.string.upgrading_version))
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                AppUpgradeUtils.upgrade(version.value.version)
            }
            actionState.value = ActionState(
                event = EVT_UPGRADE,
                msg = App.getContext().getString(R.string.upgrade_version_success)
            )
        }
    }

    fun onHandleVersion() {
        actionState.value = ActionState(event = EVT_HANDLE)
    }

    fun onUpgradeFromUrl(url: String, version: String) {
        if (isDownloading) return
        viewModelScope.launch {
            isDownloading = true
            actionState.value = ActionState(event = EVT_DOWNLOADING, msg = "开始下载新版本...")
            try {
                val configBean = withContext(Dispatchers.IO) {
                    SysConfigService.findBean(ConfigInfoBean.PREFIX, ConfigInfoV2Bean::class)
                }
                val ok = downloadAndInstallApkSync(
                    url = url,
                    versionCode = version,
                    remoteConfigBean = configBean.copy(software = version),
                )
                if (!ok) {
                    actionState.value = ActionState(
                        event = EVT_ERROR,
                        msg = App.getContext().getString(R.string.after_sale_upgrade_failed)
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "onUpgradeFromUrl failed")
                actionState.value = ActionState(
                    event = EVT_ERROR,
                    msg = e.message ?: App.getContext().getString(R.string.after_sale_upgrade_failed)
                )
            } finally {
                isDownloading = false
            }
        }
    }

    companion object {
        /**
         * 查看处理升级
         */
        const val EVT_LOADING = "loading"
        const val EVT_CHECKING = "checking"
        const val EVT_CHECK_DONE = "checkDone"
        const val EVT_CHECK_LATEST = "checkLatest"

        /**
         * 查看处理升级
         */
        const val EVT_HANDLE = "handle"

        /**
         * 执行升级
         */
        const val EVT_UPGRADE = "upgrade"

        /**
         * U盘升级
         */
        const val EVT_UDISK_UPGRADE = "uDiskUpgrade"
        const val EVT_ERROR = "error"

        /**
         * 下载和安装相关事件
         */
        const val EVT_DOWNLOADING = "downloading"
        const val EVT_INSTALLING = "installing"
        const val EVT_INSTALL_FAILED = "installFailed"
        const val EVT_LAUNCH_APP = "launchApp"
        const val EVT_DOWNLOAD_FAILED = "downloadFailed"
    }

    fun handlePluginEvent(event: CommSocketMessageRecEvent) {
        // 只监控版本升级信息
        if (CommSocketConstants.CMD_UPGRADE != event.cmd) {
            return
        }

        Timber.tag("UPGRADE").d("UPGRADE")
        try {
            val upgradeTask = CommService.instance().getWaitTask(CommEnsTaskType.UPGRADE)
            record.value = record.value.copy(count = upgradeTask.upgradeList.size)
        } catch (e: Exception) {
            Timber.tag("record").e(e.message ?: "")
            record.value = record.value.copy(count = 0)
        }
    }


    /**
     * 同步版本：下载并安装APK
     * 返回是否成功
     */
    private suspend fun downloadAndInstallApkSync(
        url: String, versionCode: String,
        remoteConfigBean: ConfigInfoV2Bean
    ): Boolean {
        return try {
            // 1. 下载APK
            val apkFile = downloadApkWithValidation(url, versionCode)

            if (apkFile != null && apkFile.exists()) {
                // 2. 安装APK
                val installSuccess = installApkSync(apkFile, remoteConfigBean)
                installSuccess
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "下载或安装失败")
            false
        }
    }

    private suspend fun downloadApkWithValidation(
        url: String,
        versionCode: String
    ): File? = downloadApkWithProgressSync(url, versionCode)

    /**
     * 带进度显示的下载APK
     */
    private suspend fun downloadApkWithProgressSync(url: String, versionCode: String): File? {
        return withContext(Dispatchers.IO) {
            val context = App.getContext()

            // 创建下载目录
            val fileName = "app_update_${versionCode}.apk"
            val downloadDirectory = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            } else {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            }

            if (downloadDirectory?.exists() == false) {
                downloadDirectory.mkdirs()
            }

            val destinationFile = File(downloadDirectory, fileName)

            // 下载前清理历史升级包：文件名带版本号，不同版本会各留一份，长期累积占满存储。
            // 安装界面无完成回调，无法在安装后删除当前包，因此在下载前统一清掉旧包（含同名的未完成残留），
            // 也顺带为本次下载腾出空间。最多只会残留最近一次成功下载的那一份，留待下次升级清理。
            downloadDirectory?.listFiles { file ->
                file.name.startsWith("app_update_") && file.name.endsWith(".apk")
            }?.forEach { it.delete() }

            // 创建下载请求
            val downloadManager =
                context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (downloadManager == null) {
                withContext(Dispatchers.Main) {
                    actionState.value = ActionState(
                        msg = "下载服务不可用",
                        event = EVT_DOWNLOAD_FAILED
                    )
                }
                return@withContext null
            }

            val request = DownloadManager.Request(Uri.parse(url))
                .setTitle("应用更新")
                .setDescription("正在下载新版本...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationUri(Uri.fromFile(destinationFile))
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
                .setMimeType("application/vnd.android.package-archive")

            // 开始下载
            val downloadId = downloadManager.enqueue(request)

            // 轮询查询下载状态
            var pausedTicks = 0
            while (isDownloading) {
                delay(1000)

                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)

                if (!cursor.moveToFirst()) {
                    cursor.close()
                    // Download record vanished — treat as failure
                    isDownloading = false
                    return@withContext null
                }

                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                cursor.close()

                when (status) {
                    DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING -> {
                        pausedTicks = 0
                        val progress = if (total > 0) (downloaded * 100 / total).toInt() else 0
                        withContext(Dispatchers.Main) {
                            actionState.value = ActionState(
                                event = EVT_DOWNLOADING,
                                msg = "正在下载新版本... $progress%"
                            )
                        }
                    }

                    DownloadManager.STATUS_PAUSED -> {
                        pausedTicks++
                        withContext(Dispatchers.Main) {
                            actionState.value = ActionState(
                                event = EVT_DOWNLOADING,
                                msg = "下载暂停，等待重试... ($pausedTicks)"
                            )
                        }
                        if (pausedTicks >= 10) {
                            downloadManager.remove(downloadId)
                            isDownloading = false
                            return@withContext null
                        }
                    }

                    DownloadManager.STATUS_SUCCESSFUL -> {
                        Timber.d("下载完成")
                        isDownloading = false
                        return@withContext destinationFile
                    }

                    DownloadManager.STATUS_FAILED -> {
                        isDownloading = false
                        return@withContext null
                    }
                }
            }

            return@withContext null
        }
    }

    /**
     * 安装APK并等待结果
     */
    private suspend fun installApkSync(
        apkFile: File,
        remoteConfigBean: ConfigInfoV2Bean
    ): Boolean {
        return suspendCoroutine { continuation ->
            val context = App.getContext()

            if (!apkFile.exists()) {
                continuation.resume(false)
                return@suspendCoroutine
            }

            try {
                val intent = Intent(Intent.ACTION_VIEW)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // 下载完成更新版本号
                    viewModelScope.launch(Dispatchers.IO) {
                        SysConfigService.saveBean(ConfigInfoBean.PREFIX, remoteConfigBean)
                        Timber.d("配置保存完成: ${remoteConfigBean.software}")
                    }

                    val apkUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )

                    intent.apply {
                        setDataAndType(apkUri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                    }
                } else {
                    val apkUri = Uri.fromFile(apkFile)
                    intent.apply {
                        setDataAndType(apkUri, "application/vnd.android.package-archive")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }

                actionState.value = ActionState(
                    event = EVT_INSTALLING,
                    msg = "请在系统安装界面完成安装"
                )
                // 启动安装界面
                context.startActivity(intent)
                // 系统安装界面无法回调取消/完成结果，立即返回，让调用方复位下载状态，
                // 用户取消安装后可再次下载或安装
                continuation.resume(true)
            } catch (e: Exception) {
                Timber.e(e, "安装APK失败")
                continuation.resume(false)
            }
        }
    }

    private fun launchApp(context: Context?) {
        try {
            val packageName = context?.packageName
            val intent = context?.packageManager?.getLaunchIntentForPackage(packageName!!)

            if (intent != null) {
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                )
                context.startActivity(intent)
                // 注意：这里不再更新状态，因为已经在installApk的广播接收器中更新了
            }
        } catch (e: Exception) {
            Timber.e(e, "启动应用失败")
        }
    }
}
