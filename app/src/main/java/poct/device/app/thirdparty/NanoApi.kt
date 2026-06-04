package poct.device.app.thirdparty

import com.google.gson.JsonParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.BuildConfig
import poct.device.app.R
import poct.device.app.bean.ConfigInfoBean
import poct.device.app.bean.ConfigInfoV2Bean
import poct.device.app.bean.ConfigSysBean
import poct.device.app.entity.service.SysConfigService
import poct.device.app.thirdparty.model.nano.NanoActivateReq
import poct.device.app.thirdparty.model.nano.NanoActivateResp
import poct.device.app.thirdparty.model.nano.NanoAuthState
import poct.device.app.thirdparty.model.nano.NanoAuthSupport
import poct.device.app.thirdparty.model.nano.NanoBiomarkersReq
import poct.device.app.thirdparty.model.nano.NanoBiomarkersResp
import poct.device.app.thirdparty.model.nano.NanoChipResp
import poct.device.app.thirdparty.model.nano.NanoEndpoints
import poct.device.app.thirdparty.model.nano.NanoKinoResultReq
import poct.device.app.thirdparty.model.nano.NanoKinoResultResp
import poct.device.app.thirdparty.model.nano.NanoMachine
import poct.device.app.thirdparty.model.nano.NanoMachineInfoResp
import poct.device.app.thirdparty.model.nano.NanoMachineInfoSupport
import poct.device.app.thirdparty.model.nano.NanoProtectedCallResult
import poct.device.app.thirdparty.model.nano.NanoProtectedCallExecutor
import poct.device.app.thirdparty.model.nano.NanoProtectedRequest
import poct.device.app.thirdparty.model.nano.NanoRawResponse
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin client for the Waven Nano AI backend (Aliyun FC 3.0).
 * Mirrors the three calls the WeChat miniapp Kino Simulator makes:
 *   GET  /kino/kino-chip?chip_id=...
 *   POST /kino/biomarkers
 *   POST /kino/kino-result
 *
 * Active when ConfigSysBean.flow == "nano". Reads baseUrl + deviceId from
 * ConfigSysBean each call so changes in Settings take effect without restart.
 */
object NanoApi {
    const val FLOW_CLINICAL = "clinical"
    const val FLOW_NANO     = "nano"

    // Holds a pre-fetched upgrade result so AfterSaleVersionUpgradeViewModel can
    // consume it directly without re-querying (used when upgrade is triggered from SysFunApiTest).
    var pendingUpgrade: poct.device.app.thirdparty.model.nano.NanoUpgradeResp? = null

    private val authClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
    private val JSON = "application/json".toMediaType()

    private suspend fun config(): ConfigSysBean =
        SysConfigService.findBean(ConfigSysBean.PREFIX, ConfigSysBean::class)

    private fun baseUrl(): String = AppParams.runtimeModeState.nanoBaseUrl().trimEnd('/')

    private fun activationToken(): String = AppParams.kinoActivationToken().trim()

    private fun text(resId: Int, vararg args: Any): String =
        App.getContext().getString(resId, *args)

    private fun errorText(error: String?): String {
        val base = text(NanoAuthSupport.messageResForError(error))
        return if (NanoAuthSupport.messageResForError(error) == R.string.nano_auth_error_backend && !error.isNullOrBlank()) {
            "$base: $error"
        } else {
            base
        }
    }

    suspend fun deviceSerial(): String = config().nanoDeviceId

    private fun Request.Builder.withAuth(token: String): Request.Builder =
        if (token.isNotEmpty()) this.header("Authorization", "Bearer $token") else this

    private fun protectedExecutor(base: String): NanoProtectedCallExecutor =
        NanoProtectedCallExecutor(
            authStore = object : NanoProtectedCallExecutor.AuthStore {
                override suspend fun load(): NanoAuthState = NanoAuthStore.load()

                override suspend fun save(state: NanoAuthState) {
                    NanoAuthStore.save(state)
                }
            },
            transport = object : NanoProtectedCallExecutor.Transport {
                override suspend fun executeProtected(
                    request: NanoProtectedRequest,
                    commToken: String,
                ): NanoRawResponse = executeNanoRequest(request, commToken)

                override suspend fun exchangeToken(rootToken: String): NanoRawResponse {
                    val request = NanoProtectedRequest.post(NanoEndpoints.tokenExchange(base), "{}")
                    return executeNanoRequest(request, rootToken)
                }
            },
            gson = App.gson,
        )

    private fun executeNanoRequest(request: NanoProtectedRequest, token: String): NanoRawResponse {
        val builder = Request.Builder()
            .url(request.url)
            .withAuth(token)
        val okHttpRequest = when (request.method) {
            "POST" -> builder.post(request.body.orEmpty().toRequestBody(JSON)).build()
            else -> builder.get().build()
        }
        authClient.newCall(okHttpRequest).execute().use { response ->
            return NanoRawResponse(
                status = response.code,
                body = response.body?.string().orEmpty(),
            )
        }
    }

    private suspend fun executeProtected(
        endpointName: String,
        request: NanoProtectedRequest,
        base: String,
    ): NanoProtectedCallResult =
        protectedExecutor(base).execute(endpointName, request).let { result ->
            if (!result.ok) {
                Timber.w(
                    "NanoApi.%s failed: status=%s error=%s message=%s",
                    endpointName,
                    result.status,
                    result.error,
                    result.message,
                )
            }
            result
        }

    data class ActivationResult(
        val ok: Boolean,
        val message: String,
        val status: Int? = null,
        val error: String? = null,
        val authState: NanoAuthState? = null,
    )

    data class ProbeResult(
        val ok: Boolean,
        val url: String,
        val status: Int? = null,
        val latencyMs: Long = 0,
        val body: String? = null,
        val error: String? = null,
    )

    data class MachineInfoResult(
        val ok: Boolean,
        val skipped: Boolean = false,
        val message: String = "",
        val status: Int? = null,
        val error: String? = null,
        val machine: NanoMachine? = null,
    )

    suspend fun activateDevice(
        mainboardId: String,
        firmwareId: String,
        firmwareVersion: String = "",
        model: String = "KNA1",
    ): ActivationResult = withContext(Dispatchers.IO) {
        val normalizedMainboardId = mainboardId.trim()
        val normalizedFirmwareId = firmwareId.trim()
        val normalizedModel = model.trim().ifEmpty { "KNA1" }
        val base = baseUrl()
        val token = activationToken()

        if (base.isEmpty()) {
            return@withContext ActivationResult(ok = false, message = text(R.string.nano_auth_base_url_missing))
        }
        if (token.isEmpty()) {
            return@withContext ActivationResult(
                ok = false,
                message = text(R.string.nano_auth_activation_token_missing)
            )
        }
        if (normalizedMainboardId.isEmpty()) {
            return@withContext ActivationResult(ok = false, message = text(R.string.nano_auth_mainboard_id_missing))
        }
        if (normalizedFirmwareId.isEmpty()) {
            return@withContext ActivationResult(ok = false, message = text(R.string.nano_auth_firmware_id_missing))
        }

        val url = "$base/kino/activate"
        try {
            val jsonBody = App.gson.toJson(
                NanoActivateReq(
                    mainboardId = normalizedMainboardId,
                    firmwareId = normalizedFirmwareId,
                    model = normalizedModel,
                )
            )
            val request = Request.Builder()
                .url(url)
                .withAuth(token)
                .post(jsonBody.toRequestBody(JSON))
                .build()
            authClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val parsed = runCatching {
                    App.gson.fromJson(body, NanoActivateResp::class.java)
                }.getOrNull()
                if (!response.isSuccessful) {
                    val error = parsed?.error
                    return@withContext ActivationResult(
                        ok = false,
                        status = response.code,
                        error = error,
                        message = text(R.string.nano_auth_activate_http_failed, response.code, errorText(error))
                    )
                }

                if (parsed == null) {
                    return@withContext ActivationResult(
                        ok = false,
                        status = response.code,
                        message = text(R.string.nano_auth_activate_parse_failed)
                    )
                }
                if (!parsed.success) {
                    return@withContext ActivationResult(
                        ok = false,
                        status = response.code,
                        error = parsed.error,
                        message = text(R.string.nano_auth_activate_failed, errorText(parsed.error))
                    )
                }

                val rootToken = parsed.rootToken.orEmpty()
                val commToken = parsed.commToken.orEmpty()
                val expiresAt = parsed.commTokenExpiresAt.orEmpty()
                val machine = parsed.machine
                if (rootToken.isEmpty() || commToken.isEmpty() || expiresAt.isEmpty() || machine?.machineNo.isNullOrEmpty()) {
                    return@withContext ActivationResult(
                        ok = false,
                        status = response.code,
                        message = text(R.string.nano_auth_activate_missing_token_or_machine)
                    )
                }

                val state = NanoAuthStore.saveActivation(
                    rootToken = rootToken,
                    commToken = commToken,
                    commTokenExpiresAt = expiresAt,
                    machine = machine!!,
                )
                uploadLocalMachineInfo(firmwareVersion = firmwareVersion)
                ActivationResult(
                    ok = true,
                    status = response.code,
                    authState = state,
                    message = text(R.string.nano_auth_activate_success, state.machineNo)
                )
            }
        } catch (e: IOException) {
            Timber.w(e, "NanoApi.activateDevice network failed")
            ActivationResult(
                ok = false,
                message = text(R.string.nano_auth_activate_network_failed, e.message ?: e.javaClass.simpleName)
            )
        } catch (e: Exception) {
            Timber.w(e, "NanoApi.activateDevice failed")
            ActivationResult(
                ok = false,
                message = text(R.string.nano_auth_activate_failed, e.message ?: e.javaClass.simpleName)
            )
        }
    }

    suspend fun uploadLocalMachineInfo(
        firmwareVersion: String = "",
    ): MachineInfoResult {
        val configBean = SysConfigService.findBean(ConfigInfoBean.PREFIX, ConfigInfoV2Bean::class)
        return uploadMachineInfo(
            softwareVersion = BuildConfig.VERSION_NAME,
            firmwareVersion = firmwareVersion.ifBlank { configBean.hardware },
        )
    }

    suspend fun uploadMachineInfo(
        softwareVersion: String?,
        firmwareVersion: String?,
    ): MachineInfoResult = withContext(Dispatchers.IO) {
        val base = baseUrl()
        if (base.isEmpty()) {
            return@withContext MachineInfoResult(ok = false, message = "nanoBaseUrl not configured")
        }
        val req = NanoMachineInfoSupport.buildRequest(softwareVersion, firmwareVersion)
            ?: return@withContext MachineInfoResult(
                ok = true,
                skipped = true,
                message = "machine info upload skipped: version values are blank or invalid",
            )

        val url = NanoEndpoints.machineInfo(base)
        try {
            val result = executeProtected(
                endpointName = "kino-machines-info",
                request = NanoProtectedRequest.post(url, App.gson.toJson(req)),
                base = base,
            )
            if (!result.ok) {
                return@withContext MachineInfoResult(
                    ok = false,
                    status = result.status,
                    error = result.error,
                    message = result.message.orEmpty(),
                )
            }
            val parsed = App.gson.fromJson(result.body.orEmpty(), NanoMachineInfoResp::class.java)
            if (!parsed.success) {
                return@withContext MachineInfoResult(
                    ok = false,
                    status = result.status,
                    error = parsed.error,
                    message = "kino-machines-info failed: ${errorText(parsed.error)}",
                )
            }
            val machine = parsed.machine
            if (machine != null) {
                updateCachedMachineInfo(machine, req.softwareVersion, req.firmwareVersion)
            }
            MachineInfoResult(
                ok = true,
                status = result.status,
                message = "machine info uploaded",
                machine = machine,
            )
        } catch (e: Exception) {
            Timber.w(e, "NanoApi.uploadMachineInfo failed")
            MachineInfoResult(
                ok = false,
                message = e.message ?: e.javaClass.simpleName,
            )
        }
    }

    private suspend fun updateCachedMachineInfo(
        machine: NanoMachine,
        requestedSoftwareVersion: String?,
        requestedFirmwareVersion: String?,
    ) {
        val currentAuth = NanoAuthStore.load()
        NanoAuthStore.save(
            currentAuth.copy(
                machineNo = machine.machineNo ?: currentAuth.machineNo,
                machineName = machine.machineName ?: currentAuth.machineName,
                model = machine.model ?: currentAuth.model,
                status = machine.status ?: currentAuth.status,
            )
        )

        val configBean = SysConfigService.findBean(ConfigInfoBean.PREFIX, ConfigInfoV2Bean::class)
        val updated = configBean.copy(
            name = machine.machineName ?: configBean.name,
            code = machine.machineNo ?: configBean.code,
            type = machine.model ?: configBean.type,
            software = machine.softwareVersion ?: requestedSoftwareVersion ?: configBean.software,
            hardware = machine.firmwareVersion ?: requestedFirmwareVersion ?: configBean.hardware,
        )
        SysConfigService.saveBean(ConfigInfoBean.PREFIX, updated)
    }

    /**
     * Hit `${nanoBaseUrl}/kino/kino-chip?chip_id=__ping__` and return a structured
     * result. The endpoint exists in the nano worker and responds with
     * `{"found":false}` (HTTP 200) for unknown chips — that proves both reachability
     * and that the request was parsed by the worker, not just that DNS resolved.
     */
    suspend fun probe(): ProbeResult = withContext(Dispatchers.IO) {
        val base = baseUrl()
        if (base.isEmpty()) {
            return@withContext ProbeResult(ok = false, url = "", error = "nanoBaseUrl not configured")
        }
        val url = NanoEndpoints.probe(base)
        val start = System.currentTimeMillis()
        try {
            val result = executeProtected("kino-chip", NanoProtectedRequest.get(url), base)
            ProbeResult(
                ok = result.ok,
                url = url,
                status = result.status,
                latencyMs = System.currentTimeMillis() - start,
                body = result.body?.take(500),
                error = result.error ?: if (!result.ok) result.message else null,
            )
        } catch (e: Exception) {
            ProbeResult(
                ok = false,
                url = url,
                latencyMs = System.currentTimeMillis() - start,
                error = e.message ?: e.javaClass.simpleName,
            )
        }
    }

    suspend fun getChip(chipId: String): NanoChipResp? = withContext(Dispatchers.IO) {
        val base = baseUrl()
        if (base.isEmpty()) {
            Timber.w("NanoApi.getChip: nanoBaseUrl not configured")
            return@withContext null
        }
        val url = NanoEndpoints.kinoChip(base, chipId)
        try {
            val result = executeProtected("kino-chip", NanoProtectedRequest.get(url), base)
            val body = result.body ?: return@withContext null
            App.gson.fromJson(body, NanoChipResp::class.java)
        } catch (e: IOException) {
            Timber.w(e, "NanoApi.getChip failed")
            null
        } catch (e: JsonParseException) {
            Timber.w(e, "NanoApi.getChip parse failed")
            null
        }
    }

    suspend fun postBiomarkers(req: NanoBiomarkersReq): NanoBiomarkersResp? = withContext(Dispatchers.IO) {
        val base = baseUrl()
        if (base.isEmpty()) {
            Timber.w("NanoApi.postBiomarkers: nanoBaseUrl not configured")
            return@withContext null
        }
        val url = NanoEndpoints.biomarkers(base)
        try {
            val jsonBody = App.gson.toJson(req)
            Timber.w("NanoApi.postBiomarkers request prepared")
            val result = executeProtected("biomarkers", NanoProtectedRequest.post(url, jsonBody), base)
            val body = result.body ?: return@withContext null
            Timber.w("NanoApi.postBiomarkers resp=$body")
            App.gson.fromJson(body, NanoBiomarkersResp::class.java)
        } catch (e: Exception) {
            Timber.w(e, "NanoApi.postBiomarkers error: ${e::class.simpleName}")
            null
        }
    }

    suspend fun postKinoResult(req: NanoKinoResultReq): NanoKinoResultResp? = withContext(Dispatchers.IO) {
        val base = baseUrl()
        if (base.isEmpty()) {
            Timber.w("NanoApi.postKinoResult: nanoBaseUrl not configured")
            return@withContext null
        }
        val url = NanoEndpoints.kinoResult(base)
        try {
            val jsonBody = App.gson.toJson(req)
            val result = executeProtected("kino-result", NanoProtectedRequest.post(url, jsonBody), base)
            val body = result.body ?: return@withContext null
            App.gson.fromJson(body, NanoKinoResultResp::class.java)
        } catch (e: IOException) {
            Timber.w(e, "NanoApi.postKinoResult failed")
            null
        } catch (e: JsonParseException) {
            Timber.w(e, "NanoApi.postKinoResult parse failed")
            null
        }
    }

    /**
     * Queries `${nanoBaseUrl}/kino/kino-upgrade` for the latest APK version and OSS URL.
     * Returns null if the request fails or is unauthorized.
     */
    suspend fun checkUpgrade(): poct.device.app.thirdparty.model.nano.NanoUpgradeResp? = withContext(Dispatchers.IO) {
        val base = baseUrl()
        if (base.isEmpty()) {
            Timber.w("NanoApi.checkUpgrade: nanoBaseUrl not configured")
            return@withContext null
        }
        val url = NanoEndpoints.kinoUpgrade(base)
        try {
            val result = executeProtected("kino-upgrade", NanoProtectedRequest.get(url), base)
            val body = result.body ?: return@withContext null
            App.gson.fromJson(body, poct.device.app.thirdparty.model.nano.NanoUpgradeResp::class.java)
        } catch (e: Exception) {
            Timber.w(e, "NanoApi.checkUpgrade failed")
            null
        }
    }
}
