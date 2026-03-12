package com.ankigate

import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.junit.Assert.assertEquals
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
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()

        val resumedActivity = currentResumedActivity()
        assertEquals(PermissionsWizardActivity::class.java, resumedActivity?.javaClass)

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

    private fun currentResumedActivity(): android.app.Activity? {
        var resumedActivity: android.app.Activity? = null
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val resumed = ActivityLifecycleMonitorRegistry.getInstance()
                .getActivitiesInStage(Stage.RESUMED)
            resumedActivity = resumed.firstOrNull()
        }
        return resumedActivity
    }
}
