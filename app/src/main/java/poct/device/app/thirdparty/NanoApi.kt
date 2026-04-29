package poct.device.app.thirdparty

import com.google.gson.JsonParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.bean.ConfigSysBean
import poct.device.app.entity.service.SysConfigService
import poct.device.app.thirdparty.model.nano.NanoBiomarkersReq
import poct.device.app.thirdparty.model.nano.NanoBiomarkersResp
import poct.device.app.thirdparty.model.nano.NanoChipResp
import poct.device.app.thirdparty.model.nano.NanoKinoResultReq
import poct.device.app.thirdparty.model.nano.NanoKinoResultResp
import poct.device.app.utils.common.HttpUtils
import timber.log.Timber
import java.io.IOException
import java.net.URLEncoder

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

    private val httpUtils = HttpUtils()
    private val JSON = "application/json".toMediaType()

    private suspend fun config(): ConfigSysBean =
        SysConfigService.findBean(ConfigSysBean.PREFIX, ConfigSysBean::class)

    private fun baseUrl(): String = AppParams.NANO_BASE_URL.trimEnd('/')

    private fun apiToken(): String = AppParams.NANO_API_TOKEN

    suspend fun deviceSerial(): String = config().nanoDeviceId

    private fun Request.Builder.withAuth(token: String): Request.Builder =
        if (token.isNotEmpty()) this.header("Authorization", "Bearer $token") else this

    data class ProbeResult(
        val ok: Boolean,
        val url: String,
        val status: Int? = null,
        val latencyMs: Long = 0,
        val body: String? = null,
        val error: String? = null,
    )

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
            val request = Request.Builder()
                .url(url)
                .withAuth(apiToken())
                .post(jsonBody.toRequestBody(JSON))
                .build()
            val body = httpUtils.executeRequest(request)
            App.gson.fromJson(body, NanoBiomarkersResp::class.java)
        } catch (e: IOException) {
            Timber.w(e, "NanoApi.postBiomarkers failed")
            null
        } catch (e: JsonParseException) {
            Timber.w(e, "NanoApi.postBiomarkers parse failed")
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
}
