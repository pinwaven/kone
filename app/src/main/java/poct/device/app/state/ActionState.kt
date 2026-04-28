package poct.device.app.state

/**
 * *
 * @date ：2024/4/14
 * @desc：基类状态密封类
 */
class ActionState(
    val event: String = "",
    val msg: String? = null,
    val payload: Any? = null,
) {
    companion object {
        val Default = ActionState()
    }
}
