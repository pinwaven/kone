package poct.device.app.thirdparty.model.fros.req

class UploadCheckDataReq(
    var code: String = "",
//    var type: String = "",
    var status: String = "completed",
//    var patient: String = "",
    var date: String = "",
    var result: List<UploadCheckDataResult> = ArrayList(),
)

class UploadCheckDataResult(
    var name: String = "",
    var result: String = "",
    var radioValue: String = "",
    var refer: String = "",
    var t1Value: String = "",
    var t2Value: String = "",
    var t3Value: String = "",
    var t4Value: String = "",
    var cValue: String = "",
    var c2Value: String = "",

    var t1ValueName: String = "",
    var t2ValueName: String = "",
    var t3ValueName: String = "",
    var t4ValueName: String = "",

    var t1ValueStr: String = "",
    var t2ValueStr: String = "",
    var t3ValueStr: String = "",
    var t4ValueStr: String = "",
)