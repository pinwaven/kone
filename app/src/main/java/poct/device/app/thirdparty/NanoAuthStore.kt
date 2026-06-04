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
        )
        save(state)
        return state
    }

    suspend fun save(state: NanoAuthState) {
        SysConfigService.saveBean(ConfigNanoAuthBean.PREFIX, state.toBean())
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
        )
}
