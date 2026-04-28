package poct.device.app.utils.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.utils.OnSingleClickListener
import timber.log.Timber
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.lang.reflect.InvocationTargetException
import java.nio.charset.Charset
import java.time.LocalDateTime

// 实际为初始
object AppSystemUtilsV2 {
    fun restartApp() {
        val intent = App.getContext().packageManager.getLaunchIntentForPackage(
            App.getContext().packageName
        )
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        App.getContext().startActivity(intent)
        Process.killProcess(Process.myPid())
    }

    fun setTime(dateTime: LocalDateTime) {
        val cmd = "date ${AppLocalDateUtils.formatForSet(dateTime)}"
        runCommand(cmd)
    }

    fun setTimeZone(id: String) {
        val cmd = "setprop persist.sys.timezone ${id}"
        runCommand(cmd)
    }

    fun setTimeAuto(checked: Boolean) {
        val checkedInt = if (checked) 1 else 0
        runCommand("settings put global auto_time $checkedInt")
        runCommand("settings put global auto_time_zone $checkedInt")
    }

    /**
     * 关闭设备
     */
    fun shutdown() {
        runCommand("reboot -p")
    }


    /**
     * 重启设备
     */
    fun reboot() {
        runCommand("reboot")
    }

    /**
     * 返回上一步
     */
    fun backUp() {
        runCommand("input keyevent " + KeyEvent.KEYCODE_BACK)
    }

    fun goToSleep(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        try {
            powerManager.javaClass.getMethod(
                "goToSleep",
                Long::class.javaPrimitiveType
            ).invoke(powerManager, SystemClock.uptimeMillis())
        } catch (e: IllegalAccessException) {
            Timber.tag("cmd").d(e.stackTraceToString())
        } catch (e: InvocationTargetException) {
            Timber.tag("cmd").d(e.stackTraceToString())
        } catch (e: NoSuchMethodException) {
            Timber.tag("cmd").d(e.stackTraceToString())
        }
    }

    fun wakeUp(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        try {
            powerManager.javaClass.getMethod(
                "wakeUp",
                Long::class.javaPrimitiveType
            ).invoke(powerManager, SystemClock.uptimeMillis())
        } catch (e: IllegalAccessException) {
            Timber.tag("cmd").d(e.stackTraceToString())
        } catch (e: InvocationTargetException) {
            Timber.tag("cmd").d(e.stackTraceToString())
        } catch (e: NoSuchMethodException) {
            Timber.tag("cmd").d(e.stackTraceToString())
        }
    }

    /**
     * 扩展板电源打开
     */
    fun powerOnCtlBoard() {
        setGpio(6, 1)
        setGpio(7, 1)
    }

    /**
     * 扩展板电源关闭
     */
    fun powerOffCtlBoard() {
        setGpio(6, 0)
        setGpio(7, 0)
    }

    /**
     * GPIO通信
     * pin：引脚
     * value：0--低电平，代表关闭，1-高电平，代表打开
     */
    private fun setGpio(pin: Int, value: Int) {
        val cmd = "echo out $pin $value >/sys/devices/platform/1000b000.pinctrl/mt_gpio"
        runCommand(cmd)
    }

    private fun runCommand(command: String) {
//        val result = false
        var dataOutputStream: DataOutputStream? = null
        val errorStream: BufferedReader? = null
        try {
            val process = Runtime.getRuntime().exec("su")
            dataOutputStream = DataOutputStream(process.outputStream)
            val command = "${command}\n"
            dataOutputStream.write(command.toByteArray(Charset.forName("utf-8")))
            dataOutputStream.flush()
            dataOutputStream.writeBytes("exit\n")
            dataOutputStream.flush()
            object : Thread() {
                override fun run() {
                    val ins = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    try {
                        while (ins.readLine().also { line = it } != null) {
                            Timber.d("datax: $line")
                        }
                    } catch (e: IOException) {
                        Timber.e(e.message!!)
                    } finally {
                        try {
                            ins.close()
                        } catch (e: IOException) {
                            Timber.e(e.message!!)
                        }
                    }
                }
            }.start()
            object : Thread() {
                override fun run() {
                    val err = BufferedReader(InputStreamReader(process.errorStream))
                    var line: String?
                    val result = StringBuilder()
                    try {
                        while (err.readLine().also { line = it } != null) {
                            result.append(line)
                            Timber.d("ssss: $line")
                        }
                    } catch (e: IOException) {
                        Timber.e(e.message!!)
                    } finally {
                        try {
                            err.close()
                        } catch (e: IOException) {
                            Timber.e(e.message!!)
                        }
                    }
                }
            }.start()
            process.waitFor()
        } catch (e: Exception) {
            Timber.e("sssv %s", e.message)
        } finally {
            try {
                dataOutputStream?.close()
                errorStream?.close()
            } catch (e: IOException) {
                Timber.e(e.message!!)
            }
        }
    }

    fun isAppForeground(context: Context, packageName: String): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningProcesses: List<ActivityManager.RunningAppProcessInfo> = am.runningAppProcesses
        for (runningProcess in runningProcesses) {
            if (runningProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                runningProcess.processName.equals(packageName)
            ) {
                return true
            }
        }
        return false
    }

    fun createFloatingWindow(context: Context, windowManager: WindowManager) {
        AppParams.resumeStatus = false
        // 浮窗按钮
        val floatButton = Button(context).apply {
            text = "返回"
            textSize = 16F
            width = 80
            height = 32
            setTextColor(getContext().getColor(R.color.white))
            setBackgroundColor(getContext().getColor(R.color.primaryColor))
            setOnClickListener(object : OnSingleClickListener() {
                override fun onClicked(v: View?) {
                    // 当按钮被点击时，回到系统设置界面
                    Timber.w("===回到系统界面====")
                    if (!AppParams.resumeStatus) {
                        backUp()
                    }
                }
            })
        }
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.BOTTOM
            x = 260 // 初始位置
            y = 100 // 初始位置
        }
        windowManager.addView(floatButton, layoutParams)
        Thread {
            while (true) {
                // 每3秒检查一次
                Thread.sleep(3000)
                Timber.w("当前页面是否再顶层：${AppParams.resumeStatus}")
                if (AppParams.resumeStatus) {
                    Timber.w("当前页面是否再顶层：${floatButton.windowToken}")
                    windowManager.removeView(floatButton)
                    AppParams.resumeStatus = false
                    break
                }
            }
        }.start()
    }
}