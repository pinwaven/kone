@file:Suppress("UNCHECKED_CAST")

package poct.device.app.entity.service

import poct.device.app.App
import poct.device.app.bean.ConfigBean
import poct.device.app.bean.ConfigInfoBean
import poct.device.app.entity.SysConfig
import java.time.LocalDateTime
import java.util.Collections
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.declaredMemberProperties

object SysConfigService {
    /**
     * 上报版本
     */
    suspend fun reportVersion(version: String): ConfigInfoBean {
        val bean = findBean(ConfigInfoBean.PREFIX, ConfigInfoBean::class)
        bean.hardware = version
        bean.software = "v1.0.37.7"
        if (bean.name.isBlank()) {
            bean.name = "微流控"
        }
        if (bean.code.isBlank()) {
            bean.code = "KINO-A1-0000000"
        }
        if (bean.type.isBlank()) {
            bean.type = "KINO-A1"
        }
        saveBean(ConfigInfoBean.PREFIX, bean)
//        CommService.instance().saveDeviceVersion("V0.0.0", "V0.0.0", bean.hardware, bean.software)
        return bean
    }

    /**
     * 查找配置记录
     */
    suspend fun <T : ConfigBean> findBean(prefix: String, klass: KClass<T>): T {
        val configMap = findByPrefix(prefix)
        val bean = klass.createInstance()
        val pro = klass.declaredMemberProperties
        pro.forEach {
            val kmp = it as KMutableProperty1<T, String>
            kmp.set(bean, configMap[kmp.name]?.value ?: "")
        }
        return bean
    }

    /**
     * 保存配置记录
     */
    suspend fun <T : ConfigBean> saveBean(prefix: String, bean: T) {
        val klass = bean.javaClass.kotlin
        val pro = klass.declaredMemberProperties
        pro.forEach {
            val kmp = it as KMutableProperty1<T, String>
            val value = kmp.get(bean)
            saveEntity("$prefix${kmp.name}", value)
        }
    }

    /**
     * 保存每个配置条目
     */
    private suspend fun saveEntity(name: String, value: String) {
        var entity = App.getDatabase().sysConfigDao().findByName(name)
        val now = LocalDateTime.now()
        if (entity == null) {
            entity = SysConfig(
                name = name,
                value = value,
                gmtCreated = now,
                gmtModified = now,
            )
            App.getDatabase().sysConfigDao().add(entity)
        } else {
            entity.value = value
            entity.gmtModified = now
            App.getDatabase().sysConfigDao().update(entity)
        }
    }

    private suspend fun findByPrefix(prefix: String): Map<String, SysConfig> {
        val list = App.getDatabase().sysConfigDao().findByPrefix("$prefix%")
            ?: Collections.emptyList()
        val map = HashMap<String, SysConfig>()
        list.forEach { it -> map[it.name.replaceFirst(prefix, "")] = it }
        return map
    }
}