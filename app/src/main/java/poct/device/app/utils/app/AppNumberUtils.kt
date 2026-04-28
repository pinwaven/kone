package poct.device.app.utils.app

object AppNumberUtils {
    fun toFloat(value: String) : Float {
        return try {
            value.toFloat()
        } catch (e: Exception) {
            0F
        }
    }
    fun toInt(value: String) : Int {
        return try {
            value.toInt()
        } catch (e: Exception) {
            0
        }
    }
}