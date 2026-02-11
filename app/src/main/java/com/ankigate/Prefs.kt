package com.ankigate

import android.content.Context
import android.content.SharedPreferences

object Prefs {

    private const val NAME = "ankigate_prefs"
    private const val KEY_SELECTED_DECKS = "selected_decks"
    private const val KEY_BLOCKED_PACKAGES = "blocked_packages"

    private val DEFAULT_DECKS = setOf("spanish")
    private val DEFAULT_PACKAGES = setOf(
        "com.instagram.android",
        "com.google.android.youtube",
        "com.twitter.android",
    )

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
}
