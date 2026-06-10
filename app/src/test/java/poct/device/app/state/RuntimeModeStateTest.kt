package poct.device.app.state

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeModeStateTest {
    @Test
    fun factoryTestUnlockAcceptsPasswordForOneHourOnly() {
        val state = RuntimeModeState(nowMillis = { currentTime })

        assertFalse(state.isFactoryTestUnlocked())
        assertFalse(state.unlockFactoryTest("1234"))

        assertTrue(state.unlockFactoryTest("5988"))
        assertTrue(state.isFactoryTestUnlocked())

        currentTime += RuntimeModeState.FACTORY_TEST_UNLOCK_DURATION_MILLIS - 1
        assertTrue(state.isFactoryTestUnlocked())

        currentTime += 1
        assertFalse(state.isFactoryTestUnlocked())
    }

    @Test
    fun factoryTestUnlockIsMemoryOnly() {
        val firstState = RuntimeModeState(nowMillis = { currentTime })
        assertTrue(firstState.unlockFactoryTest("5988"))
        assertTrue(firstState.isFactoryTestUnlocked())

        val restartedState = RuntimeModeState(nowMillis = { currentTime })
        assertFalse(restartedState.isFactoryTestUnlocked())
    }

    @Test
    fun venueModeDefaultsOffAndResetsFirstDetectionWhenDisabled() {
        val state = RuntimeModeState(nowMillis = { currentTime })

        assertFalse(state.venueModeEnabled.value)
        assertFalse(state.shouldSkipHomeVenueWait())

        state.setVenueModeEnabled(true)
        assertFalse(state.shouldSkipHomeVenueWait())

        state.markHomeVenueDetectionWaitComplete()
        assertTrue(state.shouldSkipHomeVenueWait())

        state.setVenueModeEnabled(false)
        assertFalse(state.venueModeEnabled.value)
        assertFalse(state.shouldSkipHomeVenueWait())

        state.setVenueModeEnabled(true)
        assertFalse(state.shouldSkipHomeVenueWait())
    }

    @Test
    fun venueContinueDetectionSkipsNextCutOffWaitOnceOnly() {
        val state = RuntimeModeState(nowMillis = { currentTime })

        state.requestVenueContinueDetection()
        assertFalse(state.consumeVenueContinueCutOffWaitSkip())

        state.setVenueModeEnabled(true)
        state.requestVenueContinueDetection()

        assertTrue(state.consumeVenueContinueCutOffWaitSkip())
        assertFalse(state.consumeVenueContinueCutOffWaitSkip())

        state.requestVenueContinueDetection()
        state.setVenueModeEnabled(false)
        assertFalse(state.consumeVenueContinueCutOffWaitSkip())
    }

    @Test
    fun testModeAndVenueModeAreMutuallyExclusive() {
        val state = RuntimeModeState(nowMillis = { currentTime })

        assertFalse(state.testModeEnabled.value)
        assertFalse(state.venueModeEnabled.value)

        state.setVenueModeEnabled(true)
        assertTrue(state.venueModeEnabled.value)
        assertFalse(state.testModeEnabled.value)

        state.setTestModeEnabled(true)
        assertTrue(state.testModeEnabled.value)
        assertFalse(state.venueModeEnabled.value)

        state.setVenueModeEnabled(true)
        assertTrue(state.venueModeEnabled.value)
        assertFalse(state.testModeEnabled.value)
    }

    @Test
    fun nanoEnvironmentDefaultsToProductionAndCyclesInMemory() {
        val state = RuntimeModeState(nowMillis = { currentTime })

        assertEquals(RuntimeModeState.NanoEnvironment.PRODUCTION, state.nanoEnvironment.value)
        assertEquals("https://nano.fros.cc", state.nanoBaseUrl())

        state.switchToNextNanoEnvironment()
        assertEquals(RuntimeModeState.NanoEnvironment.DEVELOPMENT, state.nanoEnvironment.value)
        assertEquals("https://nano-dev.fros.cc", state.nanoBaseUrl())

        state.switchToNextNanoEnvironment()
        assertEquals(RuntimeModeState.NanoEnvironment.TEST, state.nanoEnvironment.value)
        assertEquals("https://nano-test.fros.cc", state.nanoBaseUrl())

        state.switchToNextNanoEnvironment()
        assertEquals(RuntimeModeState.NanoEnvironment.PRODUCTION, state.nanoEnvironment.value)
        assertEquals("https://nano.fros.cc", state.nanoBaseUrl())
    }

    @Test
    fun nanoEnvironmentCanBeSelectedDirectly() {
        val state = RuntimeModeState(nowMillis = { currentTime })

        state.setNanoEnvironment(RuntimeModeState.NanoEnvironment.TEST)

        assertEquals(RuntimeModeState.NanoEnvironment.TEST, state.nanoEnvironment.value)
        assertEquals("https://nano-test.fros.cc", state.nanoBaseUrl())
    }

    private var currentTime = 1_000L
}
