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
class MonitorServiceStartRobolectricTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
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
    fun `does not start service when permissions missing`() {
        PermissionSetup.allGrantedOverrideForTests = false
        Prefs.setStatusNotificationEnabled(app, true)

        MonitorService.start(app)

        assertNull(shadowOf(app).peekNextStartedService())
    }

    @Test
    fun `does not start service when status notification disabled`() {
        PermissionSetup.allGrantedOverrideForTests = true
        Prefs.setStatusNotificationEnabled(app, false)

        MonitorService.start(app)

        assertNull(shadowOf(app).peekNextStartedService())
    }

    @Test
    fun `starts monitor service when all prerequisites are met`() {
        PermissionSetup.allGrantedOverrideForTests = true
        Prefs.setStatusNotificationEnabled(app, true)

        MonitorService.start(app)

        val startedIntent = shadowOf(app).peekNextStartedService()
        assertEquals(
            Intent(app, MonitorService::class.java).component,
            startedIntent?.component
        )
    }

    private fun drainStartedServices() {
        while (shadowOf(app).peekNextStartedService() != null) {
            shadowOf(app).nextStartedService
        }
    }
}
