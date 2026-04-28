package poct.device.app.utils.app

object AppFormUtils {
    /**
     * 规范字符串长度
     */
    fun regulateLength(value: String, length: Int): String {
        val regulated = value.trim()
        return if (regulated.length <= length) {
            regulated
        } else {
            regulated.substring(0, length)
        }
    }
}