package poct.device.app

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import androidx.core.app.ActivityCompat
import poct.device.app.bean.WlanBean
import timber.log.Timber
import java.lang.reflect.Method

object AppWifiManager {
    /**
     * 开始扫描wifi
     */
    fun startScanWifi(manager: WifiManager?) {
        manager?.startScan()
    }

    /**
     * 获取wifi列表
     */
    @SuppressLint("MissingPermission")
    fun getWifiList(wifiManager: WifiManager): List<WlanBean> {
        return wifiManager.scanResults.filter { item -> item.SSID != null }.map {
            Timber.w("wifi${App.gson.toJson(it)}")
            WlanBean(
                ssid = it.SSID,
                bssid = it.BSSID ?: "",
                capabilities = it.capabilities,
                signalStrength = WifiManager.calculateSignalLevel(it.level, 5),
            )
        }.filter { it.ssid.isNotEmpty() }
    }

    /**
     * 保存网络
     */
    @SuppressLint("PrivateApi")
    fun saveNetworkByConfig(manager: WifiManager?, config: WifiConfiguration?) {
        if (manager == null) {
            return
        }
        try {
            val save: Method? = manager.javaClass.getDeclaredMethod(
                "save",
                WifiConfiguration::class.java,
                Class.forName("android.net.wifi.WifiManager\$ActionListener")
            )
            if (save != null) {
                save.setAccessible(true)
                save.invoke(manager, config, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 忘记网络
     */
    @SuppressLint("PrivateApi")
    fun forgetNetwork(manager: WifiManager?, networkId: Int) {
        if (manager == null) {
            return
        }
        try {
            val forget: Method? = manager.javaClass.getDeclaredMethod(
                "forget",
                Int::class.javaPrimitiveType,
                Class.forName("android.net.wifi.WifiManager\$ActionListener")
            )
            if (forget != null) {
                forget.setAccessible(true)
                forget.invoke(manager, networkId, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 断开连接
     */
    fun disconnectNetwork(wifiManager: WifiManager, wlanBean: WlanBean) {
        App.getWifiManager().disableNetwork(App.getWifiManager().connectionInfo.networkId)
        wifiManager.disconnect()
    }

    /**
     * 获取当前wifi(仅包含标识)
     * @return
     */
    fun getCurWifi(manager: WifiManager): WlanBean {
        val wifiInfo = manager.connectionInfo
        Timber.d("wifi: ${wifiInfo.ssid}")
        return WlanBean(
            ssid = regulateSSID(wifiInfo.ssid),
            connected = !wifiInfo.bssid.isNullOrEmpty()
        )
    }

    /**
     * 规范SSID名称，去掉两边引号
     */
    private fun regulateSSID(ssid: String): String {
        return if (ssid.startsWith("")) {
            ssid.substring(1, ssid.length - 1)
        } else {
            ssid
        }
    }

    /**
     * 是否开启wifi，没有的话打开wifi
     */
    fun openWifi(wifiManager: WifiManager): Boolean {
        var bRet = true
        if (!wifiManager.isWifiEnabled) {
            bRet = wifiManager.setWifiEnabled(true)
        }
        return bRet
    }

    fun connectWifi(wifiManager: WifiManager, wifiName: String, password: String, type: String?) {
        // 1、注意热点和密码均包含引号，此处需要需要转义引号
        val ssid = "\"" + wifiName + "\""
        val psd = "\"" + password + "\""

        //2、配置wifi信息
        val conf = WifiConfiguration()
        conf.SSID = ssid
        when (type) {
            "WEP" -> {
                // 加密类型为WEP
                conf.wepKeys[0] = psd
                conf.wepTxKeyIndex = 0
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40)
            }

            "WPA" ->                 // 加密类型为WPA
                conf.preSharedKey = psd

            else ->                 //无密码
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
        }
        //3、链接wifi
        wifiManager.addNetwork(conf)
        if (ActivityCompat.checkSelfPermission(
                App.getContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val list = wifiManager.configuredNetworks
        for (i in list) {
            if (i.SSID != null && i.SSID == ssid) {
                wifiManager.disconnect()
                wifiManager.enableNetwork(i.networkId, true)
                wifiManager.reconnect()
                break
            }
        }
    }
}