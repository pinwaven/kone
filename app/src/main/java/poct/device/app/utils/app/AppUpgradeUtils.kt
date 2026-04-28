package poct.device.app.utils.app


object AppUpgradeUtils {
    fun upgrade(version: String) {
        val fileMap = downloadFile(version)
        upgradeSoftware(fileMap["software"]!!)
        upgradeHardware0(fileMap["hardware0"]!!)
        upgradeHardware1(fileMap["hardware1"]!!)
    }

    private fun downloadFile(version: String): Map<String, String> {
        // TODO 下载文件
        // TODO 解压文件
        return mapOf(
            "software" to "a/b/c.apk",
            "hardware1" to "a/b/c.apk",
            "hardware0" to "a/b/c.apk",
        )
    }

    private fun upgradeSoftware(file: String) {

    }

    private fun upgradeHardware0(file: String) {

    }

    private fun upgradeHardware1(file: String) {

    }
}