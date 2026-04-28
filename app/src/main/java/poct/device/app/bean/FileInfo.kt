package poct.device.app.bean

data class FileInfo(
    var name: String = "",
    var path: String = ""
) {
    companion object {
        val Empty = FileInfo()
    }
}
