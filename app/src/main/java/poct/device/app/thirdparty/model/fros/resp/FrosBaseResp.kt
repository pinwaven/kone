package poct.device.app.thirdparty.model.fros.resp

class FrosBaseResp<T>(
    var success: Boolean = false,
    var message: String = "",
    var result: T? = null,
) {
    fun isSuccess(): Boolean {
        return success
    }
}