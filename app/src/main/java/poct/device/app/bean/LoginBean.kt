package poct.device.app.bean

data class LoginBean(
    var username: String = "",
    var password: String = "",
) {
    companion object {
        val Empty = LoginBean()
    }
}
