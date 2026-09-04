package poct.device.app.thirdparty.nano

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import poct.device.app.thirdparty.model.nano.NanoAuthState
import poct.device.app.thirdparty.model.nano.NanoMachine
import poct.device.app.thirdparty.model.nano.NanoProtectedCallExecutor
import poct.device.app.thirdparty.model.nano.NanoProtectedRequest
import poct.device.app.thirdparty.model.nano.NanoRawResponse
import poct.device.app.thirdparty.model.nano.NanoTokenExchangeResp

class NanoProtectedCallExecutorTest {
    @Test
    fun protectedCallUsesStoredCommTokenWithoutRefreshWhenSuccessful() = runBlocking {
        val store = FakeAuthStore(authState(commToken = "comm-a", rootToken = "root-a"))
        val transport = FakeTransport(
            NanoRawResponse(200, """{"found":false}""")
        )
        val executor = NanoProtectedCallExecutor(store, transport)

        val result = executor.execute(
            endpointName = "kino-chip",
            request = NanoProtectedRequest.get("https://nano.test/kino/kino-chip?chip_id=abc")
        )

        assertTrue(result.ok)
        assertEquals("""{"found":false}""", result.body)
        assertEquals(listOf("comm-a"), transport.protectedTokens)
        assertEquals(listOf("GET"), transport.protectedRequests.map { it.method })
        assertEquals(
            listOf("https://nano.test/kino/kino-chip?chip_id=abc"),
            transport.protectedRequests.map { it.url }
        )
        assertTrue(transport.exchangeTokens.isEmpty())
        assertEquals("comm-a", store.state.commToken)
    }

    @Test
    fun commTokenExpiredRefreshesOnceThenRetriesOriginalRequest() = runBlocking {
        val store = FakeAuthStore(authState(commToken = "expired", rootToken = "root-a"))
        val transport = FakeTransport(
            NanoRawResponse(401, """{"error":"comm_token_expired"}"""),
            NanoRawResponse(200, """{"success":true,"comm_token":"fresh","comm_token_expires_at":"2026-06-05T00:00:00.000Z","machine":{"machine_no":"KNA1-001","machine_name":"Device 1","model":"KNA1","status":"active"}}"""),
            NanoRawResponse(200, """{"found":true}""")
        )
        val executor = NanoProtectedCallExecutor(store, transport)

        val result = executor.execute(
            endpointName = "kino-chip",
            request = NanoProtectedRequest.get("https://nano.test/kino/kino-chip?chip_id=abc")
        )

        assertTrue(result.ok)
        assertEquals("""{"found":true}""", result.body)
        assertEquals(listOf("expired", "fresh"), transport.protectedTokens)
        assertEquals(
            listOf(
                "https://nano.test/kino/kino-chip?chip_id=abc",
                "https://nano.test/kino/kino-chip?chip_id=abc",
            ),
            transport.protectedRequests.map { it.url }
        )
        assertEquals(listOf("root-a"), transport.exchangeTokens)
        assertEquals("fresh", store.state.commToken)
        assertEquals("2026-06-05T00:00:00.000Z", store.state.commTokenExpiresAt)
        assertEquals("KNA1-001", store.state.machineNo)
        assertTrue(store.state.refreshedAt.isNotBlank())
    }

    @Test
    fun invalidRootTokenClearsLocalTokensWithoutSecondRetry() = runBlocking {
        val store = FakeAuthStore(authState(commToken = "expired", rootToken = "root-a"))
        val transport = FakeTransport(
            NanoRawResponse(401, """{"error":"comm_token_expired"}"""),
            NanoRawResponse(401, """{"error":"invalid_root_token"}""")
        )
        val executor = NanoProtectedCallExecutor(store, transport)

        val result = executor.execute(
            endpointName = "kino-chip",
            request = NanoProtectedRequest.get("https://nano.test/kino/kino-chip?chip_id=abc")
        )

        assertFalse(result.ok)
        assertEquals("invalid_root_token", result.error)
        assertEquals(401, result.status)
        assertEquals(listOf("expired"), transport.protectedTokens)
        assertEquals(listOf("root-a"), transport.exchangeTokens)
        assertEquals("", store.state.rootToken)
        assertEquals("", store.state.commToken)
        assertEquals("", store.state.commTokenExpiresAt)
        assertEquals("invalid_root_token", store.state.status)
    }

    @Test
    fun refreshNetworkFailureKeepsExistingRootToken() = runBlocking {
        val store = FakeAuthStore(authState(commToken = "expired", rootToken = "root-a"))
        val transport = FakeTransport(
            NanoRawResponse(401, """{"error":"comm_token_expired"}"""),
            NanoRawResponse(500, """{"error":"backend_down"}""")
        )
        val executor = NanoProtectedCallExecutor(store, transport)

        val result = executor.execute(
            endpointName = "kino-chip",
            request = NanoProtectedRequest.get("https://nano.test/kino/kino-chip?chip_id=abc")
        )

        assertFalse(result.ok)
        assertEquals("backend_down", result.error)
        assertEquals(500, result.status)
        assertEquals("root-a", store.state.rootToken)
        assertEquals("expired", store.state.commToken)
    }

    @Test
    fun nonExpiryErrorDoesNotRefresh() = runBlocking {
        val store = FakeAuthStore(authState(commToken = "comm-a", rootToken = "root-a"))
        val transport = FakeTransport(
            NanoRawResponse(403, """{"error":"machine_not_active"}""")
        )
        val executor = NanoProtectedCallExecutor(store, transport)

        val result = executor.execute(
            endpointName = "kino-chip",
            request = NanoProtectedRequest.get("https://nano.test/kino/kino-chip?chip_id=abc")
        )

        assertFalse(result.ok)
        assertEquals("machine_not_active", result.error)
        assertEquals(listOf("comm-a"), transport.protectedTokens)
        assertTrue(transport.exchangeTokens.isEmpty())
        assertEquals("", store.state.rootToken)
        assertEquals("", store.state.commToken)
        assertEquals("", store.state.commTokenExpiresAt)
        assertEquals("machine_not_active", store.state.status)
    }

    @Test
    fun missingCommTokenFailsBeforeNetworkRequest() = runBlocking {
        val store = FakeAuthStore(authState(commToken = "", rootToken = "root-a"))
        val transport = FakeTransport()
        val executor = NanoProtectedCallExecutor(store, transport)

        val result = executor.execute(
            endpointName = "kino-chip",
            request = NanoProtectedRequest.get("https://nano.test/kino/kino-chip?chip_id=abc")
        )

        assertFalse(result.ok)
        assertEquals("missing_comm_token", result.error)
        assertNull(result.status)
        assertTrue(transport.protectedTokens.isEmpty())
        assertTrue(transport.exchangeTokens.isEmpty())
    }

    @Test
    fun postRequestKeepsMethodUrlAndBodyWhenExecuted() = runBlocking {
        val store = FakeAuthStore(authState(commToken = "comm-a", rootToken = "root-a"))
        val transport = FakeTransport(
            NanoRawResponse(200, """{"success":true}""")
        )
        val executor = NanoProtectedCallExecutor(store, transport)
        val body = """{"software_version":"0.3.0"}"""

        val result = executor.execute(
            endpointName = "kino-machines-info",
            request = NanoProtectedRequest.post("https://nano.test/kino/kino-machines/info", body)
        )

        assertTrue(result.ok)
        assertEquals("POST", transport.protectedRequests.single().method)
        assertEquals("https://nano.test/kino/kino-machines/info", transport.protectedRequests.single().url)
        assertEquals(body, transport.protectedRequests.single().body)
        assertEquals(listOf("comm-a"), transport.protectedTokens)
    }

    @Test
    fun invalidCommTokenReactivatesAndRetriesOriginalRequest() = runBlocking {
        val store = FakeAuthStore(authState(commToken = "bad-token", rootToken = "root-a"))
        val transport = FakeTransport(
            NanoRawResponse(401, """{"error":"invalid_comm_token"}"""),
            NanoRawResponse(200, """{"found":true}""")
        )
        val reactivator = FakeReactivator(succeed = true) {
            store.state = store.state.copy(commToken = "reactivated-token")
        }
        val executor = NanoProtectedCallExecutor(store, transport, reactivator)

        val result = executor.execute(
            endpointName = "kino-chip",
            request = NanoProtectedRequest.get("https://nano.test/kino/kino-chip?chip_id=abc")
        )

        assertTrue(result.ok)
        assertEquals("""{"found":true}""", result.body)
        assertEquals(listOf("bad-token", "reactivated-token"), transport.protectedTokens)
        assertTrue(reactivator.invoked)
        assertTrue(transport.exchangeTokens.isEmpty())
    }

    @Test
    fun invalidCommTokenRetriesLatestStoredTokenBeforeReactivation() = runBlocking {
        val store = SequenceAuthStore(
            authState(commToken = "bad-token", rootToken = "root-a"),
            authState(commToken = "fresh-from-peer", rootToken = "root-a"),
        )
        val transport = FakeTransport(
            NanoRawResponse(401, """{"error":"invalid_comm_token"}"""),
            NanoRawResponse(200, """{"found":true}""")
        )
        val reactivator = FakeReactivator(succeed = false)
        val executor = NanoProtectedCallExecutor(store, transport, reactivator)

        val result = executor.execute(
            endpointName = "kino-chip",
            request = NanoProtectedRequest.get("https://nano.test/kino/kino-chip?chip_id=abc")
        )

        assertTrue(result.ok)
        assertEquals(listOf("bad-token", "fresh-from-peer"), transport.protectedTokens)
        assertFalse(reactivator.invoked)
        assertTrue(transport.exchangeTokens.isEmpty())
    }

    @Test
    fun invalidCommTokenReactivationFailureReturnsFailureWithoutRetry() = runBlocking {
        val store = FakeAuthStore(authState(commToken = "bad-token", rootToken = "root-a"))
        val transport = FakeTransport(
            NanoRawResponse(401, """{"error":"invalid_comm_token"}""")
        )
        val reactivator = FakeReactivator(succeed = false)
        val executor = NanoProtectedCallExecutor(store, transport, reactivator)

        val result = executor.execute(
            endpointName = "kino-chip",
            request = NanoProtectedRequest.get("https://nano.test/kino/kino-chip?chip_id=abc")
        )

        assertFalse(result.ok)
        assertEquals("invalid_comm_token", result.error)
        assertEquals(listOf("bad-token"), transport.protectedTokens)
        assertTrue(reactivator.invoked)
    }

    @Test
    fun invalidCommTokenWithoutReactivatorFailsImmediately() = runBlocking {
        val store = FakeAuthStore(authState(commToken = "bad-token", rootToken = "root-a"))
        val transport = FakeTransport(
            NanoRawResponse(401, """{"error":"invalid_comm_token"}""")
        )
        val executor = NanoProtectedCallExecutor(store, transport)

        val result = executor.execute(
            endpointName = "kino-chip",
            request = NanoProtectedRequest.get("https://nano.test/kino/kino-chip?chip_id=abc")
        )

        assertFalse(result.ok)
        assertEquals("invalid_comm_token", result.error)
        assertEquals(listOf("bad-token"), transport.protectedTokens)
    }

    @Test
    fun invalidCommTokenDoubleCheckDiscoveringExpiredTokenChainsToExchange() = runBlocking {
        // Symmetry check: the invalid_comm_token recovery path's double-check must handle
        // discovering comm_token_expired the same way the comm_token_expired path's own
        // double-check handles discovering invalid_comm_token — chain into the matching
        // recovery instead of failing outright.
        val store = SequenceAuthStore(
            authState(commToken = "bad-token", rootToken = "root-a"),
            authState(commToken = "fresh-but-expired", rootToken = "root-a"),
        )
        val transport = FakeTransport(
            NanoRawResponse(401, """{"error":"invalid_comm_token"}"""),
            NanoRawResponse(401, """{"error":"comm_token_expired"}"""),
            NanoRawResponse(200, """{"success":true,"comm_token":"final-token","comm_token_expires_at":"2026-06-05T00:00:00.000Z","machine":{"machine_no":"KNA1-001","machine_name":"Device 1","model":"KNA1","status":"active"}}"""),
            NanoRawResponse(200, """{"found":true}"""),
        )
        val executor = NanoProtectedCallExecutor(store, transport)

        val result = executor.execute(
            endpointName = "kino-chip",
            request = NanoProtectedRequest.get("https://nano.test/kino/kino-chip?chip_id=abc")
        )

        assertTrue(result.ok)
        assertEquals("""{"found":true}""", result.body)
        assertEquals(listOf("bad-token", "fresh-but-expired", "final-token"), transport.protectedTokens)
        assertEquals(listOf("root-a"), transport.exchangeTokens)
        assertEquals("final-token", store.state.commToken)
    }

    @Test
    fun expiredCommTokenRetriesLatestStoredTokenBeforeExchange() = runBlocking {
        val store = SequenceAuthStore(
            authState(commToken = "expired-token", rootToken = "root-a"),
            authState(commToken = "fresh-from-peer", rootToken = "root-a"),
        )
        val transport = FakeTransport(
            NanoRawResponse(401, """{"error":"comm_token_expired"}"""),
            NanoRawResponse(200, """{"found":true}""")
        )
        val executor = NanoProtectedCallExecutor(store, transport)

        val result = executor.execute(
            endpointName = "kino-chip",
            request = NanoProtectedRequest.get("https://nano.test/kino/kino-chip?chip_id=abc")
        )

        assertTrue(result.ok)
        assertEquals(listOf("expired-token", "fresh-from-peer"), transport.protectedTokens)
        assertTrue(transport.exchangeTokens.isEmpty())
    }

    @Test
    fun networkExceptionWithReactivatorStillPropagates() {
        val store = FakeAuthStore(authState(commToken = "comm-a", rootToken = "root-a"))
        val transport = ThrowThenSucceedTransport(NanoRawResponse(200, """{"found":true}"""))
        val reactivator = FakeReactivator(succeed = true) {
            store.state = store.state.copy(commToken = "reactivated-token")
        }
        val executor = NanoProtectedCallExecutor(store, transport, reactivator)

        var threw = false
        try {
            runBlocking {
                executor.execute(
                    endpointName = "kino-chip",
                    request = NanoProtectedRequest.get("https://nano.test/kino/kino-chip?chip_id=abc")
                )
            }
        } catch (_: java.io.IOException) {
            threw = true
        }
        assertTrue(threw)
        assertFalse(reactivator.invoked)
        assertEquals(listOf("comm-a"), transport.protectedTokens)
    }

    @Test
    fun networkExceptionWithoutReactivatorPropagates() {
        val store = FakeAuthStore(authState(commToken = "comm-a", rootToken = "root-a"))
        val transport = ThrowThenSucceedTransport(NanoRawResponse(200, """{"found":true}"""))
        val executor = NanoProtectedCallExecutor(store, transport)

        var threw = false
        try {
            runBlocking {
                executor.execute(
                    endpointName = "kino-chip",
                    request = NanoProtectedRequest.get("https://nano.test/kino/kino-chip?chip_id=abc")
                )
            }
        } catch (_: java.io.IOException) {
            threw = true
        }
        assertTrue(threw)
    }

    private fun authState(commToken: String, rootToken: String): NanoAuthState =
        NanoAuthState(
            rootToken = rootToken,
            commToken = commToken,
            commTokenExpiresAt = "2026-06-04T00:00:00.000Z",
            machineNo = "KNA1-001",
            machineName = "Device 1",
            model = "KNA1",
            status = "active",
            activatedAt = "2026-06-04T00:00:00.000Z",
        )

    private class FakeAuthStore(initialState: NanoAuthState) : NanoProtectedCallExecutor.AuthStore {
        var state = initialState

        override suspend fun load(): NanoAuthState = state

        override suspend fun save(state: NanoAuthState) {
            this.state = state
        }
    }

    private class SequenceAuthStore(
        private vararg val states: NanoAuthState
    ) : NanoProtectedCallExecutor.AuthStore {
        private var index = 0
        var state = states.first()

        override suspend fun load(): NanoAuthState {
            state = states.getOrElse(index) { state }
            index++
            return state
        }

        override suspend fun save(state: NanoAuthState) {
            this.state = state
        }
    }

    private class FakeTransport(
        private vararg val responses: NanoRawResponse
    ) : NanoProtectedCallExecutor.Transport {
        val protectedTokens = mutableListOf<String>()
        val exchangeTokens = mutableListOf<String>()
        val protectedRequests = mutableListOf<NanoProtectedRequest>()
        private var index = 0

        override suspend fun executeProtected(
            request: NanoProtectedRequest,
            commToken: String
        ): NanoRawResponse {
            protectedRequests += request
            protectedTokens += commToken
            return next()
        }

        override suspend fun exchangeToken(rootToken: String): NanoRawResponse {
            exchangeTokens += rootToken
            return next()
        }

        private fun next(): NanoRawResponse = responses[index++]
    }

    private class FakeReactivator(
        private val succeed: Boolean,
        private val onReactivate: () -> Unit = {},
    ) : NanoProtectedCallExecutor.Reactivator {
        var invoked = false

        override suspend fun reactivate(state: NanoAuthState): Boolean {
            invoked = true
            if (succeed) onReactivate()
            return succeed
        }
    }

    /** Throws once (simulating backoff-exhausted transport failure), then succeeds. */
    private class ThrowThenSucceedTransport(
        private val successResponse: NanoRawResponse,
    ) : NanoProtectedCallExecutor.Transport {
        val protectedTokens = mutableListOf<String>()
        private var callCount = 0

        override suspend fun executeProtected(
            request: NanoProtectedRequest,
            commToken: String
        ): NanoRawResponse {
            callCount++
            protectedTokens += commToken
            if (callCount == 1) throw java.io.IOException("network exhausted")
            return successResponse
        }

        override suspend fun exchangeToken(rootToken: String): NanoRawResponse {
            throw UnsupportedOperationException("not used in this test")
        }
    }
}
