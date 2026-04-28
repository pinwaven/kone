package poct.device.app

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

class MyDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(context: Context, intent: Intent) {
        println("设备管理员已启用")
    }

    override fun onDisabled(context: Context, intent: Intent) {
        println("设备管理员已禁用")
    }
}