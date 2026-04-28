package poct.device.app.bean

data class PrinterInfo(
    /**
     * 名称
     */
    val name: String = "",
    /**
     * 连接状态
     */
    val connected: Boolean = false,

    ) {
    companion object {
        val Empty = PrinterInfo()
    }
}
