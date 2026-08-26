package poct.device.app.entity.service

import poct.device.app.bean.ConfigLaserBean

object LaserConfigService {
    suspend fun savePower(power: String) {
        val normalized = power.trim().toIntOrNull()?.coerceIn(LASER_POWER_MIN, LASER_POWER_MAX)
            ?: return
        SysConfigService.saveBean(ConfigLaserBean.PREFIX, ConfigLaserBean(power = normalized.toString()))
    }

    /** 已设置过则返回强度值，未设置过返回 null，由调用方决定各自场景的默认值 */
    suspend fun findStoredPower(): Int? {
        val bean = SysConfigService.findBean(ConfigLaserBean.PREFIX, ConfigLaserBean::class)
        return bean.power.trim().toIntOrNull()?.coerceIn(LASER_POWER_MIN, LASER_POWER_MAX)
    }

    private const val LASER_POWER_MIN = -100
    private const val LASER_POWER_MAX = 0
}
