package com.ankigate

import android.content.Context
import android.content.SharedPreferences

object Prefs {

    private const val NAME = "ankigate_prefs"
    private const val KEY_SELECTED_DECKS = "selected_decks"
    private const val KEY_BLOCKED_PACKAGES = "blocked_packages"
    private const val KEY_TRIAL_START = "trial_start"
    private const val KEY_IS_PREMIUM = "is_premium"

    private const val TRIAL_DURATION_MS = 30L * 24 * 60 * 60 * 1000 // 30 days

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

    // --- Trial & Premium ---

    /** Call on first launch to record trial start. No-op if already set. */
    fun ensureTrialStarted(context: Context) {
        val p = prefs(context)
        if (p.getLong(KEY_TRIAL_START, 0L) == 0L) {
            p.edit().putLong(KEY_TRIAL_START, System.currentTimeMillis()).apply()
        }
    }

    fun getTrialStartTime(context: Context): Long =
        prefs(context).getLong(KEY_TRIAL_START, 0L)

    /** Days remaining in trial (0 if expired or not started). */
    fun trialDaysRemaining(context: Context): Int {
        val start = getTrialStartTime(context)
        if (start == 0L) return 0
        val elapsed = System.currentTimeMillis() - start
        val remaining = TRIAL_DURATION_MS - elapsed
        return if (remaining > 0) ((remaining / (24 * 60 * 60 * 1000)) + 1).toInt() else 0
    }

    fun isTrialActive(context: Context): Boolean =
        trialDaysRemaining(context) > 0

    fun isPremium(context: Context): Boolean =
        prefs(context).getBoolean(KEY_IS_PREMIUM, false)

    fun setPremium(context: Context, premium: Boolean) {
        prefs(context).edit().putBoolean(KEY_IS_PREMIUM, premium).apply()
    }

    /** Full access = trial active OR purchased premium. */
    fun hasFullAccess(context: Context): Boolean =
        isTrialActive(context) || isPremium(context)
}
