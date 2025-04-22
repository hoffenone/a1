package com.example.hf_a1.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LottoHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: LottoHistory)

    @Query("SELECT * FROM lotto_history ORDER BY roundNumber DESC")
    fun getAllHistory(): Flow<List<LottoHistory>>

    @Query("SELECT * FROM lotto_history WHERE roundNumber = :roundNumber")
    suspend fun getHistoryByRound(roundNumber: Int): List<LottoHistory>

    @Delete
    suspend fun delete(history: LottoHistory)

    @Query("DELETE FROM lotto_history")
    suspend fun deleteAll()

    @Query("UPDATE lotto_history SET isAdWatched = :isWatched WHERE roundNumber = :roundNumber")
    suspend fun updateAdWatchedStatus(roundNumber: Int, isWatched: Boolean)
} 