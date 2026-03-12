package com.ankigate

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action == Intent.ACTION_BOOT_COMPLETED &&
            PermissionSetup.isAllGranted(context) &&
            Prefs.isStatusNotificationEnabled(context)
        ) {
            MonitorService.start(context)
        }
    }
}
