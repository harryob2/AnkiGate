package com.ankigate

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import org.json.JSONArray

object AnkiChecker {

    @Volatile
    var testModeComplete: Boolean? = null  // null = normal mode, true/false = override

    private const val AUTHORITY = "com.ichi2.anki.flashcards"
    private const val ANKI_PACKAGE = "com.ichi2.anki"
    const val ANKI_DB_PERMISSION = "com.ichi2.anki.permission.READ_WRITE_DATABASE"
    private val DECKS_URI: Uri = Uri.parse("content://$AUTHORITY/decks")
    private const val COL_NAME = "deck_name"
    private const val COL_COUNTS = "deck_count"

    data class DeckStatus(
        val found: Boolean,
        val learnCount: Int = 0,
        val reviewCount: Int = 0,
        val newCount: Int = 0,
    ) {
        val totalDue: Int get() = learnCount + reviewCount + newCount
        val isComplete: Boolean get() = found && totalDue == 0
    }

    fun getSpanishDeckStatus(context: Context): DeckStatus {
        testModeComplete?.let { complete ->
            return if (complete) DeckStatus(found = true, 0, 0, 0)
            else DeckStatus(found = true, 5, 10, 3)
        }
        if (!hasAnkiAccess(context)) return DeckStatus(found = false)
        return try {
            val cursor = context.contentResolver.query(
                DECKS_URI,
                arrayOf(COL_NAME, COL_COUNTS),
                null, null, null
            ) ?: return DeckStatus(found = false)

            cursor.use {
                while (it.moveToNext()) {
                    val name = it.getString(it.getColumnIndexOrThrow(COL_NAME))
                    if (name.equals("spanish", ignoreCase = true)) {
                        val countsJson = it.getString(it.getColumnIndexOrThrow(COL_COUNTS))
                        val counts = JSONArray(countsJson)
                        return DeckStatus(
                            found = true,
                            learnCount = counts.getInt(0),
                            reviewCount = counts.getInt(1),
                            newCount = counts.getInt(2),
                        )
                    }
                }
            }
            DeckStatus(found = false)
        } catch (e: Exception) {
            DeckStatus(found = false)
        }
    }

    /** Returns all deck names from AnkiDroid. */
    fun getAllDeckNames(context: Context): List<String> {
        if (!hasAnkiAccess(context)) return emptyList()
        return try {
            val cursor = context.contentResolver.query(
                DECKS_URI,
                arrayOf(COL_NAME),
                null, null, null
            ) ?: return emptyList()

            val names = mutableListOf<String>()
            cursor.use {
                while (it.moveToNext()) {
                    names.add(it.getString(it.getColumnIndexOrThrow(COL_NAME)))
                }
            }
            names.sorted()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Returns aggregated status across multiple decks.
     * Blocking applies when ANY selected deck has due cards.
     * Returns found=true if at least one selected deck was found.
     */
    fun getMultiDeckStatus(context: Context, selectedDecks: Set<String>): DeckStatus {
        if (selectedDecks.isEmpty()) return DeckStatus(found = false)
        if (!hasAnkiAccess(context)) return DeckStatus(found = false)

        testModeComplete?.let { complete ->
            return if (complete) DeckStatus(found = true, 0, 0, 0)
            else DeckStatus(found = true, 5, 10, 3)
        }

        return try {
            val cursor = context.contentResolver.query(
                DECKS_URI,
                arrayOf(COL_NAME, COL_COUNTS),
                null, null, null
            ) ?: return DeckStatus(found = false)

            var totalLearn = 0
            var totalReview = 0
            var totalNew = 0
            var anyFound = false
            val lowerSelected = selectedDecks.map { it.lowercase() }.toSet()

            cursor.use {
                while (it.moveToNext()) {
                    val name = it.getString(it.getColumnIndexOrThrow(COL_NAME))
                    if (name.lowercase() in lowerSelected) {
                        anyFound = true
                        val countsJson = it.getString(it.getColumnIndexOrThrow(COL_COUNTS))
                        val counts = JSONArray(countsJson)
                        totalLearn += counts.getInt(0)
                        totalReview += counts.getInt(1)
                        totalNew += counts.getInt(2)
                    }
                }
            }

            DeckStatus(
                found = anyFound,
                learnCount = totalLearn,
                reviewCount = totalReview,
                newCount = totalNew,
            )
        } catch (e: Exception) {
            DeckStatus(found = false)
        }
    }

    fun isAnkiInstalled(context: Context): Boolean {
        return try {
            val pm = context.packageManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(ANKI_PACKAGE, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(ANKI_PACKAGE, 0)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    fun hasAnkiPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        return ContextCompat.checkSelfPermission(
            context,
            ANKI_DB_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasAnkiAccess(context: Context): Boolean =
        isAnkiInstalled(context) && hasAnkiPermission(context)
}
