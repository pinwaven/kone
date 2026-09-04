package poct.device.app.utils.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.BoardPowerAttemptReason
import poct.device.app.event.AppBatteryEvent
import poct.device.app.utils.common.EventUtils
import timber.log.Timber

/**
 * 原始电量快照，不经过 AppBatteryUtils 的平滑/锁定，供 BoardPowerGuard 使用。
 * plugged 为 null 表示无法确认充电状态（ACTION_BATTERY_CHANGED 未携带 EXTRA_PLUGGED）。
 */
data class BatterySnapshot(val percent: Int, val plugged: Boolean?)

class AppBatteryReceiverHelper {
    companion object {
        /**
         * 将 EXTRA_PLUGGED 的原始值映射为充电状态。未知/缺失的值一律返回 null，
         * 而不是当作"未插电"或"已插电"，避免误判（此前 `!= 0` 的写法会把 extra
         * 缺失时的默认值 -1 误判为"已插电"）。
         */
        private fun readPluggedState(pluggedExtra: Int): Boolean? {
            return when (pluggedExtra) {
                BatteryManager.BATTERY_PLUGGED_AC,
                BatteryManager.BATTERY_PLUGGED_USB,
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> true
                0 -> false
                else -> null
            }
        }

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

        /**
         * 同步读取一次原始电量与充电状态，用于 BoardPowerGuard 的电量快照与阻断判断。
         */
        fun readRawBatteryOnce(context: Context): BatterySnapshot {
            val batteryStatus = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )

            val current = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val total = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val percent = if (current >= 0 && total > 0) current * 100 / total else -1
            val plugged = readPluggedState(batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: -1)

            return BatterySnapshot(percent, plugged)
        }
    }

    private var lastPluggedState: Boolean? = null

    val batteryStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val current = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val total = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val pluggedState = readPluggedState(intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1))
            val plugged = pluggedState == true

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

            // 充电器插入边沿触发：仅在主流程因 board power guard 被锁定时重试上电
            val wasPlugged = lastPluggedState
            lastPluggedState = pluggedState
            if (wasPlugged == false && plugged && AppParams.boardPowerBlocked.value) {
                Timber.w("charger plugged while board power blocked, retry board power on")
                App.getContext().attemptBoardPowerOn(BoardPowerAttemptReason.CHARGER_PLUGGED)
            }
        }
    }
}
