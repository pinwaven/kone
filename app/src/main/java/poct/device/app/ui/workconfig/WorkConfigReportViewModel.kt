package poct.device.app.ui.workconfig

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import poct.device.app.App
import poct.device.app.R
import poct.device.app.bean.ConfigReportBean
import poct.device.app.entity.service.SysConfigService
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.utils.app.AppFileUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class WorkConfigReportViewModel : ViewModel() {
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    val actionState = MutableStateFlow(ActionState.Default)

    val showTemp = MutableStateFlow(false)
    val mode = MutableStateFlow("view")

    val bean = MutableStateFlow(ConfigReportBean.Empty)

    fun onLoad() {
        viewState.value = ViewState.LoadingOver()
        viewModelScope.launch(Dispatchers.IO) {
            val configBean = SysConfigService.findBean(ConfigReportBean.PREFIX, ConfigReportBean::class)
            bean.value = configBean
            viewState.value = ViewState.LoadSuccess()
        }
    }

    fun onClearInteraction() {
        actionState.value = ActionState.Default
    }

    fun onBeanUpdate(newBean: ConfigReportBean) {
        bean.value = newBean
    }

    fun onShowTempUpdate(value: Boolean) {
        showTemp.value = value
    }

    fun onModifyPre() {
        mode.value = "modify"
    }

    // 退出确认
    fun onBackConfirm() {
        actionState.value = ActionState(event= EVT_EXIT)
    }

    fun onBack() {
        // 删除临时存储目录里面的文件
        val tempFile = File(AppFileUtils.getLogoUrlTemp())
        if(tempFile.exists()) {
            tempFile.delete()
        }
        mode.value = "view"
    }

    fun onSave() {
        actionState.value = ActionState(
            event = EVT_LOADING,
            msg = App.getContext().getString(R.string.action_saving)
        )
        viewModelScope.launch(Dispatchers.IO) {
            SysConfigService.saveBean(ConfigReportBean.PREFIX, bean.value)
            actionState.value = ActionState(EVT_SAVE_DONE)
            // copy临时文件到正式文件中
            val tempFile = File(AppFileUtils.getLogoUrlTemp())
            if(tempFile.exists()) {
                val filePath = AppFileUtils.getLogoUrl()
                val file = File(filePath)
                if (!file.parentFile?.exists()!!) {
                    file.parentFile?.mkdirs()
                }
                if (file.exists()) {
                    file.delete()
                }
                val inputStream = FileInputStream(tempFile)

                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                // 删除临时存储目录里面的文件
                tempFile.delete()
            }
        }
    }

    fun onSaveSuccess() {
        mode.value = "view"
        onClearInteraction()
    }
    fun onCopyImageToAssets(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val filePath = AppFileUtils.getLogoUrlTemp()
            val file = File(filePath)
            if (!file.parentFile?.exists()!!) {
                file.parentFile?.mkdirs()
            }
            if (file.exists()) {
                file.delete()
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            showTemp.value = true
        }
    }
    companion object {
        const val EVT_LOADING = "loading"
        const val EVT_ERROR = "error"
        const val EVT_SAVE_DONE = "saveDone"
        const val EVT_EXIT = "exit"
    }

}