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

    private class FakeTransport(
        private vararg val responses: NanoRawResponse
    ) : NanoProtectedCallExecutor.Transport {
        val protectedTokens = mutableListOf<String>()
        val exchangeTokens = mutableListOf<String>()
        private var index = 0

        override suspend fun executeProtected(
            request: NanoProtectedRequest,
            commToken: String
        ): NanoRawResponse {
            protectedTokens += commToken
            return next()
        }

        override suspend fun exchangeToken(rootToken: String): NanoRawResponse {
            exchangeTokens += rootToken
            return next()
        }

        private fun next(): NanoRawResponse = responses[index++]
    }
}
