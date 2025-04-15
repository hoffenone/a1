package com.example.hf_a1.viewmodel

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.hf_a1.generator.LottoGenerator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import com.example.hf_a1.model.LottoNumber

class LottoViewModel : ViewModel() {
    companion object {
        private const val PREFS_NAME = "LottoHistory"
        private const val HISTORY_KEY = "history"
        private const val LAST_GENERATION_DATE_KEY = "last_generation_date"
        private const val REMAINING_GENERATIONS_KEY = "remaining_generations"
        private const val MAX_HISTORY_SIZE = 50
    }

    private val lottoGenerator = LottoGenerator()
    private val _currentLottoSets = MutableLiveData<List<List<Int>>>()
    val currentLottoSets: LiveData<List<List<Int>>> = _currentLottoSets

    private val _historyData = MutableLiveData<List<LottoNumber>>()
    val historyData: LiveData<List<LottoNumber>> = _historyData

    private val selectedWords = mutableListOf<String>()
    private lateinit var sharedPreferences: SharedPreferences
    private val gson = Gson()
    private val historyType = object : TypeToken<List<LottoNumber>>() {}.type

    var isSentenceComplete = false
        private set

    private var isAdWatched = false

    private val _remainingGenerations = MutableLiveData<Int>().apply {
        value = 1  // 초기값 설정
    }
    val remainingGenerations: LiveData<Int> = _remainingGenerations

    // UI 상태를 저장하기 위한 변수들
    private val _uiState = MutableLiveData<UIState>()
    val uiState: LiveData<UIState> = _uiState

    data class UIState(
        val isWordGridVisible: Boolean = false,
        val isFreeTextVisible: Boolean = false,
        val isResultVisible: Boolean = false,
        val isBlurredVisible: Boolean = false,
        val isRevealButtonVisible: Boolean = false
    )

    fun initialize(context: Context) {
        if (!::sharedPreferences.isInitialized) {
            sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            loadHistory()
            checkAndUpdateDailyLimit()
        }
    }

    private fun checkAndUpdateDailyLimit() {
        val lastGenerationDate = sharedPreferences.getLong(LAST_GENERATION_DATE_KEY, 0)
        val currentDate = System.currentTimeMillis()
        val isNewDay = !isSameDay(lastGenerationDate, currentDate)

        if (isNewDay) {
            _remainingGenerations.value = 1
            sharedPreferences.edit().putInt(REMAINING_GENERATIONS_KEY, 1).apply()
        } else {
            _remainingGenerations.value = sharedPreferences.getInt(REMAINING_GENERATIONS_KEY, 1)
        }
    }

    private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun addGeneration() {
        val current = _remainingGenerations.value ?: 0
        _remainingGenerations.value = current + 1
        sharedPreferences.edit().putInt(REMAINING_GENERATIONS_KEY, current + 1).apply()
    }

    fun useGeneration(): Boolean {
        val current = _remainingGenerations.value ?: 0
        if (current <= 0) return false

        _remainingGenerations.value = current - 1
        sharedPreferences.edit()
            .putInt(REMAINING_GENERATIONS_KEY, current - 1)
            .putLong(LAST_GENERATION_DATE_KEY, System.currentTimeMillis())
            .apply()
        return true
    }

    fun generateNewSets() {
        val newSets = lottoGenerator.generateMultipleSets(5)
        _currentLottoSets.value = newSets
        val setsToSave = if (!isAdWatched) {
            newSets.take(3)
        } else {
            newSets
        }
        saveToHistory(setsToSave)
        updateUIState(
            isResultVisible = true,
            isBlurredVisible = true,
            isRevealButtonVisible = true
        )
    }

    fun generateFromText(text: String) {
        val newSets = lottoGenerator.generateMultipleSets(5)
        _currentLottoSets.value = newSets
        val setsToSave = if (!isAdWatched) {
            newSets.take(3)
        } else {
            newSets
        }
        saveToHistory(setsToSave)
        updateUIState(
            isResultVisible = true,
            isBlurredVisible = true,
            isRevealButtonVisible = true
        )
    }

    fun getVisibleLottoSets(): List<List<Int>> {
        return _currentLottoSets.value?.take(3) ?: emptyList()
    }

    fun getBlurredLottoSets(): List<List<Int>> {
        return _currentLottoSets.value?.drop(3) ?: emptyList()
    }

    fun selectWord(word: String) {
        if (isSentenceComplete) return
        selectedWords.add(word)
        if (selectedWords.size == 3) {
            generateNewSets()
            isSentenceComplete = true
        }
    }

    fun reset() {
        selectedWords.clear()
        isSentenceComplete = false
        isAdWatched = false
        // UI 상태는 리셋하지 않음
    }

    fun getCurrentSentence(): String {
        return selectedWords.joinToString(" ")
    }

    fun loadHistory(): List<LottoNumber> {
        if (!::sharedPreferences.isInitialized) {
            Log.w("LottoViewModel", "SharedPreferences가 초기화되지 않았습니다")
            return emptyList()
        }
        
        return try {
            val historyJson = sharedPreferences.getString(HISTORY_KEY, "[]")
            val history = gson.fromJson<List<LottoNumber>>(historyJson, historyType) ?: emptyList()
            _historyData.value = history
            history
        } catch (e: Exception) {
            Log.e("LottoViewModel", "History 로딩 실패", e)
            emptyList()
        }
    }

    fun saveToHistory(sets: List<List<Int>>) {
        if (!::sharedPreferences.isInitialized) {
            Log.w("LottoViewModel", "SharedPreferences가 초기화되지 않았습니다")
            return
        }
        
        try {
            val history = loadHistory().toMutableList()
            val newEntries = sets.map { LottoNumber(it) }
            history.addAll(newEntries)
            
            val recentHistory = if (history.size > MAX_HISTORY_SIZE) {
                history.takeLast(MAX_HISTORY_SIZE)
            } else {
                history
            }
            
            val historyJson = gson.toJson(recentHistory)
            sharedPreferences.edit().putString(HISTORY_KEY, historyJson).apply()
            _historyData.value = recentHistory
        } catch (e: Exception) {
            Log.e("LottoViewModel", "History 저장 실패", e)
        }
    }

    fun setAdWatched(watched: Boolean) {
        isAdWatched = watched
        // 광고를 봤다면 현재 세트의 나머지 번호도 히스토리에 추가
        if (watched) {
            _currentLottoSets.value?.let { currentSets ->
                val remainingSets = currentSets.drop(3)
                if (remainingSets.isNotEmpty()) {
                    saveToHistory(remainingSets)
                }
            }
        }
    }

    fun clearHistory() {
        try {
            if (!::sharedPreferences.isInitialized) {
                Log.w("LottoViewModel", "SharedPreferences가 초기화되지 않았습니다")
                return
            }
            
            sharedPreferences.edit().putString(HISTORY_KEY, "[]").apply()
            _historyData.value = emptyList()
            Log.d("LottoViewModel", "히스토리 비우기 성공")
        } catch (e: Exception) {
            Log.e("LottoViewModel", "히스토리 비우기 실패", e)
        }
    }

    fun updateUIState(
        isWordGridVisible: Boolean = false,
        isFreeTextVisible: Boolean = false,
        isResultVisible: Boolean = false,
        isBlurredVisible: Boolean = false,
        isRevealButtonVisible: Boolean = false
    ) {
        _uiState.value = UIState(
            isWordGridVisible = isWordGridVisible,
            isFreeTextVisible = isFreeTextVisible,
            isResultVisible = isResultVisible,
            isBlurredVisible = isBlurredVisible,
            isRevealButtonVisible = isRevealButtonVisible
        )
    }

    fun updateWinningNumbers(winningNumbers: List<Int>, bonusNumber: Int, drawDate: Long) {
        val history = loadHistory().toMutableList()
        
        // 각 번호의 당첨 여부 확인
        history.forEach { lottoNumber ->
            val matches = lottoNumber.numbers.intersect(winningNumbers.toSet())
            lottoNumber.matchCount = matches.size
            lottoNumber.hasBonusMatch = lottoNumber.numbers.contains(bonusNumber)
        }
        
        // 업데이트된 히스토리 저장
        val historyJson = gson.toJson(history)
        sharedPreferences.edit().putString(HISTORY_KEY, historyJson).apply()
        _historyData.value = history
    }
} 