package com.ankigate

import android.app.Activity
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

        findViewById<Button>(R.id.btnStartService).setOnClickListener {
            MonitorService.start(this)
            refreshStatus()
        }

        findViewById<Button>(R.id.btnStopService).setOnClickListener {
            MonitorService.stop(this)
            refreshStatus()
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
        val status = AnkiChecker.getSpanishDeckStatus(this)
        val tvDeck = findViewById<TextView>(R.id.tvDeckStatus)
        val tvBlocked = findViewById<TextView>(R.id.tvBlockedApps)

        if (!status.found) {
            tvDeck.text = "Deck 'spanish' not found.\nMake sure AnkiDroid is installed and has been opened at least once."
        } else if (status.isComplete) {
            tvDeck.text = "Spanish deck: COMPLETE\nAll apps are unlocked."
        } else {
            tvDeck.text = "Spanish deck: ${status.totalDue} cards due\n" +
                "(${status.newCount} new, ${status.reviewCount} review, ${status.learnCount} learning)\n" +
                "Instagram, YouTube, and X are BLOCKED."
        }

        val blocked = MonitorService.BLOCKED_PACKAGES.joinToString("\n") { pkg ->
            val appName = when (pkg) {
                "com.instagram.android" -> "Instagram"
                "com.google.android.youtube" -> "YouTube"
                "com.twitter.android" -> "X (Twitter)"
                else -> pkg
            }
            "  - $appName"
        }
        tvBlocked.text = "Blocked apps:\n$blocked"
    }
}
