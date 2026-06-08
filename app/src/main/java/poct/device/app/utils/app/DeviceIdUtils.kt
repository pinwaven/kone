package poct.device.app.utils.app

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import timber.log.Timber

object DeviceIdUtils {
    private const val TAG = "DeviceIdUtils"
    private const val FC_READER_SERIAL_URI =
        "content://poct.virtualhealth.fcreader.serial/device_serial.txt"
    private val SERIAL_NUMBER_PROPERTIES = listOf(
        "ro.serialno",
        "ro.boot.serialno",
        "vendor.serialno",
        "persist.vendor.serialno",
        "persist.sys.serialno",
        "ril.serialnumber"
    )
    private const val UNKNOWN_SERIAL = "unknown"

    @Suppress("DEPRECATION")
    fun getSn(context: Context? = null): String {
        logInfo("Device SN read start, sdk=${Build.VERSION.SDK_INT}")
        val fcReaderSn = readFcReaderSn(context)
        logInfo(
            "Device SN fcreader provider len=${fcReaderSn.trim().length} " +
                "value=${maskForLog(fcReaderSn)}"
        )

        val buildSerialNew = runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // 系统应用或拥有 READ_PRIVILEGED_PHONE_STATE 权限的应用可以直接获取
                Build.getSerial()
            } else {
                @Suppress("DEPRECATION")
                Build.SERIAL
            }
        }.onFailure {
            logWarn(it, "Device SN Build.getSerial failed")
        }.getOrNull()

        val buildSerial = buildSerialNew ?: Build.SERIAL
        logInfo(
            "Device SN Build serial len=${buildSerial.orEmpty().trim().length} " +
                "value=${maskForLog(buildSerial)}"
        )

        val systemPropertySns = SERIAL_NUMBER_PROPERTIES.map(::getSystemProperty)
        val sn = selectSn(
            fcReaderSn = fcReaderSn,
            systemPropertySns = systemPropertySns,
            buildSerial = buildSerial
        )
        logInfo("Device SN selected len=${sn.length} value=${maskForLog(sn)}")
        return sn
    }

    fun selectSn(
        fcReaderSn: String? = null,
        systemPropertySns: List<String?>,
        buildSerial: String?
    ): String {
        return (listOf(fcReaderSn) + systemPropertySns + buildSerial)
            .asSequence()
            .map { it.orEmpty().trim() }
            .firstOrNull { it.isNotEmpty() && !it.equals(UNKNOWN_SERIAL, ignoreCase = true) }
            .orEmpty()
    }

    fun maskForLog(value: String?): String {
        val trimmed = value.orEmpty().trim()
        return when {
            trimmed.isEmpty() -> "<empty>"
            trimmed.length <= 4 -> "****"
            else -> "${trimmed.take(2)}***${trimmed.takeLast(4)}"
        }
    }

    fun buildGetpropCommand(name: String): String = "getprop $name"

    fun buildSuGetpropCommand(name: String): String = "su -c 'getprop $name'"

    private fun readFcReaderSn(context: Context?): String {
        if (context == null) {
            return ""
        }

        return runCatching {
            context.contentResolver
                .openInputStream(Uri.parse(FC_READER_SERIAL_URI))
                ?.bufferedReader(StandardCharsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()
                .trim()
        }.onFailure {
            logWarn(it, "Device SN fcreader provider read failed")
        }.getOrNull().orEmpty()
    }

    private fun getSystemProperty(name: String): String {
        val reflectionValue = getSystemPropertyByReflection(name)
        logPropertyResult(source = "reflection", name = name, value = reflectionValue)
        if (reflectionValue.isNotEmpty()) {
            return reflectionValue
        }

        val commandValue = getSystemPropertyByCommand(name)
        if (commandValue.isNotEmpty()) {
            return commandValue
        }

        return getSystemPropertyBySuCommand(name).also {
            if (it.isEmpty()) {
                logWarn("Device SN property $name is empty after all read methods")
            }
        }
    }

    private fun getSystemPropertyByReflection(name: String): String {
        return runCatching {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val get = systemProperties.getMethod("get", String::class.java)
            get.invoke(null, name) as? String
        }.onFailure {
            logWarn(it, "Device SN reflection read failed, property=$name")
        }.getOrNull().orEmpty()
    }

    private fun getSystemPropertyByCommand(name: String): String {
        return runShellGetpropCommand(
            source = "getprop",
            propertyName = name,
            command = buildGetpropCommand(name)
        )
    }

    private fun getSystemPropertyBySuCommand(name: String): String {
        return runShellGetpropCommand(
            source = "su",
            propertyName = name,
            command = buildSuGetpropCommand(name)
        )
    }

    private fun runShellGetpropCommand(
        source: String,
        propertyName: String,
        command: String
    ): String {
        return runCatching {
            logInfo("Device SN $source command=$command")
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                logWarn("Device SN $source read timed out, property=$propertyName")
                return@runCatching ""
            }
            val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
            val exitCode = process.exitValue()
            if (exitCode == 0) {
                logPropertyResult(source = source, name = propertyName, value = output)
                output
            } else {
                logWarn(
                    "Device SN $source read failed, property=$propertyName " +
                        "exit=$exitCode output=${output.take(120)}"
                )
                ""
            }
        }.onFailure {
            logWarn(it, "Device SN $source read crashed, property=$propertyName")
        }.getOrNull().orEmpty()
    }

    private fun logPropertyResult(source: String, name: String, value: String?) {
        val trimmed = value.orEmpty().trim()
        logInfo(
            "Device SN $source property=$name len=${trimmed.length} " +
                "value=${maskForLog(trimmed)}"
        )
    }

    private fun logInfo(message: String) {
        Log.i(TAG, message)
        Timber.i(message)
    }

    private fun logWarn(message: String) {
        Log.w(TAG, message)
        Timber.w(message)
    }

    private fun logWarn(throwable: Throwable, message: String) {
        Log.w(TAG, message, throwable)
        Timber.w(throwable, message)
    }
}
