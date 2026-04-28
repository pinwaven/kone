package poct.device.app.utils.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import poct.device.app.AppParams
import poct.device.app.event.AppBatteryEvent
import poct.device.app.utils.common.EventUtils
import timber.log.Timber

class AppBatteryReceiverHelper {
    companion object {
        // 在应用启动时调用一次，立即同步电量
        fun initBatteryOnAppStart(context: Context) {
            val batteryStatus = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )

            batteryStatus?.let { intent ->
                val current = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val total = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (current >= 0 && total > 0) {
                    val percent = current * 100 / total
                    // 强制立即更新，不使用滤波
                    AppBatteryUtils.forceUpdateDisplayPercent(percent.toFloat())
                    Timber.w(
                        "应用启动时初始化电量: 实际=%s, 显示=%s",
                        percent,
                        AppBatteryUtils.getCurrentDisplayPercent()
                    )
                }
            }
        }
    }

    val batteryStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val current = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val total = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) != 0

            if (current < 0 || total <= 0) return

            val percent = current * 100 / total
//            Timber.w("电池广播: 实际电量=%s%, 充电状态=%s", percent, plugged)

            // 更新充电状态
            AppBatteryUtils.setChargingState(plugged)

            // 获取显示电量
            val displayPercent = AppBatteryUtils.updateAndGetDisplayPercent(percent.toFloat())
            val displayPercentInt = displayPercent.toInt()

            // 存储到全局参数
            AppParams.battery = displayPercentInt
            AppParams.batteryPlugged = plugged

//            Timber.w("处理后电量: 显示电量=%s", displayPercentInt)

            // 发送事件
            EventUtils.publishEvent(AppBatteryEvent(displayPercentInt, plugged))
        }
    }
}