package poct.device.app.thirdparty.model.sbedge.resp

class SbEdgeBaseResp<T>(
    var code: String = "",
    var msg: String = "",
    var data: T? = null,
) {
    fun isSuccess(): Boolean {
        return code == "200"
    }
}