package poct.device.app.serial

data class SerialConnectEvent(
    val state: Int = -1 // -1：代表串口故障（30s无响应）， 0：串口断开（15S无响应）  1：串口连接
) {

}