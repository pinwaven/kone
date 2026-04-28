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
import okhttp3.Request
import poct.device.app.App
import poct.device.app.R
import poct.device.app.bean.ConfigInfoBean
import poct.device.app.bean.ConfigInfoV2Bean
import poct.device.app.bean.VersionBean
import poct.device.app.bean.VersionUpgradeInfo
import poct.device.app.entity.service.SysConfigService
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.thirdparty.SbEdgeFunc
import poct.device.app.ui.sysfun.SysFunInfoViewModel.Companion.EVT_CONTACT_ADMIN
import poct.device.app.utils.app.AppFileUtils
import poct.device.app.utils.app.AppUpgradeUtils
import poct.device.app.utils.app.VersionUtils
import poct.device.app.utils.common.HttpUtils
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
                val deviceId: String = App.getDeviceId()
                val remoteConfigBean = SbEdgeFunc.getDeviceConfig(deviceId)

                Timber.w("currentConfig %s", currentConfigBean.software)
                Timber.w("remoteConfig %s", remoteConfigBean.software)

                if (VersionUtils.isLessThan(
                        currentConfigBean.software,
                        remoteConfigBean.software
                    )
                ) {
                    val downloadApkUrl = String.format(APK_URL, remoteConfigBean.software)

                    // 下载APK文件
                    val isOk = downloadAndInstallApkSync(
                        url = downloadApkUrl,
                        versionCode = remoteConfigBean.software,
                        remoteConfigBean = remoteConfigBean  // 传递配置
                    )

                    if (isOk) {
                        record.value = VersionUpgradeInfo(sysInfo = remoteConfigBean)
                        actionState.value = ActionState(
                            msg = "开始下载新版本...",
                            event = EVT_DOWNLOADING
                        )
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

    /**
     * 检查URL是否可访问
     */
    private suspend fun checkUrlAvailability(url: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val httpUtil = HttpUtils()
            val client = httpUtil.buildClient()

            val request = Request.Builder()
                .url(url)
                .head() // 使用HEAD请求，只获取头部信息
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                Timber.d("URL检查成功: ${response.code}")
                true
            } else {
                Timber.e("URL检查失败: ${response.code} ${response.message}")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "URL检查异常")
            false
        }
    }

    /**
     * 增强的下载方法（带URL验证）
     */
    private suspend fun downloadApkWithValidation(
        url: String,
        versionCode: String
    ): File? = withContext(Dispatchers.IO) {
        // 1. 先检查URL可用性
        val isUrlValid = checkUrlAvailability(url)
        if (!isUrlValid) {
            withContext(Dispatchers.Main) {
                actionState.value = ActionState(
                    msg = "下载链接不可用，请稍后重试",
                    event = EVT_DOWNLOAD_FAILED
                )
            }
            return@withContext null
        }

        // 2. 进行下载
        return@withContext downloadApkWithProgressSync(url, versionCode)
    }

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

            // 如果文件已存在，先删除
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

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
            while (isDownloading) {
                delay(1000) // 每秒查询一次

                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)

                if (cursor.moveToFirst()) {
                    val status =
                        cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val downloaded =
                        cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total =
                        cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                    when (status) {
                        DownloadManager.STATUS_RUNNING -> {
                            // 更新下载进度
                            val progress = if (total > 0) {
                                (downloaded * 100 / total)
                            } else 0

                            withContext(Dispatchers.Main) {
                                actionState.value = ActionState(
                                    event = EVT_DOWNLOADING,
                                    msg = "正在下载新版本... $progress%"
                                )
                            }
                        }

                        DownloadManager.STATUS_SUCCESSFUL -> {
                            Timber.d("下载完成")
                            cursor.close()
                            isDownloading = false
                            return@withContext destinationFile
                        }

                        DownloadManager.STATUS_FAILED -> {
                            cursor.close()
                            isDownloading = false
                            return@withContext null
                        }
                    }
                }
                cursor.close()
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

                // 启动安装界面
                context.startActivity(intent)
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