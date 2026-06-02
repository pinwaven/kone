package poct.device.app.state

import kotlinx.coroutines.flow.MutableStateFlow

class RuntimeModeState(
    private val nowMillis: () -> Long = { System.currentTimeMillis() }
) {
    val venueModeEnabled = MutableStateFlow(false)
    val nanoEnvironment = MutableStateFlow(NanoEnvironment.PRODUCTION)

    private var factoryTestUnlockedAtMillis: Long? = null
    private var homeVenueDetectionWaitCompleted = false
    private var venueContinueCutOffWaitSkipRequested = false

    fun unlockFactoryTest(password: String): Boolean {
        if (password != FACTORY_TEST_PASSWORD) {
            return false
        }
        factoryTestUnlockedAtMillis = nowMillis()
        return true
    }

    fun isFactoryTestUnlocked(): Boolean {
        val unlockedAt = factoryTestUnlockedAtMillis ?: return false
        return nowMillis() - unlockedAt < FACTORY_TEST_UNLOCK_DURATION_MILLIS
    }

    fun setVenueModeEnabled(enabled: Boolean) {
        venueModeEnabled.value = enabled
        if (!enabled) {
            homeVenueDetectionWaitCompleted = false
            venueContinueCutOffWaitSkipRequested = false
        }
    }

    fun shouldSkipHomeVenueWait(): Boolean {
        return venueModeEnabled.value && homeVenueDetectionWaitCompleted
    }

    fun markHomeVenueDetectionWaitComplete() {
        if (venueModeEnabled.value) {
            homeVenueDetectionWaitCompleted = true
        }
    }

    fun requestVenueContinueDetection() {
        if (venueModeEnabled.value) {
            venueContinueCutOffWaitSkipRequested = true
        }
    }

    fun consumeVenueContinueCutOffWaitSkip(): Boolean {
        if (!venueModeEnabled.value || !venueContinueCutOffWaitSkipRequested) {
            return false
        }
        venueContinueCutOffWaitSkipRequested = false
        return true
    }

    fun switchToNextNanoEnvironment() {
        nanoEnvironment.value = when (nanoEnvironment.value) {
            NanoEnvironment.PRODUCTION -> NanoEnvironment.DEVELOPMENT
            NanoEnvironment.DEVELOPMENT -> NanoEnvironment.TEST
            NanoEnvironment.TEST -> NanoEnvironment.PRODUCTION
        }
    }

    fun setNanoEnvironment(environment: NanoEnvironment) {
        nanoEnvironment.value = environment
    }

    fun nanoBaseUrl(): String = nanoEnvironment.value.baseUrl

    enum class NanoEnvironment(
        val label: String,
        val baseUrl: String,
    ) {
        PRODUCTION("生产", "https://nano.fros.cc"),
        DEVELOPMENT("开发", "https://nano-dev.fros.cc"),
        TEST("测试", "https://nano-test.fros.cc"),
    }

    companion object {
        const val FACTORY_TEST_PASSWORD = "5988"
        const val FACTORY_TEST_UNLOCK_DURATION_MILLIS = 60 * 60 * 1000L
    }
}
