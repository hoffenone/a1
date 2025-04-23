package com.example.hf_a1.database

import androidx.room.*
import com.example.hf_a1.models.LottoHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface LottoHistoryDao {
    @Query("SELECT * FROM lotto_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<LottoHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: LottoHistory)

    @Delete
    suspend fun delete(history: LottoHistory)

    @Query("DELETE FROM lotto_history")
    suspend fun deleteAll()

    @Query("SELECT * FROM lotto_history WHERE roundNumber = :roundNumber")
    suspend fun getHistoryByRound(roundNumber: Int): List<LottoHistory>

    @Query("UPDATE lotto_history SET isAdWatched = :isAdWatched WHERE id = :id")
    suspend fun updateAdWatchedStatus(id: Long, isAdWatched: Boolean)
} 