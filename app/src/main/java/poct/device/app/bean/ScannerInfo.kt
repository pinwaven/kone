package poct.device.app.bean

import android.bluetooth.BluetoothDevice

data class ScannerInfo(
    /**
     * 信号
     */
    val address: String = "",
    /**
     * 名称
     */
    val name: String = "",
    /**
     * 名称
     */
    val device: BluetoothDevice? = null,
    /**
     * 连接状态
     */
    var connected: Boolean = false,
    ) {
    companion object {
        val Empty = ScannerInfo()
    }
}
