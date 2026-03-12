package com.ankigate

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentationTest {

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        PermissionSetup.allGrantedOverrideForTests = null
        Prefs.setStatusNotificationEnabled(context, true)
        Prefs.setSeenPermissionWizard(context, false)
        MonitorService.stop(context)
    }

    @After
    fun tearDown() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        MonitorService.stop(context)
        PermissionSetup.allGrantedOverrideForTests = null
        Prefs.setStatusNotificationEnabled(context, true)
        Prefs.setSeenPermissionWizard(context, false)
    }

    @Test
    fun opensPermissionsWizardWhenRequiredPermissionsMissing() {
        PermissionSetup.allGrantedOverrideForTests = false
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        onView(withId(R.id.tvPermissionSummary)).check(matches(isDisplayed()))

        scenario.close()
    }

    @Test
    fun doesNotAutostartMonitorWhenStatusNotificationDisabled() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        PermissionSetup.allGrantedOverrideForTests = true
        Prefs.setSeenPermissionWizard(context, true)
        Prefs.setStatusNotificationEnabled(context, false)

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity {
            assertFalse(MonitorService.isRunning)
        }
        scenario.close()
    }

    @Test
    fun autostartsMonitorWhenAllPrerequisitesMet() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        PermissionSetup.allGrantedOverrideForTests = true
        Prefs.setSeenPermissionWizard(context, true)
        Prefs.setStatusNotificationEnabled(context, true)

        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity {
            assertTrue(MonitorService.isRunning)
        }
        scenario.close()
    }
}
