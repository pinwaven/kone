package poct.device.app

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompleteReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        startApp(context)
    }

    private fun startApp(context: Context) {
        // 方式1：启动Activity（需FLAG_ACTIVITY_NEW_TASK）
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(launchIntent)

        // 方式2：启动前台服务（Android 8.0+ 需Notification）
        context.startForegroundService(Intent(context, BootStartService::class.java))
    }
}