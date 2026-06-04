package poct.device.app.thirdparty.model.nano

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import java.net.URLEncoder
import java.time.Instant

object NanoEndpoints {
    fun probe(baseUrl: String): String = kinoChip(baseUrl, "__ping__")

    fun kinoChip(baseUrl: String, chipId: String): String =
        "${base(baseUrl)}/kino/kino-chip?chip_id=${URLEncoder.encode(chipId, "UTF-8")}"

    fun biomarkers(baseUrl: String): String = "${base(baseUrl)}/kino/biomarkers"

    fun kinoResult(baseUrl: String): String = "${base(baseUrl)}/kino/kino-result"

    fun machineInfo(baseUrl: String): String = "${base(baseUrl)}/kino/kino-machines/info"

    fun deviceMe(baseUrl: String): String = "${base(baseUrl)}/kino/device/me"

    fun kinoUpgrade(baseUrl: String): String = "${base(baseUrl)}/kino/kino-upgrade"

    fun tokenExchange(baseUrl: String): String = "${base(baseUrl)}/kino/token/exchange"

    private fun base(baseUrl: String): String = baseUrl.trimEnd('/')
}

data class NanoProtectedRequest(
    val method: String,
    val url: String,
    val body: String? = null,
) {
    companion object {
        fun get(url: String): NanoProtectedRequest = NanoProtectedRequest("GET", url)

        fun post(url: String, body: String): NanoProtectedRequest =
            NanoProtectedRequest("POST", url, body)
    }
}

data class NanoRawResponse(
    val status: Int,
    val body: String,
)

data class NanoProtectedCallResult(
    val ok: Boolean,
    val body: String? = null,
    val status: Int? = null,
    val error: String? = null,
    val message: String? = null,
)

data class NanoTokenExchangeResp(
    val success: Boolean = false,
    val machine: NanoMachine? = null,
    val commToken: String? = null,
    val commTokenExpiresAt: String? = null,
    val error: String? = null,
)

class NanoProtectedCallExecutor(
    private val authStore: AuthStore,
    private val transport: Transport,
    private val gson: Gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .create(),
) {
    interface AuthStore {
        suspend fun load(): NanoAuthState
        suspend fun save(state: NanoAuthState)
    }

    interface Transport {
        suspend fun executeProtected(
            request: NanoProtectedRequest,
            commToken: String,
        ): NanoRawResponse

        suspend fun exchangeToken(rootToken: String): NanoRawResponse
    }

    suspend fun execute(
        endpointName: String,
        request: NanoProtectedRequest,
    ): NanoProtectedCallResult {
        val initialState = authStore.load()
        val initialCommToken = initialState.commToken.trim()
        if (initialCommToken.isEmpty()) {
            return NanoProtectedCallResult(
                ok = false,
                error = "missing_comm_token",
                message = "$endpointName failed: missing communication token",
            )
        }

        val first = transport.executeProtected(request, initialCommToken)
        if (first.isSuccessful()) {
            return NanoProtectedCallResult(ok = true, status = first.status, body = first.body)
        }

        val firstError = first.errorValue()
        if (firstError != "comm_token_expired") {
            return first.toFailure(endpointName, firstError)
        }

        val rootToken = initialState.rootToken.trim()
        if (rootToken.isEmpty()) {
            return NanoProtectedCallResult(
                ok = false,
                status = first.status,
                error = "missing_root_token",
                message = "$endpointName failed: communication token expired and root token is missing",
            )
        }

        val exchange = transport.exchangeToken(rootToken)
        if (!exchange.isSuccessful()) {
            return exchange.toFailure("token-exchange", exchange.errorValue())
        }

        val exchanged = exchange.parseTokenExchange()
            ?: return NanoProtectedCallResult(
                ok = false,
                status = exchange.status,
                error = "parse_error",
                message = "token-exchange failed: response parse failed",
            )

        if (!exchanged.success) {
            return NanoProtectedCallResult(
                ok = false,
                status = exchange.status,
                error = exchanged.error,
                message = "token-exchange failed: ${exchanged.error.orEmpty()}",
            )
        }

        val refreshedCommToken = exchanged.commToken.orEmpty().trim()
        val refreshedExpiresAt = exchanged.commTokenExpiresAt.orEmpty().trim()
        if (refreshedCommToken.isEmpty() || refreshedExpiresAt.isEmpty()) {
            return NanoProtectedCallResult(
                ok = false,
                status = exchange.status,
                error = "missing_comm_token",
                message = "token-exchange failed: response missing communication token",
            )
        }

        val refreshedState = initialState.withExchange(exchanged, refreshedCommToken, refreshedExpiresAt)
        authStore.save(refreshedState)

        val retry = transport.executeProtected(request, refreshedCommToken)
        return if (retry.isSuccessful()) {
            NanoProtectedCallResult(ok = true, status = retry.status, body = retry.body)
        } else {
            retry.toFailure(endpointName, retry.errorValue())
        }
    }

    private fun NanoRawResponse.isSuccessful(): Boolean = status in 200..299

    private fun NanoRawResponse.errorValue(): String? =
        runCatching {
            gson.fromJson(body, JsonObject::class.java)
                ?.get("error")
                ?.takeIf { !it.isJsonNull }
                ?.asString
        }.getOrNull()

    private fun NanoRawResponse.parseTokenExchange(): NanoTokenExchangeResp? =
        try {
            gson.fromJson(body, NanoTokenExchangeResp::class.java)
        } catch (_: JsonParseException) {
            null
        } catch (_: IllegalStateException) {
            null
        }

    private fun NanoRawResponse.toFailure(endpointName: String, error: String?): NanoProtectedCallResult =
        NanoProtectedCallResult(
            ok = false,
            status = status,
            error = error,
            message = "$endpointName failed: HTTP $status${error?.let { ", error=$it" }.orEmpty()}",
        )

    private fun NanoAuthState.withExchange(
        exchanged: NanoTokenExchangeResp,
        commToken: String,
        commTokenExpiresAt: String,
    ): NanoAuthState {
        val machine = exchanged.machine
        return copy(
            commToken = commToken,
            commTokenExpiresAt = commTokenExpiresAt,
            machineNo = machine?.machineNo ?: machineNo,
            machineName = machine?.machineName ?: machineName,
            model = machine?.model ?: model,
            status = machine?.status ?: status,
            refreshedAt = Instant.now().toString(),
        )
    }
}
