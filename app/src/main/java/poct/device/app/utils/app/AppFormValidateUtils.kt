package poct.device.app.utils.app

object AppFormValidateUtils {
    /**
     * 识别码
     */
    private val REGEX_IDEN =  Regex("^[a-zA-Z0-9_-]+$")

    /**
     * 数字，包含整数、小数
     */
    private val REGEX_NUMBER = Regex("^(-?\\d+(\\.\\d+)?)$")

    /**
     * 整数
     */
    private val REGEX_INTEGER = Regex("^([+-]?)\\d+$")

    /**
     * 正整数
     */
    private val REGEX_INTEGER_POS = Regex("^[1-9]\\d*$")

    /**
     * 验证数字
     */
    fun validateNumber(value: String): Boolean {
        return REGEX_NUMBER.matches(value)
    }

    fun validateInteger(value: String): Boolean {
        return REGEX_INTEGER.matches(value)
    }

    fun validateIntegerPos(value: String): Boolean {
        return REGEX_INTEGER_POS.matches(value)
    }

    /**
     * 验证唯一标识，仅数字、字母、下划线，其他不允许，且必须以字母开头
     */
    fun validateIden(value: String): Boolean {
        return REGEX_IDEN.matches(value)
    }

    fun validateRequired(value: String): Boolean {
        return value.isNotBlank()
    }

    fun validateLength(value: String, length: Int): Boolean {
        return value.length <= length
    }

    fun validateLengthRange(value: String, min: Int, max: Int): Boolean {
        return value.length in min..max
    }
}