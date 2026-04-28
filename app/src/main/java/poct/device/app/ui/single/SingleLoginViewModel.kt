package poct.device.app.ui.single

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.bean.LoginBean
import poct.device.app.entity.User
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.utils.app.AppFormValidateUtils
import java.time.LocalDateTime

class SingleLoginViewModel : ViewModel() {
    // 视图状态
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    val actionState = MutableStateFlow(ActionState.Default)

    val loginBean = MutableStateFlow(LoginBean.Empty)

    fun onLoad() {
        viewState.value = ViewState.LoadingOver(App.getContext().getString(R.string.msg_sys_init))
        viewModelScope.launch {
//            AppWifiManager.openWifi(App.getWifiManager())
            viewState.value = ViewState.LoadSuccess()
        }
    }

    fun onClearInteraction() {
        actionState.value = ActionState.Default
    }

    fun onLogin(callback: () -> Unit = {}) {
        viewState.value = ViewState.LoadingOver()
        if (!validateInput()) {
            viewState.value = ViewState.LoadSuccess()
            return
        }

        val username = loginBean.value.username
        val password = loginBean.value.password

        viewModelScope.launch {
            val user = withContext(Dispatchers.IO) {
                val userDao = App.getDatabase().userDao()
                userDao.findByUsername(username)
            }
            if (user == null || user.pwd != password) {
                actionState.value = ActionState(
                    EVT_LOGIN_FAILED,
                    App.getContext().getString(R.string.msg_login_mismatch)
                )
            } else {
                AppParams.curUser = User(
                    username = username,
                    role = user.role,
                    pwd = password,
                    loginTime = LocalDateTime.now()
                )
                callback()
            }
            viewState.value = ViewState.LoadSuccess()

//            // 当前开发调试用
//            val user = userDao.findByUsername("admin")!!.copy(loginTime = LocalDateTime.now())
//            userDao.update(user)
//            AppParams.curUser = user
//            callback()
        }
    }

    fun onLoginWithDefaultUser(callback: () -> Unit = {}) {
        val username = "admin"
        val password = "888888"

        viewModelScope.launch {
            val user = withContext(Dispatchers.IO) {
                val userDao = App.getDatabase().userDao()
                userDao.findByUsername(username)
            }
            if (user == null || user.pwd != password) {
                actionState.value = ActionState(
                    EVT_LOGIN_FAILED,
                    App.getContext().getString(R.string.msg_login_mismatch)
                )
            } else {
                AppParams.curUser = User(username = username, role = user.role)
                callback()
            }
        }
    }

    private fun validateInput(): Boolean {
        val username = loginBean.value.username
        val password = loginBean.value.password
        if (!AppFormValidateUtils.validateRequired(username)) {
            actionState.value = ActionState(
                EVT_LOGIN_FAILED,
                App.getContext().getString(R.string.msg_login_username_required)
            )
            return false
        }
        if (!AppFormValidateUtils.validateRequired(password)) {
            actionState.value = ActionState(
                EVT_LOGIN_FAILED,
                App.getContext().getString(R.string.msg_login_pwd_required)
            )
            return false
        }
        return true
    }

    companion object {
        const val EVT_LOGIN_FAILED = "loginFailed"
    }
}