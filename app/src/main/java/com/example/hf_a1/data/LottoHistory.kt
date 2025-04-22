package com.example.hf_a1.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lotto_history")
data class LottoHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val roundNumber: Int,
    val numbers: String,
    val generatedDate: String,
    val isWinning: Boolean,
    val winningRank: Int,
    val isAdWatched: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) 