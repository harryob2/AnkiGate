package com.ankigate

import android.content.Context

object MonitoringGate {

    enum class BlockReason {
        MISSING_PERMISSIONS,
        STATUS_NOTIFICATION_DISABLED
    }

    data class Decision(
        val canStart: Boolean,
        val blockReason: BlockReason? = null
    )

    fun evaluate(allPermissionsGranted: Boolean, statusNotificationEnabled: Boolean): Decision {
        if (!allPermissionsGranted) {
            return Decision(
                canStart = false,
                blockReason = BlockReason.MISSING_PERMISSIONS
            )
        }
        if (!statusNotificationEnabled) {
            return Decision(
                canStart = false,
                blockReason = BlockReason.STATUS_NOTIFICATION_DISABLED
            )
        }
        return Decision(canStart = true)
    }

    fun evaluate(context: Context): Decision =
        evaluate(
            allPermissionsGranted = PermissionSetup.isAllGranted(context),
            statusNotificationEnabled = Prefs.isStatusNotificationEnabled(context)
        )
}
