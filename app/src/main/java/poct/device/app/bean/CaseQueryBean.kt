package poct.device.app.bean

data class CaseQueryBean(
    var dateStarted: String = "",
    var dateEnded: String = "",
    var caseType: String = "",
    var name: String = "",
) {
    companion object {
        val Empty = CaseQueryBean()
    }
}
