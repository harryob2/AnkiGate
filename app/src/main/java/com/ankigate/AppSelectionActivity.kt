package com.ankigate

import android.app.Activity
import android.app.AlertDialog
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class AppSelectionActivity : Activity() {

    companion object {
        private val POPULAR_PACKAGES = listOf(
            "com.instagram.android",
            "com.google.android.youtube",
            "com.twitter.android",
            "com.zhiliaoapp.musically",   // TikTok
            "com.snapchat.android",
            "com.reddit.frontpage",
            "com.facebook.katana",
            "com.facebook.orca",          // Messenger
            "com.whatsapp",
            "com.discord",
            "com.linkedin.android",
        )

        private val EXCLUDED_PACKAGES = setOf(
            "com.ankigate",
            "com.ichi2.anki",
        )
    }

    private val selected = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_selection)

        selected.addAll(Prefs.getBlockedPackages(this))

        val items = buildAppList()
        val rv = findViewById<RecyclerView>(R.id.rvApps)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = AppAdapter(items)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<View>(R.id.btnSave).setOnClickListener {
            Prefs.setBlockedPackages(this, selected)
            finish()
        }
    }

    private fun showUpgradePrompt() {
        AlertDialog.Builder(this)
            .setTitle("Pro Feature")
            .setMessage("Blocking multiple apps requires AnkiGate Pro. Upgrade for \$9.99 to unlock unlimited apps.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun buildAppList(): List<ListItem> {
        val pm = packageManager
        val launchIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(launchIntent, 0)

        val installedMap = mutableMapOf<String, ResolveInfo>()
        for (ri in resolveInfos) {
            val pkg = ri.activityInfo.packageName
            if (pkg !in EXCLUDED_PACKAGES) {
                installedMap[pkg] = ri
            }
        }

        val usageMap = getUsageMap()

        val items = mutableListOf<ListItem>()

        // Popular section
        val popularInstalled = POPULAR_PACKAGES.filter { it in installedMap }
        if (popularInstalled.isNotEmpty()) {
            items.add(ListItem.Header("Popular"))
            for (pkg in popularInstalled) {
                val ri = installedMap[pkg]!!
                items.add(ListItem.App(
                    packageName = pkg,
                    label = ri.loadLabel(pm).toString(),
                    icon = ri.loadIcon(pm),
                ))
            }
        }

        // All Apps section — sorted by last used desc, then alphabetical
        val otherPackages = installedMap.keys - POPULAR_PACKAGES.toSet()
        val sorted = otherPackages.sortedWith(
            compareByDescending<String> { usageMap[it] ?: 0L }
                .thenBy { installedMap[it]!!.loadLabel(pm).toString().lowercase() }
        )

        if (sorted.isNotEmpty()) {
            items.add(ListItem.Header("All Apps"))
            for (pkg in sorted) {
                val ri = installedMap[pkg]!!
                items.add(ListItem.App(
                    packageName = pkg,
                    label = ri.loadLabel(pm).toString(),
                    icon = ri.loadIcon(pm),
                ))
            }
        }

        return items
    }

    private fun getUsageMap(): Map<String, Long> {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()
        val end = System.currentTimeMillis()
        val begin = end - 30L * 24 * 60 * 60 * 1000  // 30 days
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_MONTHLY, begin, end)
        val map = mutableMapOf<String, Long>()
        for (s in stats) {
            val existing = map[s.packageName] ?: 0L
            if (s.lastTimeUsed > existing) {
                map[s.packageName] = s.lastTimeUsed
            }
        }
        return map
    }

    sealed class ListItem {
        data class Header(val title: String) : ListItem()
        data class App(val packageName: String, val label: String, val icon: Drawable) : ListItem()
    }

    private inner class AppAdapter(private val items: List<ListItem>) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val TYPE_HEADER = 0
        private val TYPE_APP = 1

        inner class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
            val tv: TextView = view as TextView
        }

        inner class AppVH(view: View) : RecyclerView.ViewHolder(view) {
            val icon: ImageView = view.findViewById(R.id.ivIcon)
            val label: TextView = view.findViewById(R.id.tvAppName)
            val cb: CheckBox = view.findViewById(R.id.cbApp)
        }

        override fun getItemViewType(position: Int) = when (items[position]) {
            is ListItem.Header -> TYPE_HEADER
            is ListItem.App -> TYPE_APP
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            return if (viewType == TYPE_HEADER) {
                val tv = TextView(parent.context).apply {
                    textSize = 11f
                    setTextColor(0xFF6B6B6B.toInt())
                    setPadding(dp(20), dp(16), dp(20), dp(8))
                    letterSpacing = 0.1f
                    setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL))
                }
                HeaderVH(tv)
            } else {
                val view = inflater.inflate(R.layout.item_app, parent, false)
                AppVH(view)
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            when (val item = items[position]) {
                is ListItem.Header -> (holder as HeaderVH).tv.text = item.title
                is ListItem.App -> {
                    val h = holder as AppVH
                    h.icon.setImageDrawable(item.icon)
                    h.label.text = item.label
                    h.cb.isChecked = item.packageName in selected
                    h.itemView.setOnClickListener {
                        if (item.packageName in selected) {
                            selected.remove(item.packageName)
                            h.cb.isChecked = false
                        } else {
                            if (!Prefs.hasFullAccess(this@AppSelectionActivity) && selected.size >= 1) {
                                showUpgradePrompt()
                                return@setOnClickListener
                            }
                            selected.add(item.packageName)
                            h.cb.isChecked = true
                        }
                    }
                }
            }
        }

        override fun getItemCount() = items.size

        private fun dp(value: Int): Int =
            (value * resources.displayMetrics.density).toInt()
    }
}
