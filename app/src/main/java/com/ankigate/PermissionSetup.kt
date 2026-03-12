package com.ankigate

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionSetup {

    // Test-only override to make permission-gated flows deterministic in local/CI tests.
    @Volatile
    var allGrantedOverrideForTests: Boolean? = null

    enum class Step {
        ANKI_DB,
        NOTIFICATIONS,
        USAGE_ACCESS,
        OVERLAY
    }

    fun isAnkiDbGranted(context: Context): Boolean =
        AnkiChecker.isAnkiInstalled(context) && AnkiChecker.hasAnkiPermission(context)

    fun isNotificationsGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isUsageAccessGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun isOverlayGranted(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    fun isAllGranted(context: Context): Boolean {
        allGrantedOverrideForTests?.let { return it }
        return isAnkiDbGranted(context) &&
            isNotificationsGranted(context) &&
            isUsageAccessGranted(context) &&
            isOverlayGranted(context)
    }

    fun grantedCount(context: Context): Int {
        var count = 0
        if (isAnkiDbGranted(context)) count++
        if (isNotificationsGranted(context)) count++
        if (isUsageAccessGranted(context)) count++
        if (isOverlayGranted(context)) count++
        return count
    }

    fun totalCount(): Int = 4

    fun nextMissingStep(context: Context): Step? {
        if (!isAnkiDbGranted(context)) return Step.ANKI_DB
        if (!isNotificationsGranted(context)) return Step.NOTIFICATIONS
        if (!isUsageAccessGranted(context)) return Step.USAGE_ACCESS
        if (!isOverlayGranted(context)) return Step.OVERLAY
        return null
    }

    fun openUsageAccessSettings(activity: Activity) {
        activity.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }

    fun openOverlaySettings(activity: Activity) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${activity.packageName}")
        )
        activity.startActivity(intent)
    }
}
