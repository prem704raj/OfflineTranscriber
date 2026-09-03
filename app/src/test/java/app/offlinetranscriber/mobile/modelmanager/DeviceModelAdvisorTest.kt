package app.offlinetranscriber.mobile.modelmanager

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceModelAdvisorTest {

    @Test
    fun recommendsFastForLowRam() {
        val lowRamProfile = DeviceProfile(
            totalRamBytes = 3L * 1024L * 1024L * 1024L, // 3 GB
            cpuCores = 4
        )
        val recommendation = DeviceModelAdvisor.recommendForProfile(lowRamProfile)
        assertEquals(ModelCatalog.fast.id, recommendation.id)
    }

    @Test
    fun recommendsBalancedForStandardRam() {
        val balancedProfile = DeviceProfile(
            totalRamBytes = 6L * 1024L * 1024L * 1024L, // 6 GB
            cpuCores = 8
        )
        val recommendation = DeviceModelAdvisor.recommendForProfile(balancedProfile)
        assertEquals(ModelCatalog.balanced.id, recommendation.id)
    }

    @Test
    fun recommendsAccurateForHighEndHardware() {
        val highEndProfile = DeviceProfile(
            totalRamBytes = 12L * 1024L * 1024L * 1024L, // 12 GB
            cpuCores = 8
        )
        val recommendation = DeviceModelAdvisor.recommendForProfile(highEndProfile)
        assertEquals(ModelCatalog.accurate.id, recommendation.id)
    }
}
