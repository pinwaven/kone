package poct.device.app.thirdparty

import poct.device.app.bean.ConfigNanoAuthBean
import poct.device.app.entity.service.SysConfigService
import poct.device.app.thirdparty.model.nano.NanoAuthState
import poct.device.app.thirdparty.model.nano.NanoMachine
import java.time.Instant

object NanoAuthStore {
    suspend fun load(): NanoAuthState {
        val bean = SysConfigService.findBean(ConfigNanoAuthBean.PREFIX, ConfigNanoAuthBean::class)
        return bean.toState()
    }

    suspend fun saveActivation(
        rootToken: String,
        commToken: String,
        commTokenExpiresAt: String,
        machine: NanoMachine,
        firmwareId: String = "",
    ): NanoAuthState {
        val state = NanoAuthState(
            rootToken = rootToken,
            commToken = commToken,
            commTokenExpiresAt = commTokenExpiresAt,
            machineNo = machine.machineNo.orEmpty(),
            machineName = machine.machineName.orEmpty(),
            model = machine.model.orEmpty(),
            status = machine.status.orEmpty(),
            activatedAt = Instant.now().toString(),
            firmwareId = firmwareId,
        )
        save(state)
        return state
    }

    suspend fun save(state: NanoAuthState) {
        SysConfigService.saveBean(ConfigNanoAuthBean.PREFIX, state.toBean())
    }

    /**
     * Caches the MCU firmware id read from serial handshake so an auto reactivation
     * (see [NanoProtectedCallExecutor]) can call `/activate` without a human present.
     * Called on app boot, on manual activation, and whenever the device-info screen
     * re-reads the handshake.
     */
    suspend fun updateFirmwareId(firmwareId: String) {
        val trimmed = firmwareId.trim()
        if (trimmed.isEmpty()) return
        val current = load()
        if (current.firmwareId == trimmed) return
        save(current.copy(firmwareId = trimmed))
    }

    suspend fun invalidate(reason: String): NanoAuthState {
        val state = load().invalidated(reason)
        save(state)
        return state
    }

    suspend fun clear() {
        SysConfigService.saveBean(ConfigNanoAuthBean.PREFIX, ConfigNanoAuthBean.Empty)
    }

    private fun ConfigNanoAuthBean.toState(): NanoAuthState =
        NanoAuthState(
            rootToken = rootToken,
            commToken = commToken,
            commTokenExpiresAt = commTokenExpiresAt,
            machineNo = machineNo,
            machineName = machineName,
            model = model,
            status = status,
            activatedAt = activatedAt,
            refreshedAt = refreshedAt,
            firmwareId = firmwareId,
        )

    private fun NanoAuthState.toBean(): ConfigNanoAuthBean =
        ConfigNanoAuthBean(
            rootToken = rootToken,
            commToken = commToken,
            commTokenExpiresAt = commTokenExpiresAt,
            machineNo = machineNo,
            machineName = machineName,
            model = model,
            status = status,
            activatedAt = activatedAt,
            refreshedAt = refreshedAt,
            firmwareId = firmwareId,
        )
}
