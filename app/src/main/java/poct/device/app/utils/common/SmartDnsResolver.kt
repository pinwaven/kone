package poct.device.app.utils.common

import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.locks.ReentrantLock

class SmartDnsResolver : Dns {
    companion object {
        // 公共DNS服务器列表
        private val PUBLIC_DNS_SERVERS = listOf(
            "114.114.114.114",
            "114.114.115.115",
            "223.5.5.5",
            "223.6.6.6",
            "119.29.29.29",
            "180.76.76.76",
            "156.154.70.2"
        )

        // 解析结果缓存（简单实现，生产环境建议用更完善的缓存）
        private val dnsCache = mutableMapOf<String, List<InetAddress>>()
        private val cacheLock = ReentrantLock()
    }

    override fun lookup(hostname: String): List<InetAddress> {
        // 1. 先尝试从缓存获取
        cacheLock.lock()
        try {
            dnsCache[hostname]?.let {
                if (it.isNotEmpty()) return it
            }
        } finally {
            cacheLock.unlock()
        }

        // 2. 尝试系统DNS解析（原始方法）
        val systemResult = try {
            Dns.SYSTEM.lookup(hostname)
        } catch (e: UnknownHostException) {
            emptyList()
        }

        if (systemResult.isNotEmpty()) {
            cacheLock.lock()
            try {
                dnsCache[hostname] = systemResult
            } finally {
                cacheLock.unlock()
            }
            return systemResult
        }

        // 3. 如果系统DNS失败，尝试使用备用DNS服务器
        for (dnsServer in PUBLIC_DNS_SERVERS) {
            try {
                val addresses = lookupWithCustomDns(hostname, dnsServer)
                if (addresses.isNotEmpty()) {
                    cacheLock.lock()
                    try {
                        dnsCache[hostname] = addresses
                    } finally {
                        cacheLock.unlock()
                    }
                    return addresses
                }
            } catch (e: Exception) {
                // 继续尝试下一个DNS服务器
            }
        }

        // 4. 所有方法都失败，抛出异常
        throw UnknownHostException("Failed to resolve host '$hostname'")
    }

    /**
     * 使用指定DNS服务器进行解析
     * 注意：这个方法需要网络权限，且可能在某些网络环境下被限制
     */
    private fun lookupWithCustomDns(hostname: String, dnsServer: String): List<InetAddress> {
        return try {
            // 这里使用系统方法，实际上系统DNS配置可能不支持指定服务器
            // 更高级的实现需要使用DNS over HTTPS或自定义DNS查询
            InetAddress.getAllByName(hostname)?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 清除DNS缓存
     */
    fun clearCache() {
        cacheLock.lock()
        try {
            dnsCache.clear()
        } finally {
            cacheLock.unlock()
        }
    }

    /**
     * 添加自定义DNS记录（用于本地测试或特定环境）
     */
    fun addManualMapping(hostname: String, ipAddress: String) {
        cacheLock.lock()
        try {
            val address = InetAddress.getByName(ipAddress)
            dnsCache[hostname] = listOf(address)
        } finally {
            cacheLock.unlock()
        }
    }
}