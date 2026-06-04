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
import poct.device.app.R
import poct.device.app.bean.ConfigSysBean
import poct.device.app.entity.service.SysConfigService
import poct.device.app.thirdparty.model.nano.NanoActivateReq
import poct.device.app.thirdparty.model.nano.NanoActivateResp
import poct.device.app.thirdparty.model.nano.NanoAuthState
import poct.device.app.thirdparty.model.nano.NanoAuthSupport
import poct.device.app.thirdparty.model.nano.NanoBiomarkersReq
import poct.device.app.thirdparty.model.nano.NanoBiomarkersResp
import poct.device.app.thirdparty.model.nano.NanoChipResp
import poct.device.app.thirdparty.model.nano.NanoKinoResultReq
import poct.device.app.thirdparty.model.nano.NanoKinoResultResp
import poct.device.app.utils.common.HttpUtils
import timber.log.Timber
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Thin client for the Waven Nano AI backend (Aliyun FC 3.0).
 * Mirrors the three calls the WeChat miniapp Kino Simulator makes:
 *   GET  /api/kino-chip?chip_id=...
 *   POST /api/biomarkers
 *   POST /api/kino-result
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

    private val httpUtils = HttpUtils()
    private val httpUtilsSlow = HttpUtils(readTimeout = 60)
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

    private fun apiToken(): String = AppParams.NANO_API_TOKEN

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

    suspend fun activateDevice(
        mainboardId: String,
        firmwareId: String,
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

    /**
     * Hit `${nanoBaseUrl}/api/kino-chip?chip_id=__ping__` and return a structured
     * result. The endpoint exists in the nano worker and responds with
     * `{"found":false}` (HTTP 200) for unknown chips — that proves both reachability
     * and that the request was parsed by the worker, not just that DNS resolved.
     */
    suspend fun probe(): ProbeResult = withContext(Dispatchers.IO) {
        val base = baseUrl()
        if (base.isEmpty()) {
            return@withContext ProbeResult(ok = false, url = "", error = "nanoBaseUrl not configured")
        }
        val url = "$base/api/kino-chip?chip_id=__ping__"
        val start = System.currentTimeMillis()
        try {
            val client = httpUtils.buildClient()
            val request = Request.Builder().url(url).withAuth(apiToken()).get().build()
            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - start
                val body = response.body?.string()?.take(500)
                val ok = response.isSuccessful
                ProbeResult(
                    ok = ok,
                    url = url,
                    status = response.code,
                    latencyMs = latency,
                    body = body,
                    error = if (!ok) "HTTP ${response.code}" else null,
                )
            }
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
        val url = "$base/api/kino-chip?chip_id=${URLEncoder.encode(chipId, "UTF-8")}"
        try {
            val request = Request.Builder().url(url).withAuth(apiToken()).get().build()
            val body = httpUtils.executeRequest(request)
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
        val url = "$base/api/biomarkers"
        try {
            val jsonBody = App.gson.toJson(req)
            Timber.w("NanoApi.postBiomarkers req=$jsonBody")
            val request = Request.Builder()
                .url(url)
                .withAuth(apiToken())
                .post(jsonBody.toRequestBody(JSON))
                .build()
            val body = httpUtilsSlow.executeRequest(request)
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
        val url = "$base/api/kino-result"
        try {
            val jsonBody = App.gson.toJson(req)
            val request = Request.Builder()
                .url(url)
                .withAuth(apiToken())
                .post(jsonBody.toRequestBody(JSON))
                .build()
            val body = httpUtils.executeRequest(request)
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
     * Queries `${nanoBaseUrl}/api/kino-upgrade` for the latest APK version and OSS URL.
     * Returns null if the request fails or is unauthorized.
     */
    suspend fun checkUpgrade(): poct.device.app.thirdparty.model.nano.NanoUpgradeResp? = withContext(Dispatchers.IO) {
        val base = baseUrl()
        if (base.isEmpty()) {
            Timber.w("NanoApi.checkUpgrade: nanoBaseUrl not configured")
            return@withContext null
        }
        val url = "$base/api/kino-upgrade"
        try {
            val request = Request.Builder().url(url).withAuth(apiToken()).get().build()
            val body = httpUtils.executeRequest(request)
            App.gson.fromJson(body, poct.device.app.thirdparty.model.nano.NanoUpgradeResp::class.java)
        } catch (e: Exception) {
            Timber.w(e, "NanoApi.checkUpgrade failed")
            null
        }
    }
}
