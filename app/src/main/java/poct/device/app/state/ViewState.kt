package poct.device.app.state

/**
 * *
 * @date ：2024/4/14
 * @desc：基类状态密封类
 */
sealed class ViewState {

    /**
     * 默认状态 需要加载等
     */
    object Default : ViewState()

    /**
     * 正在加载
     */
    data class Loading(val msg: String? = null) : ViewState()

    /**
     * 正在加载(浮在界面上面)
     */
    data class LoadingOver(val msg: String? = null) : ViewState()

    /**
     * 加载失败
     * @param msg 加载失败的信息
     */
    data class LoadError(val msg: String) : ViewState()

    /**
     * 加载成功
     */
    data class LoadSuccess(val msg: String? = null) : ViewState()

    companion object {

    }
}
