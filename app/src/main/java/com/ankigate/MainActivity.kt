package com.ankigate

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshStatus()
            handler.postDelayed(this, 3000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnSelectDecks).setOnClickListener {
            startActivity(Intent(this, DeckSelectionActivity::class.java))
        }

        findViewById<Button>(R.id.btnSelectApps).setOnClickListener {
            startActivity(Intent(this, AppSelectionActivity::class.java))
        }

        findViewById<Button>(R.id.btnToggleService).setOnClickListener {
            if (MonitorService.isRunning) {
                MonitorService.stop(this)
            } else {
                MonitorService.start(this)
            }
            // Small delay so the service state has time to update
            handler.postDelayed({ refreshStatus() }, 500)
        }

        MonitorService.start(this)
    }

    override fun onResume() {
        super.onResume()
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        handler.removeCallbacks(refreshRunnable)
        super.onPause()
    }

    private fun refreshStatus() {
        val selectedDecks = Prefs.getSelectedDecks(this)
        val status = AnkiChecker.getMultiDeckStatus(this, selectedDecks)
        val tvDeck = findViewById<TextView>(R.id.tvDeckStatus)
        val tvBlocked = findViewById<TextView>(R.id.tvBlockedApps)
        val btnToggle = findViewById<Button>(R.id.btnToggleService)

        // Update toggle button
        if (MonitorService.isRunning) {
            btnToggle.text = "Stop Monitoring"
            btnToggle.setBackgroundResource(R.drawable.bg_btn_danger)
        } else {
            btnToggle.text = "Start Monitoring"
            btnToggle.setBackgroundResource(R.drawable.bg_btn_accent)
        }

        if (selectedDecks.isEmpty()) {
            tvDeck.text = "No decks selected.\nTap 'Select Decks' to choose which decks to monitor."
        } else if (!status.found) {
            tvDeck.text = "Selected decks not found.\nMake sure AnkiDroid is installed and has been opened at least once."
        } else if (status.isComplete) {
            tvDeck.text = "All selected decks: COMPLETE\nAll apps are unlocked."
        } else {
            val deckList = selectedDecks.sorted().joinToString(", ")
            tvDeck.text = "Decks ($deckList): ${status.totalDue} cards due\n" +
                "(${status.newCount} new, ${status.reviewCount} review, ${status.learnCount} learning)\n" +
                "Blocked apps are LOCKED."
        }

        val blockedPackages = Prefs.getBlockedPackages(this)
        if (blockedPackages.isEmpty()) {
            tvBlocked.text = "No blocked apps selected.\nTap 'Select Blocked Apps' to choose."
        } else {
            val appNames = blockedPackages.map { pkg -> getAppLabel(pkg) }.sorted()
            tvBlocked.text = "Blocked apps:\n" + appNames.joinToString("\n") { "  - $it" }
        }
    }

    private fun getAppLabel(packageName: String): String {
        return try {
            val ai = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(ai).toString()
        } catch (e: Exception) {
            packageName
        }
    }
}
