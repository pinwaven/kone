package poct.device.app.ui.aftersale

import android.annotation.SuppressLint
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import poct.device.app.App
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import timber.log.Timber

class AfterSaleViewModel : ViewModel() {
    // 视图状态
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    val actionState = MutableStateFlow(ActionState.Default)

    fun onClearInteraction() {
        actionState.value = ActionState.Default
    }


    fun onUpgradeSunConfirm() {
        actionState.value = ActionState(event = SALE_SUN_LOGIN)
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun onUpgradeSunLogin() {
        val packageManager: PackageManager = App.getContext().packageManager
        val installedApplications =
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        var applicationInfo: ApplicationInfo? = null
        for (i in installedApplications.indices) {
            val appInfo = installedApplications[i]
            val packageName = appInfo.packageName
            if (packageName.startsWith("com.oray.sunlogin")) {
                applicationInfo = appInfo
                break
            }
        }
        if (applicationInfo != null) {
            val intent = packageManager.getLaunchIntentForPackage(applicationInfo.packageName)
            if (intent != null) {
                actionState.value = ActionState(event = SALE_SUN_BUT)
                // 打开远程软件
                App.getContext().startActivity(intent)
            }
        } else {
            Timber.w("未安装远程协助应用")
        }
        //actionState.value = ActionState.Default
    }

    companion object {
        const val SALE_SUN_LOGIN = "saleSunLogin"
        const val SALE_SUN_BUT = "saleSunBut"
    }
}