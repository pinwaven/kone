package poct.device.app.bean

import poct.device.app.serial.v2.CtlSerialMessageV2
import poct.device.app.serial.v2.ctl.CtlCommandsV2

data class WorkFlowActionV2(
    val type: String = TYPE_UI,
    var cmd: CtlSerialMessageV2 = CtlCommandsV2.EMPTY, // 仅串口类型时有用
    val time: Int = 0, // 秒, -1代表等待交互, -2代表等待响应
    val step: String, // 见WorkMainViewModel的Step
    val action: String, // 见WorkMainViewModel的Action
) {
    companion object {
        const val TYPE_UI = "ui"
        const val TYPE_SERIAL = "serial"
    }
}

