package poct.device.app.ui.sysfun

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.entity.User
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.utils.app.AppUserUtils
import java.util.Collections

class SysFunUserViewModel : ViewModel() {
    // 视图状态
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    val actionState = MutableStateFlow(ActionState.Default)

    // 记录列表
    val records = MutableStateFlow<List<User>>(Collections.emptyList())

    fun onLoad() {
        viewState.value = ViewState.LoadingOver()
        viewModelScope.launch {
            val userList = withContext(Dispatchers.IO) {
                val userDao = App.getDatabase().userDao()
                userDao.findAll() ?: Collections.emptyList()
            }
            delay(300)
            records.value = userList
            viewState.value = ViewState.LoadSuccess()
        }
    }

    fun onAdd(callback: () -> Unit = {}) {
        AppParams.varUser = User(role = User.ROLE_CHECKER)
        AppParams.varUserMode = "add"
        callback()
    }

    fun onDeleteConfirm(user: User) {
        AppParams.varUser = user
        actionState.value = ActionState(event = EVT_DEL_CONFIRM)
    }

    fun onDelete(user: User) {
        actionState.value = ActionState(EVT_LOADING)
        viewModelScope.launch {
            if (AppUserUtils.isDefault(user.username)) {
                delay(300)
                actionState.value = ActionState(
                    event = EVT_ERROR,
                    msg = App.getContext().getString(R.string.msg_user_default_delete_deny)
                )
            } else {
                withContext(Dispatchers.IO) {
                    App.getDatabase().userDao().delete(user)
                }
                delay(300)
                actionState.value = ActionState(event = EVT_DEL_DONE)
            }
        }
    }

    fun onDeleteDone() {
        viewModelScope.launch {
            val userList = withContext(Dispatchers.IO) {
                val userDao = App.getDatabase().userDao()
                userDao.findAll() ?: Collections.emptyList()
            }
            records.value = userList
            onClearInteraction()
        }
    }

    fun onEdit(user: User, callback: () -> Unit = {}) {
        AppParams.varUser = user
        AppParams.varUserMode = "edit"
        callback()
    }

    fun onClearInteraction() {
        actionState.value = ActionState.Default
    }


    companion object {
        const val EVT_LOADING = "EVT_LOADING"
        const val EVT_ERROR = "error"
        const val EVT_DEL_CONFIRM = "delConfirm"
        const val EVT_DEL_DONE = "delDone"
    }

}