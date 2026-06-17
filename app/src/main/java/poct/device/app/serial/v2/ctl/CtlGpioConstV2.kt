package poct.device.app.serial.v2.ctl

object CtlGpioConstV2 {
    const val GPIO_SS_POWER: Byte = 0
    const val GPIO_SS_UD: Byte = 1
    const val GPIO_SS_START: Byte = 2
    const val GPIO_SS_STOP: Byte = 3
    const val GPIO_SS_CARD: Byte = 4

    // 激光电源开关
    const val GPIO_LD_POWER: Byte = 5
    const val GPIO_QR_TGL: Byte = 6
    const val GPIO_QR_RST: Byte = 7
}