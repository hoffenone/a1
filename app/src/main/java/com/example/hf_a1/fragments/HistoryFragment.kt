package com.example.hf_a1.fragments

import android.app.AlertDialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.hf_a1.R
import com.example.hf_a1.model.LottoNumber
import com.example.hf_a1.viewmodel.LottoViewModel
import java.text.SimpleDateFormat
import java.util.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import com.example.hf_a1.network.LottoService
import com.example.hf_a1.network.LottoResponse

class HistoryFragment : Fragment() {
    private lateinit var viewModel: LottoViewModel
    private lateinit var historyContainer: LinearLayout
    private lateinit var clearButton: Button
    private var latestWinningNumbers: List<Int>? = null
    private var latestBonusNumber: Int? = null
    private var latestDrawDate: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[LottoViewModel::class.java]
        viewModel.initialize(requireContext())
        
        historyContainer = view.findViewById(R.id.historyContainer)
        clearButton = view.findViewById(R.id.btnClearHistory)
        
        setupClearButton()
        fetchLatestWinningNumbers()
        
        viewModel.historyData.observe(viewLifecycleOwner) { history ->
            updateHistoryDisplay(history)
        }
        
        loadHistory()
    }

    private fun loadHistory() {
        viewModel.loadHistory()
    }

    private fun fetchLatestWinningNumbers() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://www.dhlottery.co.kr")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(LottoService::class.java)
        val latestRound = calculateLatestRound()

        service.getWinningNumbers(latestRound).enqueue(object : Callback<LottoResponse> {
            override fun onResponse(call: Call<LottoResponse>, response: Response<LottoResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { 
                        latestWinningNumbers = listOf(
                            it.drwtNo1, it.drwtNo2, it.drwtNo3,
                            it.drwtNo4, it.drwtNo5, it.drwtNo6
                        ).sorted()
                        latestBonusNumber = it.bnusNo
                        
                        // 당첨일 타임스탬프 설정 (토요일 저녁 8시 45분)
                        val drawDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .parse(it.drwNoDate)
                        val calendar = Calendar.getInstance().apply {
                            time = drawDate
                            set(Calendar.HOUR_OF_DAY, 20)
                            set(Calendar.MINUTE, 45)
                        }
                        latestDrawDate = calendar.timeInMillis
                        
                        // 히스토리 업데이트
                        viewModel.updateWinningNumbers(
                            latestWinningNumbers!!,
                            latestBonusNumber!!,
                            latestDrawDate
                        )
                    }
                }
            }

            override fun onFailure(call: Call<LottoResponse>, t: Throwable) {
                // 에러 처리
            }
        })
    }

    private fun updateHistoryDisplay(history: List<LottoNumber>) {
        historyContainer.removeAllViews()
        
        if (history.isEmpty()) {
            val emptyText = TextView(context).apply {
                text = "저장된 번호가 없습니다."
                textSize = 16f
                setPadding(16, 16, 16, 16)
            }
            historyContainer.addView(emptyText)
            return
        }

        // 날짜별로 그룹화
        val groupedHistory = history.groupBy { number ->
            getDateString(number.timestamp)
        }

        // 날짜별로 정렬 (최신 날짜가 위로)
        groupedHistory.entries.sortedByDescending { it.key }.forEach { (date, numbers) ->
            // 날짜 헤더 추가
            addDateHeader(date)
            
            // 해당 날짜의 번호들 추가
            numbers.forEach { number ->
                addNumberEntry(number)
            }
        }
    }

    private fun addDateHeader(date: String) {
        val dateHeader = TextView(context).apply {
            text = date
            textSize = 18f
            setTextColor(resources.getColor(R.color.purple_500, null))
            setPadding(16, 24, 16, 8)
        }
        historyContainer.addView(dateHeader)
    }

    private fun addNumberEntry(number: LottoNumber) {
        val numberView = TextView(context).apply {
            val numbersText = number.numbers.sorted().joinToString(", ")
            
            // 당첨 번호와 비교하여 스타일 적용
            when {
                number.matchCount >= 3 -> {
                    val matchText = if (number.hasBonusMatch) "보너스" else ""
                    text = "$numbersText (${number.matchCount}개 일치 $matchText)"
                    when (number.matchCount) {
                        6 -> {
                            setTextColor(resources.getColor(R.color.win_first, null))
                            setTypeface(null, Typeface.BOLD)
                        }
                        5 -> {
                            if (number.hasBonusMatch) {
                                setTextColor(resources.getColor(R.color.win_second, null))
                            } else {
                                setTextColor(resources.getColor(R.color.win_third, null))
                            }
                        }
                        4 -> setTextColor(resources.getColor(R.color.win_fourth, null))
                        3 -> setTextColor(resources.getColor(R.color.win_fifth, null))
                    }
                }
                isWithinLastWeek(number.timestamp) -> {
                    text = numbersText
                    setTextColor(resources.getColor(R.color.black, null))
                }
                else -> {
                    text = numbersText
                    setTextColor(resources.getColor(R.color.gray, null))
                }
            }
            
            textSize = 16f
            setPadding(32, 8, 16, 8)
        }
        historyContainer.addView(numberView)
    }

    private fun getDateString(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun setupClearButton() {
        clearButton.setOnClickListener {
            showClearConfirmationDialog()
        }
    }

    private fun showClearConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("히스토리 비우기")
            .setMessage("저장된 모든 번호를 삭제하시겠습니까?")
            .setPositiveButton("확인") { _, _ ->
                viewModel.clearHistory()
                Toast.makeText(context, "히스토리가 비워졌습니다.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun isWithinLastWeek(timestamp: Long): Boolean {
        val weekInMillis = 7 * 24 * 60 * 60 * 1000L
        return timestamp > (latestDrawDate - weekInMillis)
    }

    private fun calculateLatestRound(): Int {
        val firstDrawDate = Calendar.getInstance().apply {
            set(2002, Calendar.DECEMBER, 7)
        }
        val today = Calendar.getInstance()
        
        val diffInMillis = today.timeInMillis - firstDrawDate.timeInMillis
        val diffInWeeks = (diffInMillis / (7 * 24 * 60 * 60 * 1000)).toInt()
        
        return diffInWeeks + 1
    }
} 