package com.ankigate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.pm.ApplicationInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

class MonitorService : Service() {

    companion object {
        const val TAG = "AnkiGate"
        const val CHANNEL_ID = "ankigate_monitor"
        const val NOTIFICATION_ID = 1
        private const val ACTION_SYNC_NOTIFICATION_SETTING = "com.ankigate.SYNC_NOTIFICATION_SETTING"
        private const val POLL_INTERVAL_MS = 1000L

        @Volatile
        var isRunning = false
            private set

        fun start(context: Context) {
            val decision = MonitoringGate.evaluate(context)
            if (!decision.canStart) {
                Log.e(TAG, "Refusing to start monitor: ${decision.blockReason}")
                return
            }
            val intent = Intent(context, MonitorService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }

        fun syncNotificationSetting(context: Context) {
            if (!isRunning) return
            val intent = Intent(context, MonitorService::class.java).apply {
                action = ACTION_SYNC_NOTIFICATION_SETTING
            }
            context.startService(intent)
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isBlockingScreenShown = false
    private var cachedDeckComplete: Boolean? = null
    private var lastDeckCheckTime = 0L
    private val DECK_CACHE_MS = 15_000L
    private var pollCount = 0
    private var latestNotificationText = "Monitoring active"
    private var notificationVisible = false
    private var testReceiverRegistered = false

    private val pollRunnable = object : Runnable {
        override fun run() {
            try {
                checkAndBlock()
            } catch (e: Exception) {
                Log.e(TAG, "Error in checkAndBlock", e)
            }
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    private val testReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "com.ankigate.TEST_COMPLETE" -> {
                    AnkiChecker.testModeComplete = true
                    cachedDeckComplete = null
                    lastDeckCheckTime = 0
                    Log.e(TAG, "TEST MODE: deck set to COMPLETE")
                }
                "com.ankigate.TEST_INCOMPLETE" -> {
                    AnkiChecker.testModeComplete = false
                    cachedDeckComplete = null
                    lastDeckCheckTime = 0
                    Log.e(TAG, "TEST MODE: deck set to INCOMPLETE")
                }
                "com.ankigate.TEST_OFF" -> {
                    AnkiChecker.testModeComplete = null
                    cachedDeckComplete = null
                    lastDeckCheckTime = 0
                    Log.e(TAG, "TEST MODE: OFF — using real Anki data")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val createDecision = MonitoringGate.evaluate(this)
        if (!createDecision.canStart) {
            Log.e(TAG, "Stopping monitor onCreate: ${createDecision.blockReason}")
            stopSelf()
            return
        }
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(latestNotificationText))
        notificationVisible = true
        applyNotificationSetting()
        if (isDebuggableBuild()) {
            val filter = IntentFilter().apply {
                addAction("com.ankigate.TEST_COMPLETE")
                addAction("com.ankigate.TEST_INCOMPLETE")
                addAction("com.ankigate.TEST_OFF")
            }
            registerReceiver(testReceiver, filter, Context.RECEIVER_EXPORTED)
            testReceiverRegistered = true
        } else {
            AnkiChecker.testModeComplete = null
        }
        isRunning = true
        Log.e(TAG, "MonitorService onCreate")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_SYNC_NOTIFICATION_SETTING) {
            val syncDecision = MonitoringGate.evaluate(this)
            if (!syncDecision.canStart) {
                Log.e(TAG, "Stopping monitor: ${syncDecision.blockReason}")
                stopSelf()
                return START_NOT_STICKY
            }
            applyNotificationSetting()
            return START_STICKY
        }
        val startDecision = MonitoringGate.evaluate(this)
        if (!startDecision.canStart) {
            Log.e(TAG, "Stopping monitor: ${startDecision.blockReason}")
            stopSelf()
            return START_NOT_STICKY
        }
        handler.removeCallbacks(pollRunnable)
        handler.post(pollRunnable)
        isRunning = true
        Log.e(TAG, "MonitorService onStartCommand — polling started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        isRunning = false
        handler.removeCallbacks(pollRunnable)
        if (notificationVisible) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            notificationVisible = false
        }
        if (testReceiverRegistered) {
            unregisterReceiver(testReceiver)
            testReceiverRegistered = false
        }
        if (!isDebuggableBuild()) {
            AnkiChecker.testModeComplete = null
        }
        Log.e(TAG, "MonitorService destroyed")
        super.onDestroy()
    }

    private fun checkAndBlock() {
        pollCount++
        val foreground = getForegroundPackage()

        if (pollCount % 10 == 0) {
            Log.e(TAG, "Poll #$pollCount — foreground: $foreground")
        }

        // Always update deck status periodically for the notification
        val now = System.currentTimeMillis()
        if (now - lastDeckCheckTime >= DECK_CACHE_MS) {
            refreshDeckStatus()
        }

        val blockedPackages = Prefs.getBlockedPackages(this)
        if (foreground != null && foreground in blockedPackages) {
            val deckComplete = cachedDeckComplete ?: false
            if (!deckComplete) {
                Log.e(TAG, "BLOCKING $foreground — deck not complete")
                if (!isBlockingScreenShown) {
                    showBlockingScreen()
                }
            } else {
                Log.e(TAG, "ALLOWING $foreground — deck complete!")
                isBlockingScreenShown = false
            }
        } else {
            if (isBlockingScreenShown) {
                isBlockingScreenShown = false
            }
        }
    }

    private fun refreshDeckStatus() {
        val selectedDecks = Prefs.getSelectedDecks(this)
        val status = AnkiChecker.getMultiDeckStatus(this, selectedDecks)
        cachedDeckComplete = status.isComplete
        lastDeckCheckTime = System.currentTimeMillis()

        val notifText = if (selectedDecks.isEmpty()) {
            "No decks selected"
        } else if (!status.found) {
            "Selected decks not found"
        } else if (status.isComplete) {
            "Decks complete — apps unlocked"
        } else {
            "Due: ${status.totalDue} cards — apps blocked"
        }

        Log.e(TAG, "Deck status: $notifText (found=${status.found}, due=${status.totalDue})")
        updateNotification(notifText)
    }

    private fun getForegroundPackage(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        if (usm == null) {
            Log.e(TAG, "UsageStatsManager is null!")
            return null
        }
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 5000
        val events = usm.queryEvents(beginTime, endTime)
        var foregroundPkg: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                foregroundPkg = event.packageName
            }
        }
        return foregroundPkg
    }

    private fun showBlockingScreen() {
        isBlockingScreenShown = true
        val intent = Intent(this, BlockingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "AnkiGate Monitor",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows when AnkiGate is monitoring app usage"
            setShowBadge(false)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("AnkiGate")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        latestNotificationText = text
        if (!Prefs.isStatusNotificationEnabled(this)) {
            if (notificationVisible) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                notificationVisible = false
            }
            return
        }

        if (!notificationVisible) {
            startForeground(NOTIFICATION_ID, buildNotification(latestNotificationText))
            notificationVisible = true
            return
        }

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun applyNotificationSetting() {
        if (Prefs.isStatusNotificationEnabled(this)) {
            if (notificationVisible) {
                updateNotification(latestNotificationText)
            } else {
                startForeground(NOTIFICATION_ID, buildNotification(latestNotificationText))
                notificationVisible = true
            }
        } else if (notificationVisible) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            notificationVisible = false
        }
    }

    private fun isDebuggableBuild(): Boolean =
        (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
