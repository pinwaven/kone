package poct.device.app.utils.app

/**
 * 版本工具类
 */
object VersionUtils {

    /**
     * 比较两个版本号
     * @return -1: version1 < version2, 0: version1 == version2, 1: version1 > version2
     */
    fun compare(version1: String, version2: String): Int {
        // 清理版本号，移除前缀如"v"、"V"
        val v1 = cleanVersion(version1)
        val v2 = cleanVersion(version2)

        // 按"."分割成数字数组
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }

        // 比较每个部分
        val maxLength = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLength) {
            val num1 = parts1.getOrElse(i) { 0 }
            val num2 = parts2.getOrElse(i) { 0 }

            when {
                num1 < num2 -> return -1
                num1 > num2 -> return 1
                // 相等则继续比较下一部分
            }
        }

        return 0 // 所有部分都相等
    }

    /**
     * 检查 version1 是否小于 version2
     */
    fun isLessThan(version1: String, version2: String): Boolean {
        return compare(version1, version2) < 0
    }

    /**
     * 检查 version1 是否小于或等于 version2
     */
    fun isLessThanOrEqual(version1: String, version2: String): Boolean {
        return compare(version1, version2) <= 0
    }

    /**
     * 检查 version1 是否大于 version2
     */
    fun isGreaterThan(version1: String, version2: String): Boolean {
        return compare(version1, version2) > 0
    }

    /**
     * 检查 version1 是否大于或等于 version2
     */
    fun isGreaterThanOrEqual(version1: String, version2: String): Boolean {
        return compare(version1, version2) >= 0
    }

    /**
     * 清理版本号字符串
     * 移除 "v"、"V"、"version" 等前缀，并处理可能的尾部信息
     */
    private fun cleanVersion(version: String): String {
        return version
            .lowercase() // 转为小写
            .replace("^[vversion:\\s]+".toRegex(), "") // 移除前缀
            .split("[^0-9.]".toRegex()) // 按非数字非点号分割
            .first() // 取第一部分
            .trimEnd('.') // 移除末尾的点号
    }

    /**
     * 获取版本号的数字部分数组
     */
    fun getVersionParts(version: String): List<Int> {
        val cleaned = cleanVersion(version)
        return cleaned.split(".").map { it.toIntOrNull() ?: 0 }
    }
}