package poct.device.app.bean

import poct.device.app.thirdparty.SbEdgeFunc

data class ConfigInfoV2Bean(
    var name: String = "",
    var code: String = "",
    var type: String = "",
    var software: String = "",
    var hardware: String = "",
) : ConfigBean {
    companion object {
        val DefaultCode = "KINO-A1-0000000"
        val Empty = ConfigInfoV2Bean()
    }

    fun hasData(): Boolean {
        return name.isNotEmpty() && code.isNotEmpty()
                && type.isNotEmpty() && software.isNotEmpty()
                && hardware.isNotEmpty()
                && name != SbEdgeFunc.EMPTY_VAL && code != SbEdgeFunc.EMPTY_VAL
                && type != SbEdgeFunc.EMPTY_VAL && software != SbEdgeFunc.EMPTY_VAL
                && hardware != SbEdgeFunc.EMPTY_VAL
    }
}
