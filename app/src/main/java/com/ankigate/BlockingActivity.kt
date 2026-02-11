package com.ankigate

import android.content.Intent
import android.os.Bundle
import android.app.Activity
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

class BlockingActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )

        setContentView(R.layout.activity_blocking)

        findViewById<Button>(R.id.btnOpenAnki).setOnClickListener {
            openAnkiDroid()
        }

        findViewById<Button>(R.id.btnGoHome).setOnClickListener {
            goHome()
        }

        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val status = AnkiChecker.getSpanishDeckStatus(this)
        val statusText = findViewById<TextView>(R.id.tvStatus)

        if (status.isComplete) {
            finish()
            return
        }

        if (!status.found) {
            statusText.text = "Could not read Anki deck.\nOpen AnkiDroid first, then try again."
        } else {
            statusText.text = "You have ${status.totalDue} cards due\n" +
                "(${status.newCount} new, ${status.reviewCount} review, ${status.learnCount} learning)"
        }
    }

    private fun openAnkiDroid() {
        val intent = packageManager.getLaunchIntentForPackage("com.ichi2.anki")
        if (intent != null) {
            startActivity(intent)
        }
        finish()
    }

    private fun goHome() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    @Deprecated("Use onBackPressed")
    override fun onBackPressed() {
        goHome()
    }
}
