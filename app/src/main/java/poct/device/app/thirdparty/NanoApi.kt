package poct.device.app.thirdparty

import com.google.gson.JsonParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.BuildConfig
import poct.device.app.R
import poct.device.app.bean.ConfigInfoBean
import poct.device.app.bean.ConfigInfoV2Bean
import poct.device.app.entity.service.SysConfigService
import poct.device.app.thirdparty.model.nano.NanoActivateReq
import poct.device.app.thirdparty.model.nano.NanoActivateResp
import poct.device.app.thirdparty.model.nano.NanoAuthState
import poct.device.app.thirdparty.model.nano.NanoAuthSupport
import poct.device.app.thirdparty.model.nano.NanoBiomarkersReq
import poct.device.app.thirdparty.model.nano.NanoBiomarkersResp
import poct.device.app.thirdparty.model.nano.NanoChipResp
import poct.device.app.thirdparty.model.nano.NanoDeviceInfoSupport
import poct.device.app.thirdparty.model.nano.NanoDeviceConfigReq
import poct.device.app.thirdparty.model.nano.NanoDeviceMeResp
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
import poct.device.app.thirdparty.model.nano.NanoTokenExchangeResp
import poct.device.app.thirdparty.model.nano.NanoTokenRecoveryGate
import timber.log.Timber
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit
import com.google.gson.JsonObject

/**
 * Thin client for the Waven Nano AI backend (Aliyun FC 3.0).
 * Mirrors the three calls the WeChat miniapp Kino Simulator makes:
 *   GET  /kino/kino-chip?chip_id=...
 *   POST /kino/biomarkers
 *   POST /kino/kino-result
 *
 * Active when the system flow == "nano". Reads baseUrl from runtime config each
 * call so changes in Settings take effect without restart.
 */
object NanoApi {
    const val FLOW_CLINICAL = "clinical"
    const val FLOW_NANO     = "nano"

    // Holds a pre-fetched upgrade result so AfterSaleVersionUpgradeViewModel can
    // consume it directly without re-querying (used when upgrade is triggered from SysFunInfo).
    var pendingUpgrade: poct.device.app.thirdparty.model.nano.NanoUpgradeResp? = null

    internal fun buildClient(
        connectTimeoutSec: Long = 5,
        readTimeoutSec: Long = 15,
        writeTimeoutSec: Long = 10,
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(connectTimeoutSec, TimeUnit.SECONDS)
        .readTimeout(readTimeoutSec, TimeUnit.SECONDS)
        .writeTimeout(writeTimeoutSec, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val authClient: OkHttpClient by lazy { buildClient() }
    private val JSON = "application/json".toMediaType()

    // 曲线文件上传体积较大，用更长的读写超时
    private val uploadCurveClient: OkHttpClient by lazy {
        buildClient(readTimeoutSec = 30, writeTimeoutSec = 60)
    }

    private fun baseUrl(): String = AppParams.runtimeModeState.nanoBaseUrl().trimEnd('/')

    private fun activationToken(): String = AppParams.kinoActivationToken().trim()

    // 网络层重试：解决"慢/偶发掉包"本身，减少真正需要人工介入的概率。
    // 只重试连接异常和 5xx（服务端可能重启/过载），4xx 是业务错误，重试没意义。
    private const val NETWORK_MAX_ATTEMPTS = 3
    private const val NETWORK_INITIAL_BACKOFF_MS = 500L

    private suspend fun executeWithBackoff(
        maxAttempts: Int = NETWORK_MAX_ATTEMPTS,
        initialBackoffMs: Long = NETWORK_INITIAL_BACKOFF_MS,
        call: () -> NanoRawResponse,
    ): NanoRawResponse {
        var backoffMs = initialBackoffMs
        var attempt = 1
        while (true) {
            val response = try {
                call()
            } catch (e: IOException) {
                if (attempt >= maxAttempts) throw e
                Timber.w(e, "NanoApi network attempt %d/%d failed, retrying in %dms", attempt, maxAttempts, backoffMs)
                delay(backoffMs)
                backoffMs *= 2
                attempt++
                continue
            }
            val isServerError = response.status in 500..599
            if (isServerError && attempt < maxAttempts) {
                Timber.w("NanoApi network attempt %d/%d got HTTP %d, retrying in %dms", attempt, maxAttempts, response.status, backoffMs)
                delay(backoffMs)
                backoffMs *= 2
                attempt++
                continue
            }
            return response
        }
    }

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

    private fun Request.Builder.withAuth(token: String): Request.Builder =
        NanoAuthSupport.bearerHeader(token)?.let { header("Authorization", it) } ?: this

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
            reactivator = object : NanoProtectedCallExecutor.Reactivator {
                override suspend fun reactivate(state: NanoAuthState): Boolean = autoReactivate()
            },
            gson = App.gson,
        )

    /**
     * Re-activates with the mainboard/firmware id already cached on this device
     * (see [NanoAuthStore.updateFirmwareId]) — no human input. `/activate` is
     * idempotent and keyed off hardware ids only, so calling it again here is safe.
     */
    private suspend fun autoReactivate(): Boolean {
        val mainboardId = App.getDeviceId().trim()
        val firmwareId = NanoAuthStore.load().firmwareId.trim()
        if (mainboardId.isEmpty() || firmwareId.isEmpty()) {
            Timber.w("NanoApi auto reactivate skipped: missing mainboardId or firmwareId")
            return false
        }
        val result = activateDevice(mainboardId = mainboardId, firmwareId = firmwareId)
        if (!result.ok) {
            Timber.w("NanoApi auto reactivate failed: %s", result.message)
        }
        return result.ok
    }

    private suspend fun executeNanoRequest(request: NanoProtectedRequest, token: String): NanoRawResponse =
        executeWithBackoff {
            val builder = Request.Builder()
                .url(request.url)
                .withAuth(token)
            val okHttpRequest = when (request.method) {
                "POST" -> builder.post(request.body.orEmpty().toRequestBody(JSON)).build()
                else -> builder.get().build()
            }
            authClient.newCall(okHttpRequest).execute().use { response ->
                NanoRawResponse(
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

    data class DeviceMeResult(
        val ok: Boolean,
        val message: String = "",
        val status: Int? = null,
        val error: String? = null,
        val machine: NanoMachine? = null,
        val config: ConfigInfoV2Bean? = null,
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
            // /activate 幂等、只按硬件ID查、不吃token，重试几次无副作用——这是让固件
            // 在 invalid_comm_token 时自动兜底自调用的安全前提。
            val response = executeWithBackoff {
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
                authClient.newCall(request).execute().use { resp ->
                    NanoRawResponse(status = resp.code, body = resp.body?.string().orEmpty())
                }
            }

            val parsed = runCatching {
                App.gson.fromJson(response.body, NanoActivateResp::class.java)
            }.getOrNull()

            if (response.status !in 200..299) {
                val error = parsed?.error
                return@withContext ActivationResult(
                    ok = false,
                    status = response.status,
                    error = error,
                    message = text(R.string.nano_auth_activate_http_failed, response.status, errorText(error))
                )
            }

            if (parsed == null) {
                return@withContext ActivationResult(
                    ok = false,
                    status = response.status,
                    message = text(R.string.nano_auth_activate_parse_failed)
                )
            }
            if (!parsed.success) {
                return@withContext ActivationResult(
                    ok = false,
                    status = response.status,
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
                    status = response.status,
                    message = text(R.string.nano_auth_activate_missing_token_or_machine)
                )
            }

            // 先落盘 token 再做其它副作用（uploadLocalMachineInfo），避免"服务端已转正、
            // 设备没存上"——即使后面这步失败，token 已经在本地，下次直接能用。
            val state = NanoAuthStore.saveActivation(
                rootToken = rootToken,
                commToken = commToken,
                commTokenExpiresAt = expiresAt,
                machine = machine!!,
                firmwareId = normalizedFirmwareId,
            )
            uploadLocalMachineInfo(firmwareVersion = firmwareVersion)
            ActivationResult(
                ok = true,
                status = response.status,
                authState = state,
                message = text(R.string.nano_auth_activate_success, state.machineNo)
            )
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

    suspend fun getDeviceMe(): DeviceMeResult = withContext(Dispatchers.IO) {
        val base = baseUrl()
        if (base.isEmpty()) {
            return@withContext DeviceMeResult(ok = false, message = "nanoBaseUrl not configured")
        }
        val url = NanoEndpoints.deviceMe(base)
        try {
            val result = executeProtected(
                endpointName = "device-me",
                request = NanoProtectedRequest.get(url),
                base = base,
            )
            if (!result.ok) {
                return@withContext DeviceMeResult(
                    ok = false,
                    status = result.status,
                    error = result.error,
                    message = authFacingMessage(result.error, result.message),
                )
            }

            val parsed = App.gson.fromJson(result.body.orEmpty(), NanoDeviceMeResp::class.java)
            if (!parsed.success) {
                return@withContext DeviceMeResult(
                    ok = false,
                    status = result.status,
                    error = parsed.error,
                    message = authFacingMessage(parsed.error, "device-me failed: ${errorText(parsed.error)}"),
                )
            }

            val config = NanoDeviceInfoSupport.toConfigInfo(parsed.machine)
                ?: return@withContext DeviceMeResult(
                    ok = false,
                    status = result.status,
                    error = "parse_error",
                    message = "device-me failed: response missing machine_no or model",
                )

            updateCachedMachineInfo(
                machine = parsed.machine!!,
                requestedSoftwareVersion = config.software,
                requestedFirmwareVersion = config.hardware,
            )
            DeviceMeResult(
                ok = true,
                status = result.status,
                machine = parsed.machine,
                config = config,
            )
        } catch (e: Exception) {
            Timber.w(e, "NanoApi.getDeviceMe failed")
            DeviceMeResult(ok = false, message = e.message ?: e.javaClass.simpleName)
        }
    }

    private fun authFacingMessage(error: String?, fallback: String?): String {
        return when (error) {
            "missing_comm_token" -> "设备未激活，请先进入工厂测试 -> 设备激活"
            "missing_root_token" -> "通信令牌已过期且缺少 root token，请重新执行设备激活"
            "invalid_root_token", "machine_not_active", "comm_token_expired" -> errorText(error)
            else -> fallback.orEmpty()
        }
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
            Timber.w("NanoApi.postBiomarkers resp=%s", NanoAuthSupport.redactSensitiveText(body))
            App.gson.fromJson(body, NanoBiomarkersResp::class.java)
        } catch (e: Exception) {
            Timber.w(e, "NanoApi.postBiomarkers error: ${e::class.simpleName}")
            null
        }
    }

    /**
     * Uploads this device's calibrated laser intensity so the server can apply it on top of
     * the chip model's config for future kino-chip responses (see nano's applyDeviceConfig,
     * which writes laser_intensity into chip_config.cut_off1).
     */
    suspend fun postDeviceConfig(laserIntensity: Int): Boolean = withContext(Dispatchers.IO) {
        val base = baseUrl()
        if (base.isEmpty()) {
            Timber.w("NanoApi.postDeviceConfig: nanoBaseUrl not configured")
            return@withContext false
        }
        val url = NanoEndpoints.deviceConfig(base)
        try {
            val jsonBody = App.gson.toJson(NanoDeviceConfigReq(deviceConfig = mapOf("laser_intensity" to laserIntensity)))
            val result = executeProtected("device-config", NanoProtectedRequest.post(url, jsonBody), base)
            result.ok
        } catch (e: Exception) {
            Timber.w(e, "NanoApi.postDeviceConfig error: ${e::class.simpleName}")
            false
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

    data class CurveUploadResult(
        val ok: Boolean,
        val id: Int? = null,
        val message: String = "",
        val status: Int? = null,
        val error: String? = null,
    )

    suspend fun uploadCurve(
        qrcode: String,
        referenceValues: String,
        curveFile: File,
    ): CurveUploadResult = withContext(Dispatchers.IO) {
        val base = baseUrl()
        if (base.isEmpty()) return@withContext CurveUploadResult(ok = false, message = "nanoBaseUrl not configured")
        if (!curveFile.exists()) return@withContext CurveUploadResult(ok = false, message = "curve file not found")

        val url = NanoEndpoints.kinoCurve(base)
        val authState = NanoAuthStore.load()
        var commToken = authState.commToken.trim()
        if (commToken.isEmpty()) return@withContext CurveUploadResult(ok = false, message = "missing_comm_token, please activate")

        fun buildMultipart(): okhttp3.RequestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("qrcode", qrcode)
            .addFormDataPart("reference_values", referenceValues)
            .addFormDataPart("curve_file", curveFile.name, curveFile.asRequestBody("application/octet-stream".toMediaType()))
            .build()

        suspend fun execute(token: String): NanoRawResponse = executeWithBackoff {
            val request = Request.Builder()
                .url(url)
                .withAuth(token)
                .post(buildMultipart())
                .build()
            uploadCurveClient.newCall(request).execute().use { resp ->
                val rawBody = resp.body?.string().orEmpty()
                if (resp.code !in 200..299) {
                    Timber.w(
                        "NanoApi.uploadCurve non-2xx status=%d body=%s",
                        resp.code,
                        NanoAuthSupport.redactSensitiveText(rawBody)
                    )
                }
                NanoRawResponse(status = resp.code, body = rawBody)
            }
        }

        fun NanoRawResponse.errorField(): String? = runCatching {
            App.gson.fromJson(body, JsonObject::class.java)?.get("error")?.takeIf { !it.isJsonNull }?.asString
        }.getOrNull()

        fun NanoRawResponse.successId(): Int? = runCatching {
            App.gson.fromJson(body, JsonObject::class.java)?.get("id")?.takeIf { !it.isJsonNull }?.asInt
        }.getOrNull()

        try {
            fun NanoRawResponse.toCurveUploadResult(): CurveUploadResult =
                if (status in 200..299) {
                    CurveUploadResult(ok = true, id = successId(), status = status)
                } else {
                    CurveUploadResult(ok = false, status = status, error = errorField(), message = "kino-curve failed: HTTP $status")
                }

            fun shouldInvalidateAuth(error: String?): Boolean =
                error == "invalid_root_token" || error == "machine_not_active"

            suspend fun exchangeWithRootTokenAndRetry(latestState: NanoAuthState): CurveUploadResult {
                val rootToken = latestState.rootToken.trim()
                if (rootToken.isEmpty()) {
                    return CurveUploadResult(
                        ok = false,
                        status = 401,
                        error = "missing_root_token",
                        message = "comm_token_expired and root token missing, please activate",
                    )
                }

                val exchangeResp = executeNanoRequest(
                    NanoProtectedRequest.post(NanoEndpoints.tokenExchange(base), "{}"),
                    rootToken,
                )
                if (exchangeResp.status !in 200..299) {
                    val exchangeError = exchangeResp.errorField()
                    if (shouldInvalidateAuth(exchangeError)) {
                        NanoAuthStore.save(latestState.invalidated(exchangeError.orEmpty()))
                    }
                    return CurveUploadResult(
                        ok = false,
                        status = exchangeResp.status,
                        error = exchangeError,
                        message = "token exchange failed",
                    )
                }

                val exchanged = runCatching {
                    App.gson.fromJson(exchangeResp.body, NanoTokenExchangeResp::class.java)
                }.getOrNull() ?: return CurveUploadResult(ok = false, message = "token exchange parse failed")

                if (!exchanged.success) {
                    if (shouldInvalidateAuth(exchanged.error)) {
                        NanoAuthStore.save(latestState.invalidated(exchanged.error.orEmpty()))
                    }
                    return CurveUploadResult(
                        ok = false,
                        status = exchangeResp.status,
                        error = exchanged.error,
                        message = "token exchange failed: ${exchanged.error.orEmpty()}",
                    )
                }

                commToken = exchanged.commToken.orEmpty().trim()
                val commTokenExpiresAt = exchanged.commTokenExpiresAt.orEmpty().trim()
                if (commToken.isEmpty() || commTokenExpiresAt.isEmpty()) {
                    return CurveUploadResult(ok = false, error = "missing_comm_token", message = "token exchange returned empty token")
                }

                val machine = exchanged.machine
                NanoAuthStore.save(
                    latestState.copy(
                        commToken = commToken,
                        commTokenExpiresAt = commTokenExpiresAt,
                        machineNo = machine?.machineNo ?: latestState.machineNo,
                        machineName = machine?.machineName ?: latestState.machineName,
                        model = machine?.model ?: latestState.model,
                        status = machine?.status ?: latestState.status,
                        refreshedAt = Instant.now().toString(),
                    )
                )

                return execute(commToken).toCurveUploadResult()
            }

            suspend fun exchangeExpiredToken(): CurveUploadResult? {
                val latestState = NanoAuthStore.load()
                val latestCommToken = latestState.commToken.trim()
                if (latestCommToken.isNotEmpty() && latestCommToken != commToken) {
                    commToken = latestCommToken
                    val retryLatest = execute(commToken)
                    if (retryLatest.status in 200..299 || retryLatest.errorField() != "comm_token_expired") {
                        return retryLatest.toCurveUploadResult()
                    }
                }

                return exchangeWithRootTokenAndRetry(latestState)
            }

            suspend fun recoverInvalidToken(): CurveUploadResult? {
                val latestState = NanoAuthStore.load()
                val latestCommToken = latestState.commToken.trim()
                if (latestCommToken.isNotEmpty() && latestCommToken != commToken) {
                    commToken = latestCommToken
                    val retryLatest = execute(commToken)
                    if (retryLatest.status in 200..299) {
                        return retryLatest.toCurveUploadResult()
                    }
                    val latestError = retryLatest.errorField()
                    // 与 exchangeExpiredToken 对称：二次检查若发现改口成了 comm_token_expired，
                    // 接续 exchange 续期，而不是当普通失败直接返回。
                    if (latestError == "comm_token_expired") {
                        return exchangeWithRootTokenAndRetry(latestState)
                    }
                    if (latestError != "invalid_comm_token") {
                        return retryLatest.toCurveUploadResult()
                    }
                }

                if (autoReactivate()) {
                    commToken = NanoAuthStore.load().commToken.trim()
                    if (commToken.isNotEmpty()) {
                        return execute(commToken).toCurveUploadResult()
                    }
                    return CurveUploadResult(
                        ok = false,
                        error = "missing_comm_token",
                        message = "reactivation did not yield a communication token",
                    )
                }

                return null
            }

            var resp = execute(commToken)
            if (resp.status == 401 && resp.errorField() == "comm_token_expired") {
                NanoTokenRecoveryGate.withLock { exchangeExpiredToken() }?.let { return@withContext it }
            }
            // invalid_comm_token（非过期，而是被判无效）自动兜底：本地已有 mainboardId+firmwareId，
            // 免打扰重新 /activate 一次再重试，重试仍失败才升级给人工。
            if (resp.status == 401 && resp.errorField() == "invalid_comm_token") {
                NanoTokenRecoveryGate.withLock { recoverInvalidToken() }?.let { return@withContext it }
            }
            if (resp.status in 200..299) {
                CurveUploadResult(ok = true, id = resp.successId(), status = resp.status)
            } else if (resp.status == 401 && resp.errorField() == "invalid_comm_token") {
                CurveUploadResult(ok = false, status = resp.status, error = resp.errorField(), message = "invalid_comm_token, auto reactivation failed, please check device network/backend")
            } else {
                CurveUploadResult(ok = false, status = resp.status, error = resp.errorField(), message = "kino-curve failed: HTTP ${resp.status}")
            }
        } catch (e: Exception) {
            Timber.w(e, "NanoApi.uploadCurve failed")
            CurveUploadResult(ok = false, message = e.message ?: "unknown error")
        }
    }
}
