package com.ankigate

import android.Manifest
import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat

class PermissionsWizardActivity : Activity() {

    companion object {
        private const val REQ_ANKI_DB = 2001
        private const val REQ_NOTIFICATIONS = 2002
    }

    private lateinit var tvSummary: TextView

    private lateinit var tvStatusAnki: TextView
    private lateinit var tvStatusNotifications: TextView
    private lateinit var tvStatusUsage: TextView
    private lateinit var tvStatusOverlay: TextView

    private lateinit var btnGrantAnki: Button
    private lateinit var btnGrantNotifications: Button
    private lateinit var btnGrantUsage: Button
    private lateinit var btnGrantOverlay: Button
    private lateinit var btnGrantNext: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions_wizard)

        tvSummary = findViewById(R.id.tvPermissionSummary)

        tvStatusAnki = findViewById(R.id.tvStatusAnki)
        tvStatusNotifications = findViewById(R.id.tvStatusNotifications)
        tvStatusUsage = findViewById(R.id.tvStatusUsage)
        tvStatusOverlay = findViewById(R.id.tvStatusOverlay)

        btnGrantAnki = findViewById(R.id.btnGrantAnki)
        btnGrantNotifications = findViewById(R.id.btnGrantNotifications)
        btnGrantUsage = findViewById(R.id.btnGrantUsage)
        btnGrantOverlay = findViewById(R.id.btnGrantOverlay)
        btnGrantNext = findViewById(R.id.btnGrantNext)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnDone).setOnClickListener { finish() }

        btnGrantAnki.setOnClickListener {
            if (!AnkiChecker.isAnkiInstalled(this)) {
                updateStatus()
                return@setOnClickListener
            }
            ActivityCompat.requestPermissions(
                this,
                arrayOf(AnkiChecker.ANKI_DB_PERMISSION),
                REQ_ANKI_DB
            )
        }

        btnGrantNotifications.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQ_NOTIFICATIONS
                )
            } else {
                updateStatus()
            }
        }

        btnGrantUsage.setOnClickListener {
            PermissionSetup.openUsageAccessSettings(this)
        }

        btnGrantOverlay.setOnClickListener {
            PermissionSetup.openOverlaySettings(this)
        }

        btnGrantNext.setOnClickListener {
            when (PermissionSetup.nextMissingStep(this)) {
                PermissionSetup.Step.ANKI_DB -> btnGrantAnki.performClick()
                PermissionSetup.Step.NOTIFICATIONS -> btnGrantNotifications.performClick()
                PermissionSetup.Step.USAGE_ACCESS -> btnGrantUsage.performClick()
                PermissionSetup.Step.OVERLAY -> btnGrantOverlay.performClick()
                null -> updateStatus()
            }
        }

        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        updateStatus()
    }

    private fun updateStatus() {
        val ankiInstalled = AnkiChecker.isAnkiInstalled(this)
        val ankiGranted = PermissionSetup.isAnkiDbGranted(this)
        val notificationsGranted = PermissionSetup.isNotificationsGranted(this)
        val usageGranted = PermissionSetup.isUsageAccessGranted(this)
        val overlayGranted = PermissionSetup.isOverlayGranted(this)

        val grantedCount = PermissionSetup.grantedCount(this)
        val totalCount = PermissionSetup.totalCount()

        if (PermissionSetup.isAllGranted(this)) {
            tvSummary.text = "All permissions granted ($grantedCount/$totalCount)."
            tvSummary.setTextColor(getColor(R.color.positive))
            btnGrantNext.text = "All Set"
            btnGrantNext.isEnabled = false
            Prefs.setSeenPermissionWizard(this, true)
        } else {
            tvSummary.text = "Permissions granted: $grantedCount/$totalCount\nAnkiGate will not work until all required permissions are approved."
            tvSummary.setTextColor(getColor(R.color.textSecondary))
            btnGrantNext.text = "Grant Next Missing Permission"
            btnGrantNext.isEnabled = true
        }

        if (!ankiInstalled) {
            tvStatusAnki.text = "AnkiDroid not installed"
            tvStatusAnki.setTextColor(getColor(R.color.negative))
            btnGrantAnki.text = "Install AnkiDroid First"
            btnGrantAnki.isEnabled = false
            btnGrantAnki.alpha = 0.5f
        } else if (ankiGranted) {
            tvStatusAnki.text = "Granted"
            tvStatusAnki.setTextColor(getColor(R.color.positive))
            btnGrantAnki.text = "Granted"
            btnGrantAnki.isEnabled = false
            btnGrantAnki.alpha = 0.5f
        } else {
            tvStatusAnki.text = "Missing"
            tvStatusAnki.setTextColor(getColor(R.color.negative))
            btnGrantAnki.text = "Grant Permission"
            btnGrantAnki.isEnabled = true
            btnGrantAnki.alpha = 1f
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            tvStatusNotifications.text = "Not required on this Android version"
            tvStatusNotifications.setTextColor(getColor(R.color.textSecondary))
            btnGrantNotifications.text = "Not Required"
            btnGrantNotifications.isEnabled = false
            btnGrantNotifications.alpha = 0.5f
        } else if (notificationsGranted) {
            tvStatusNotifications.text = "Granted"
            tvStatusNotifications.setTextColor(getColor(R.color.positive))
            btnGrantNotifications.text = "Granted"
            btnGrantNotifications.isEnabled = false
            btnGrantNotifications.alpha = 0.5f
        } else {
            tvStatusNotifications.text = "Missing"
            tvStatusNotifications.setTextColor(getColor(R.color.negative))
            btnGrantNotifications.text = "Grant Permission"
            btnGrantNotifications.isEnabled = true
            btnGrantNotifications.alpha = 1f
        }

        if (usageGranted) {
            tvStatusUsage.text = "Granted"
            tvStatusUsage.setTextColor(getColor(R.color.positive))
            btnGrantUsage.text = "Granted"
            btnGrantUsage.isEnabled = false
            btnGrantUsage.alpha = 0.5f
        } else {
            tvStatusUsage.text = "Missing (AnkiGate will not work until approved)"
            tvStatusUsage.setTextColor(getColor(R.color.negative))
            btnGrantUsage.text = "Open Usage Access Settings"
            btnGrantUsage.isEnabled = true
            btnGrantUsage.alpha = 1f
        }

        if (overlayGranted) {
            tvStatusOverlay.text = "Granted"
            tvStatusOverlay.setTextColor(getColor(R.color.positive))
            btnGrantOverlay.text = "Granted"
            btnGrantOverlay.isEnabled = false
            btnGrantOverlay.alpha = 0.5f
        } else {
            tvStatusOverlay.text = "Missing (AnkiGate will not work until approved)"
            tvStatusOverlay.setTextColor(getColor(R.color.negative))
            btnGrantOverlay.text = "Open Overlay Settings"
            btnGrantOverlay.isEnabled = true
            btnGrantOverlay.alpha = 1f
        }
    }
}
