package com.ankigate

import android.content.Context
import android.net.Uri
import org.json.JSONArray

object AnkiChecker {

    @Volatile
    var testModeComplete: Boolean? = null  // null = normal mode, true/false = override

    private const val AUTHORITY = "com.ichi2.anki.flashcards"
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
}
