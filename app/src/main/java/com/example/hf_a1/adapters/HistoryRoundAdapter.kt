package com.example.hf_a1.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.hf_a1.R
import com.example.hf_a1.fragments.HistoryDetailFragment
import com.example.hf_a1.models.LottoHistory

class HistoryRoundAdapter(private val fragmentManager: FragmentManager) : ListAdapter<LottoHistory, HistoryRoundAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_round, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val roundNumberText: TextView = itemView.findViewById(R.id.roundNumberText)
        private val generatedDateText: TextView = itemView.findViewById(R.id.generatedDateText)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    val numbers = item.numbers.split(";").map { set ->
                        set.split(",").map { it.toInt() }
                    }
                    HistoryDetailFragment.newInstance(
                        item.roundNumber,
                        item.generatedDate,
                        numbers,
                        item.isAdWatched
                    ).also { fragment ->
                        fragmentManager.beginTransaction()
                            .replace(R.id.fragmentContainer, fragment)
                            .addToBackStack(null)
                            .commit()
                    }
                }
            }
        }

        fun bind(item: LottoHistory) {
            roundNumberText.text = "제 ${item.roundNumber}회"
            generatedDateText.text = item.generatedDate
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<LottoHistory>() {
        override fun areItemsTheSame(oldItem: LottoHistory, newItem: LottoHistory): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LottoHistory, newItem: LottoHistory): Boolean {
            return oldItem == newItem
        }
    }
} 