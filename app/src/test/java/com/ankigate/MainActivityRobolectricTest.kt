package com.ankigate

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class MainActivityRobolectricTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        PermissionSetup.allGrantedOverrideForTests = null
        Prefs.setStatusNotificationEnabled(app, true)
        Prefs.setSeenPermissionWizard(app, true)
        drainStartedServices()
    }

    @After
    fun tearDown() {
        PermissionSetup.allGrantedOverrideForTests = null
        Prefs.setStatusNotificationEnabled(app, true)
        Prefs.setSeenPermissionWizard(app, false)
        drainStartedServices()
    }

    @Test
    fun `start button does not start monitor when persistent notification disabled`() {
        PermissionSetup.allGrantedOverrideForTests = true
        Prefs.setStatusNotificationEnabled(app, false)

        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        drainStartedServices()

        activity.findViewById<android.widget.Button>(R.id.btnToggleService).performClick()

        assertNull(shadowOf(app).peekNextStartedService())
    }

    @Test
    fun `start button starts monitor when prerequisites become valid`() {
        PermissionSetup.allGrantedOverrideForTests = false
        Prefs.setStatusNotificationEnabled(app, true)

        val activity = Robolectric.buildActivity(MainActivity::class.java).setup().get()
        drainStartedServices()

        PermissionSetup.allGrantedOverrideForTests = true
        activity.findViewById<android.widget.Button>(R.id.btnToggleService).performClick()

        assertEquals(
            android.content.Intent(app, MonitorService::class.java).component,
            shadowOf(app).peekNextStartedService()?.component
        )
    }

    private fun drainStartedServices() {
        while (shadowOf(app).peekNextStartedService() != null) {
            shadowOf(app).nextStartedService
        }
    }
}
