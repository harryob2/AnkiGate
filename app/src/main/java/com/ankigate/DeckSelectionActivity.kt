package com.ankigate

import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DeckSelectionActivity : Activity() {

    private val selected = mutableSetOf<String>()
    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView

    companion object {
        private const val REQ_ANKI_PERMISSION = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deck_selection)

        selected.addAll(Prefs.getSelectedDecks(this))
        rv = findViewById(R.id.rvDecks)
        tvEmpty = findViewById(R.id.tvEmpty)
        rv.layoutManager = LinearLayoutManager(this)
        loadDecks()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<View>(R.id.btnSave).setOnClickListener {
            Prefs.setSelectedDecks(this, selected)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadDecks()
    }

    private fun loadDecks() {
        if (!AnkiChecker.isAnkiInstalled(this)) {
            tvEmpty.text = "AnkiDroid is not installed."
            tvEmpty.visibility = View.VISIBLE
            rv.visibility = View.GONE
            return
        }

        if (!AnkiChecker.hasAnkiPermission(this)) {
            tvEmpty.text = "AnkiDroid access is required to read decks.\nAllow permission when prompted."
            tvEmpty.visibility = View.VISIBLE
            rv.visibility = View.GONE
            ActivityCompat.requestPermissions(
                this,
                arrayOf(AnkiChecker.ANKI_DB_PERMISSION),
                REQ_ANKI_PERMISSION
            )
            return
        }

        val allDecks = AnkiChecker.getAllDeckNames(this)
        if (allDecks.isEmpty()) {
            tvEmpty.text = "No decks found.\nMake sure AnkiDroid has decks and has been opened at least once."
            tvEmpty.visibility = View.VISIBLE
            rv.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rv.visibility = View.VISIBLE
            rv.adapter = DeckAdapter(allDecks)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQ_ANKI_PERMISSION) return

        val granted = grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (granted) {
            loadDecks()
        } else {
            tvEmpty.text = "AnkiDroid permission denied.\nEnable \"AnkiDroid database access\" in app permissions."
            tvEmpty.visibility = View.VISIBLE
            rv.visibility = View.GONE
        }
    }

    private fun showUpgradePrompt() {
        AlertDialog.Builder(this)
            .setTitle("Pro Feature")
            .setMessage("Monitoring multiple decks requires AnkiGate Pro. Upgrade for \$9.99 to unlock unlimited decks.")
            .setPositiveButton("OK", null)
            .show()
    }

    private inner class DeckAdapter(private val decks: List<String>) :
        RecyclerView.Adapter<DeckAdapter.VH>() {

        inner class VH(val cb: CheckBox) : RecyclerView.ViewHolder(cb)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val cb = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_deck, parent, false) as CheckBox
            return VH(cb)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val deck = decks[position]
            holder.cb.text = deck
            holder.cb.setOnCheckedChangeListener(null)
            holder.cb.isChecked = selected.any { it.equals(deck, ignoreCase = true) }
            holder.cb.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    if (!Prefs.hasFullAccess(this@DeckSelectionActivity) && selected.size >= 1) {
                        holder.cb.isChecked = false
                        showUpgradePrompt()
                        return@setOnCheckedChangeListener
                    }
                    selected.add(deck)
                } else {
                    selected.removeAll { it.equals(deck, ignoreCase = true) }
                }
            }
        }

        override fun getItemCount() = decks.size
    }
}
