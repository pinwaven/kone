package poct.device.app.thirdparty.model.nano

import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
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

    fun kinoCurve(baseUrl: String): String = "${base(baseUrl)}/kino/kino-curve"

    fun deviceConfig(baseUrl: String): String = "${base(baseUrl)}/kino/device-config"

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

object NanoTokenRecoveryGate {
    private val mutex = Mutex()

    suspend fun <T> withLock(block: suspend () -> T): T = mutex.withLock { block() }
}

class NanoProtectedCallExecutor(
    private val authStore: AuthStore,
    private val transport: Transport,
    private val reactivator: Reactivator? = null,
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

    /**
     * Re-runs `/activate` with the locally cached mainboard/firmware id, no human
     * involved. Used when the comm token itself is rejected (`invalid_comm_token`).
     * Must persist the new auth state via [AuthStore.save] before returning true.
     */
    interface Reactivator {
        suspend fun reactivate(state: NanoAuthState): Boolean
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
        if (firstError == "invalid_comm_token") {
            return recoverInvalidCommToken(
                endpointName = endpointName,
                request = request,
                state = initialState,
                staleCommToken = initialCommToken,
                fallbackError = firstError,
                fallbackStatus = first.status,
            )
        }
        if (firstError != "comm_token_expired") {
            invalidateAuthIfNeeded(initialState, firstError)
            return first.toFailure(endpointName, firstError)
        }

        return NanoTokenRecoveryGate.withLock {
            val latestState = authStore.load()
            val latestCommToken = latestState.commToken.trim()
            if (latestCommToken.isNotEmpty() && latestCommToken != initialCommToken) {
                val retryLatest = transport.executeProtected(request, latestCommToken)
                if (retryLatest.isSuccessful()) {
                    return@withLock NanoProtectedCallResult(ok = true, status = retryLatest.status, body = retryLatest.body)
                }
                val latestError = retryLatest.errorValue()
                if (latestError == "invalid_comm_token") {
                    return@withLock recoverInvalidCommTokenLocked(
                        endpointName = endpointName,
                        request = request,
                        state = latestState,
                        fallbackError = latestError,
                        fallbackStatus = retryLatest.status,
                    )
                }
                if (latestError != "comm_token_expired") {
                    invalidateAuthIfNeeded(latestState, latestError)
                    return@withLock retryLatest.toFailure(endpointName, latestError)
                }
            }

            exchangeAndRetry(endpointName, request, latestState, first.status)
        }
    }

    /** Refreshes commToken via the rootToken exchange and retries the original request once. */
    private suspend fun exchangeAndRetry(
        endpointName: String,
        request: NanoProtectedRequest,
        state: NanoAuthState,
        fallbackStatus: Int?,
    ): NanoProtectedCallResult {
        val rootToken = state.rootToken.trim()
        if (rootToken.isEmpty()) {
            return NanoProtectedCallResult(
                ok = false,
                status = fallbackStatus,
                error = "missing_root_token",
                message = "$endpointName failed: communication token expired and root token is missing",
            )
        }

        val exchange = transport.exchangeToken(rootToken)
        if (!exchange.isSuccessful()) {
            val exchangeError = exchange.errorValue()
            invalidateAuthIfNeeded(state, exchangeError)
            return exchange.toFailure("token-exchange", exchangeError)
        }

        val exchanged = exchange.parseTokenExchange()
            ?: return NanoProtectedCallResult(
                ok = false,
                status = exchange.status,
                error = "parse_error",
                message = "token-exchange failed: response parse failed",
            )

        if (!exchanged.success) {
            invalidateAuthIfNeeded(state, exchanged.error)
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

        val refreshedState = state.withExchange(exchanged, refreshedCommToken, refreshedExpiresAt)
        authStore.save(refreshedState)

        return retryWithRecoveredToken(endpointName, request, refreshedState, refreshedCommToken)
    }

    private suspend fun recoverInvalidCommToken(
        endpointName: String,
        request: NanoProtectedRequest,
        state: NanoAuthState,
        staleCommToken: String,
        fallbackError: String,
        fallbackStatus: Int?,
    ): NanoProtectedCallResult =
        NanoTokenRecoveryGate.withLock {
            val latestState = authStore.load()
            val latestCommToken = latestState.commToken.trim()
            if (latestCommToken.isNotEmpty() && latestCommToken != staleCommToken) {
                val retryLatest = transport.executeProtected(request, latestCommToken)
                if (retryLatest.isSuccessful()) {
                    return@withLock NanoProtectedCallResult(ok = true, status = retryLatest.status, body = retryLatest.body)
                }
                val latestError = retryLatest.errorValue()
                // 与 execute() 里 comm_token_expired 分支对称：二次检查若发现改口成了
                // comm_token_expired，接续 exchange 续期，而不是当普通失败直接返回。
                if (latestError == "comm_token_expired") {
                    return@withLock exchangeAndRetry(endpointName, request, latestState, retryLatest.status)
                }
                if (latestError != "invalid_comm_token") {
                    invalidateAuthIfNeeded(latestState, latestError)
                    return@withLock retryLatest.toFailure(endpointName, latestError)
                }
            }

            recoverInvalidCommTokenLocked(
                endpointName = endpointName,
                request = request,
                state = latestState.takeIf { it.commToken.isNotBlank() } ?: state,
                fallbackError = fallbackError,
                fallbackStatus = fallbackStatus,
            )
        }

    private suspend fun recoverInvalidCommTokenLocked(
        endpointName: String,
        request: NanoProtectedRequest,
        state: NanoAuthState,
        fallbackError: String,
        fallbackStatus: Int?,
    ): NanoProtectedCallResult {
        val reactivator = this.reactivator
        if (reactivator == null || !reactivator.reactivate(state)) {
            return NanoProtectedCallResult(
                ok = false,
                status = fallbackStatus,
                error = fallbackError,
                message = "$endpointName failed: $fallbackError, auto reactivation unavailable or failed",
            )
        }

        val refreshedState = authStore.load()
        val refreshedCommToken = refreshedState.commToken.trim()
        if (refreshedCommToken.isEmpty()) {
            return NanoProtectedCallResult(
                ok = false,
                error = "missing_comm_token",
                message = "$endpointName failed: reactivation did not yield a communication token",
            )
        }

        return retryWithRecoveredToken(endpointName, request, refreshedState, refreshedCommToken)
    }

    private suspend fun retryWithRecoveredToken(
        endpointName: String,
        request: NanoProtectedRequest,
        state: NanoAuthState,
        commToken: String,
    ): NanoProtectedCallResult {
        val retry = try {
            transport.executeProtected(request, commToken)
        } catch (e: IOException) {
            return NanoProtectedCallResult(
                ok = false,
                error = "network_error",
                message = "$endpointName failed after reactivation: ${e.message}",
            )
        }
        return if (retry.isSuccessful()) {
            NanoProtectedCallResult(ok = true, status = retry.status, body = retry.body)
        } else {
            val retryError = retry.errorValue()
            invalidateAuthIfNeeded(state, retryError)
            retry.toFailure(endpointName, retryError)
        }
    }

    private suspend fun invalidateAuthIfNeeded(state: NanoAuthState, error: String?) {
        if (error == "invalid_root_token" || error == "machine_not_active") {
            authStore.save(state.invalidated(error))
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
