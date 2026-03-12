package com.ankigate

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoringGateTest {

    @Test
    fun `allows start when permissions granted and notification enabled`() {
        val result = MonitoringGate.evaluate(
            allPermissionsGranted = true,
            statusNotificationEnabled = true
        )

        assertTrue(result.canStart)
        assertNull(result.blockReason)
    }

    @Test
    fun `blocks start when permissions missing`() {
        val result = MonitoringGate.evaluate(
            allPermissionsGranted = false,
            statusNotificationEnabled = true
        )

        assertEquals(false, result.canStart)
        assertEquals(MonitoringGate.BlockReason.MISSING_PERMISSIONS, result.blockReason)
    }

    @Test
    fun `blocks start when persistent notification disabled`() {
        val result = MonitoringGate.evaluate(
            allPermissionsGranted = true,
            statusNotificationEnabled = false
        )

        assertEquals(false, result.canStart)
        assertEquals(MonitoringGate.BlockReason.STATUS_NOTIFICATION_DISABLED, result.blockReason)
    }

    @Test
    fun `prioritizes missing permissions reason when both conditions fail`() {
        val result = MonitoringGate.evaluate(
            allPermissionsGranted = false,
            statusNotificationEnabled = false
        )

        assertEquals(false, result.canStart)
        assertEquals(MonitoringGate.BlockReason.MISSING_PERMISSIONS, result.blockReason)
    }
}
