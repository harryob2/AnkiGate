package com.ankigate

import android.content.Context
import android.content.SharedPreferences

object Prefs {

    private const val NAME = "ankigate_prefs"
    private const val KEY_SELECTED_DECKS = "selected_decks"
    private const val KEY_BLOCKED_PACKAGES = "blocked_packages"
    private const val KEY_STATUS_NOTIFICATION_ENABLED = "status_notification_enabled"
    private const val KEY_PERMISSION_WIZARD_SEEN = "permission_wizard_seen"

    private val DEFAULT_DECKS = emptySet<String>()
    private val DEFAULT_PACKAGES = emptySet<String>()

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun getSelectedDecks(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_SELECTED_DECKS, DEFAULT_DECKS) ?: DEFAULT_DECKS

    fun setSelectedDecks(context: Context, decks: Set<String>) {
        prefs(context).edit().putStringSet(KEY_SELECTED_DECKS, decks).apply()
    }

    fun getBlockedPackages(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_BLOCKED_PACKAGES, DEFAULT_PACKAGES) ?: DEFAULT_PACKAGES

    fun setBlockedPackages(context: Context, packages: Set<String>) {
        prefs(context).edit().putStringSet(KEY_BLOCKED_PACKAGES, packages).apply()
    }

    fun isStatusNotificationEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_STATUS_NOTIFICATION_ENABLED, true)

    fun setStatusNotificationEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_STATUS_NOTIFICATION_ENABLED, enabled).apply()
    }

    fun hasSeenPermissionWizard(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PERMISSION_WIZARD_SEEN, false)

    fun setSeenPermissionWizard(context: Context, seen: Boolean) {
        prefs(context).edit().putBoolean(KEY_PERMISSION_WIZARD_SEEN, seen).apply()
    }
}
