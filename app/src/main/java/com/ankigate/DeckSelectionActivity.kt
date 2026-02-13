package com.ankigate

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class DeckSelectionActivity : Activity() {

    private val selected = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deck_selection)

        selected.addAll(Prefs.getSelectedDecks(this))

        val allDecks = AnkiChecker.getAllDeckNames(this)
        val rv = findViewById<RecyclerView>(R.id.rvDecks)
        val tvEmpty = findViewById<TextView>(R.id.tvEmpty)

        if (allDecks.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rv.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rv.visibility = View.VISIBLE
            rv.layoutManager = LinearLayoutManager(this)
            rv.adapter = DeckAdapter(allDecks)
        }

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<View>(R.id.btnSave).setOnClickListener {
            Prefs.setSelectedDecks(this, selected)
            finish()
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
