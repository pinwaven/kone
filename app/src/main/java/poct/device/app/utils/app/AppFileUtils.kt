package poct.device.app.utils.app

import android.os.Environment
import android.text.TextUtils
import java.io.File

object AppFileUtils {
    // 被替代的原来的为com.suregna.kts
    private const val PACKAGE_NAME = "poct.device.app"
    private const val data_iot = "1_data_iot"

    /**
     * 内部目录根目录，不对外开放（文件查看需要ROOT），和应用同生命周期
     */
    fun getInnerRoot(): String {
        // context.getDataDir();
        return "/data/data/$PACKAGE_NAME"
    }
    /**
     * LOGO地址
     */
    fun getLogoUrl(): String {
        return Environment.getExternalStorageDirectory().absolutePath + File.separator + "Android" + File.separator + "Nanovate_AI" + File.separator + "logo" + File.separator + "logo.png"
    }

    /**
     * LOGO临时地址
     */
    fun getLogoUrlTemp(): String {
        return Environment.getExternalStorageDirectory().absolutePath + File.separator + "Android" + File.separator + "Nanovate_AI" + File.separator + "logo" + File.separator + "temp.png"
    }

    /**
     * LOGO地址
     */
    fun getQrCodeUrl(): String {
        return Environment.getExternalStorageDirectory().absolutePath + File.separator + "Android" + File.separator + "Nanovate_AI" + File.separator + "qrCode" + File.separator
    }

    /**
     * 私有目录根目录，对外开放，和应用同生命周期
     */
    fun getPrivateRoot(): String {
        return "/storage/emulated/0/Android/data/$PACKAGE_NAME"
    }

    fun getBaseFileDirPath(): String? {
        val dataDir = File(
            Environment.getExternalStorageDirectory()
                .toString() + File.separator + "Android" + File.separator + "Nanovate_AI"
        )
        dataDir.mkdirs()
        return dataDir.absolutePath
    }

    /**
     * apk升级文件
     *
     * @return
     */
    fun getUpgradeApkPath(): String {
        val dir = TextUtils.concat(
            getBaseFileDirPath(),
            File.separator,
            data_iot
        ).toString()
        mkdirs(dir)
        val fileName = "Nanovate_AI.apk"
        return dir + File.separator + fileName
    }

    /**
     * apk升级文件
     *
     * @return
     */
    fun getUpgradeApkMarkPath(): String {
        val dir = TextUtils.concat(
            getBaseFileDirPath(),
            File.separator,
            data_iot
        ).toString()
        mkdirs(dir)
        val fileName = "upgrade.txt"
        return dir + File.separator + fileName
    }

    /**
     * 硬件升级文件
     *
     * @return
     */
    fun getHardWareApkPath(): String {
        val dir = TextUtils.concat(
            getBaseFileDirPath(),
            File.separator,
            data_iot,
            File.separator,
            "hardware"
        ).toString()
        mkdirs(dir)
        val fileName = "wlk01c01.bin"
        return dir + File.separator + fileName
    }

    private fun mkdirs(path: String): Boolean {
        if(AppStringUtils.isEmpty(path)) {
            return false
        }
        var newDir: File? = null
        try {
            newDir = File(path)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (null == newDir) {
            return false
        }
        if (!newDir.exists()) {
            if (!newDir.mkdirs()) {
                return false
            }
        }
        return true
    }
}