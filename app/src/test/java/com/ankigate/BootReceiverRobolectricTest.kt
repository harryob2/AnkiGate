package com.ankigate

import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class BootReceiverRobolectricTest {

    private lateinit var app: Application
    private lateinit var receiver: BootReceiver

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        receiver = BootReceiver()
        PermissionSetup.allGrantedOverrideForTests = null
        Prefs.setStatusNotificationEnabled(app, true)
        drainStartedServices()
    }

    @After
    fun tearDown() {
        PermissionSetup.allGrantedOverrideForTests = null
        Prefs.setStatusNotificationEnabled(app, true)
        drainStartedServices()
    }

    @Test
    fun `starts monitor on boot when prerequisites are met`() {
        PermissionSetup.allGrantedOverrideForTests = true
        Prefs.setStatusNotificationEnabled(app, true)

        receiver.onReceive(app, Intent(Intent.ACTION_BOOT_COMPLETED))

        val startedIntent = shadowOf(app).peekNextStartedService()
        assertEquals(
            Intent(app, MonitorService::class.java).component,
            startedIntent?.component
        )
    }

    @Test
    fun `does not start monitor on boot when prerequisites fail`() {
        PermissionSetup.allGrantedOverrideForTests = false
        Prefs.setStatusNotificationEnabled(app, true)

        receiver.onReceive(app, Intent(Intent.ACTION_BOOT_COMPLETED))

        assertNull(shadowOf(app).peekNextStartedService())
    }

    @Test
    fun `ignores non boot broadcasts`() {
        PermissionSetup.allGrantedOverrideForTests = true
        Prefs.setStatusNotificationEnabled(app, true)

        receiver.onReceive(app, Intent(Intent.ACTION_MY_PACKAGE_REPLACED))

        assertNull(shadowOf(app).peekNextStartedService())
    }

    private fun drainStartedServices() {
        while (shadowOf(app).peekNextStartedService() != null) {
            shadowOf(app).nextStartedService
        }
    }
}
