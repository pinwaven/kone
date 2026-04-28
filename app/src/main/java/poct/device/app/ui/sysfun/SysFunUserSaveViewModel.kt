package poct.device.app.ui.sysfun

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.entity.User
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.utils.app.AppFormValidateUtils

class SysFunUserSaveViewModel : ViewModel() {
    // 视图状态
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    val actionState = MutableStateFlow(ActionState.Default)

    // 记录列表
    val record = MutableStateFlow<User>(User.Empty)

    fun onLoad() {
        viewState.value = ViewState.LoadingOver()
        viewModelScope.launch(Dispatchers.IO) {
            record.value = AppParams.varUser
            viewState.value = ViewState.LoadSuccess()
        }
    }

    fun onClearInteraction() {
        actionState.value = ActionState.Default
    }

    // 退出确认
    fun onBackConfirm() {
        actionState.value = ActionState(event = EVT_EXIT)
    }

    fun onSave(user: User) {
        if (AppParams.varUserMode == "add") {
            addUser(user)
        } else {
            updateUser(user)
        }
    }

    private fun updateUser(user: User) {
        actionState.value = ActionState(EVT_LOADING)
        viewModelScope.launch(Dispatchers.IO) {
            if (!validateInputForUpdate(user)) return@launch
            val username = user.username
            val userDao = App.getDatabase().userDao()
            val existUser = userDao.findByUsername(username)
            if (existUser == null) {
                actionState.value = ActionState(
                    EVT_SAVE_FAILED,
                    App.getContext().getString(R.string.msg_user_not_exists)
                )
            } else {
                AppParams.curUser = User.Empty
                userDao.update(user)
                actionState.value = ActionState(
                    event = EVT_SAVE_DONE,
                    msg = App.getContext().getString(R.string.sys_fun_xtpz_user_save_done),
                    payload = user.username
                )
            }
        }
    }

    private fun addUser(user: User) {
        if (!validateInputForAdd(user)) return
        actionState.value = ActionState(EVT_LOADING)
        viewModelScope.launch(Dispatchers.IO) {
            val username = user.username
            val userDao = App.getDatabase().userDao()
            val existUser = userDao.findByUsername(username)
            if (existUser != null) {
                actionState.value = ActionState(
                    EVT_SAVE_FAILED,
                    App.getContext().getString(R.string.msg_user_username_exists)
                )
            } else {
                AppParams.curUser = User.Empty
                userDao.add(user)
            }
            actionState.value = ActionState(
                event = EVT_SAVE_DONE,
                msg = App.getContext().getString(R.string.sys_fun_xtpz_user_save_done),
                payload = user.username
            )
        }
    }

    fun onSaveDone(callback: () -> Unit = {}) {
        callback()
        onClearInteraction()
    }

    private fun validateInputForUpdate(user: User): Boolean {
        val pwd = user.pwd
        val nickname = user.nickname
        if (!AppFormValidateUtils.validateRequired(pwd)) {
            actionState.value = ActionState(
                EVT_SAVE_FAILED,
                App.getContext().getString(R.string.msg_user_pwd_required)
            )
            return false
        }
        if (!AppFormValidateUtils.validateRequired(nickname)) {
            actionState.value = ActionState(
                EVT_SAVE_FAILED,
                App.getContext().getString(R.string.msg_user_nickname_required)
            )
            return false
        }
        return true
    }

    private fun validateInputForAdd(user: User): Boolean {
        val username = user.username
        val pwd = user.pwd
        val nickname = user.nickname
        if (!AppFormValidateUtils.validateRequired(username)) {
            actionState.value = ActionState(
                EVT_SAVE_FAILED,
                App.getContext().getString(R.string.msg_user_username_required)
            )
            return false
        }
        if (!AppFormValidateUtils.validateIden(username)) {
            actionState.value = ActionState(
                EVT_SAVE_FAILED,
                App.getContext().getString(R.string.msg_user_username_invalid)
            )
            return false
        }

        if (!AppFormValidateUtils.validateRequired(pwd)) {
            actionState.value = ActionState(
                EVT_SAVE_FAILED,
                App.getContext().getString(R.string.msg_user_pwd_required)
            )
            return false
        }
        if (!AppFormValidateUtils.validateRequired(nickname)) {
            actionState.value = ActionState(
                EVT_SAVE_FAILED,
                App.getContext().getString(R.string.msg_user_nickname_required)
            )
            return false
        }
        return true
    }

    companion object {
        const val EVT_EXIT = "exit"
        const val EVT_LOADING = "EVT_LOADING"
        const val EVT_SAVE_FAILED = "saveFailed"
        const val EVT_SAVE_DONE = "saveDone"
    }

}