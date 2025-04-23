package com.example.hf_a1.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.hf_a1.R
import com.example.hf_a1.fragments.HistoryDetailFragment
import com.example.hf_a1.models.LottoHistory
import com.example.hf_a1.databinding.ItemHistoryBinding

class HistoryItemAdapter(private val fragmentManager: FragmentManager) : ListAdapter<LottoHistory, HistoryItemAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = getItem(position)
                    val numbersList = item.numbers.split(";").map { numberSet ->
                        numberSet.split(",").map { it.trim().toInt() }
                    }
                    val fragment = HistoryDetailFragment.newInstance(
                        item.roundNumber,
                        item.generatedDate,
                        numbersList,
                        item.isAdWatched
                    )
                    fragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            }
        }

        fun bind(history: LottoHistory) {
            binding.roundNumberText.text = "${history.roundNumber}회"
            binding.generatedDateText.text = history.generatedDate
            
            if (history.isWinning) {
                binding.resultContainer.visibility = View.VISIBLE
                binding.resultText.text = when (history.winningRank) {
                    1 -> "1등 당첨"
                    2 -> "2등 당첨"
                    3 -> "3등 당첨"
                    4 -> "4등 당첨"
                    5 -> "5등 당첨"
                    else -> "당첨"
                }
            } else {
                binding.resultContainer.visibility = View.GONE
            }
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