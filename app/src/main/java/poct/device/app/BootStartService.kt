package poct.device.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class BootStartService : Service() {
    override fun onCreate() {
        super.onCreate()
        // 1. 必须创建通知渠道（Android 8.0+）
        createNotificationChannel()

        // 2. 启动前台服务（Android 9+ 必须在前台服务启动后5秒内显示通知）
        startForeground(NOTIFICATION_ID, buildNotification())

        // 3. 执行实际启动逻辑
        startMainActivity()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Boot Service",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Service running after device boot"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("System Initializing")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true) // 用户无法滑动清除
            .setTimeoutAfter(5000) // 5秒后自动消失
            .setContentIntent(pendingIntent) // 使用可变PendingIntent
            .build()
    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        stopSelf() // 任务完成后自动停止服务
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "boot_service_channel"
        private const val NOTIFICATION_ID = 1001
    }
}