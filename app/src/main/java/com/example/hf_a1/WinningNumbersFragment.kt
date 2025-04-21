package com.example.hf_a1

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.hf_a1.network.LottoService
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

class WinningNumbersFragment : Fragment() {
    private lateinit var drawNumberText: TextView
    private lateinit var drawDateText: TextView
    private lateinit var numberViews: List<TextView>
    private lateinit var nextDrawTitle: TextView
    private lateinit var remainingTimeText: TextView
    private lateinit var nextDrawDateText: TextView
    private lateinit var settingsButton: ImageButton
    private lateinit var historyButton: View
    private lateinit var homeButton: View
    private lateinit var winningButton: View
    
    private val retrofit by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://www.dhlottery.co.kr/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    private val service by lazy { retrofit.create(LottoService::class.java) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_winning_numbers, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        setupClickListeners()
        loadWinningNumbers()
    }

    private fun initializeViews(view: View) {
        drawNumberText = view.findViewById(R.id.drawNumberText)
        drawDateText = view.findViewById(R.id.drawDateText)
        nextDrawTitle = view.findViewById(R.id.nextDrawTitle)
        remainingTimeText = view.findViewById(R.id.remainingTimeText)
        nextDrawDateText = view.findViewById(R.id.nextDrawDateText)
        settingsButton = view.findViewById(R.id.settingsButton)
        historyButton = view.findViewById(R.id.historyButton)
        homeButton = view.findViewById(R.id.homeButton)
        winningButton = view.findViewById(R.id.winningButton)

        numberViews = listOf(
            view.findViewById(R.id.number1),
            view.findViewById(R.id.number2),
            view.findViewById(R.id.number3),
            view.findViewById(R.id.number4),
            view.findViewById(R.id.number5),
            view.findViewById(R.id.number6)
        )
    }

    private fun setupClickListeners() {
        settingsButton.setOnClickListener {
            // TODO: Navigate to settings
        }

        historyButton.setOnClickListener {
            // TODO: Navigate to history
        }

        homeButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        winningButton.setOnClickListener {
            loadWinningNumbers()
        }
    }

    private fun loadWinningNumbers() {
        lifecycleScope.launch {
            try {
                Log.d("WinningNumbers", "API 호출 시작")
                val response = service.getWinningNumbers(drwNo = "1168")
                Log.d("WinningNumbers", "API 응답: $response")
                
                if (response.returnValue == "success") {
                    Log.d("WinningNumbers", "응답 성공: 회차=${response.drwNo}, 날짜=${response.drwNoDate}")
                    displayWinningNumbers(
                        response.drwNo,
                        response.drwNoDate,
                        listOf(
                            response.drwtNo1,
                            response.drwtNo2,
                            response.drwtNo3,
                            response.drwtNo4,
                            response.drwtNo5,
                            response.drwtNo6
                        )
                    )
                } else {
                    Log.e("WinningNumbers", "응답 실패: ${response.returnValue}")
                    Toast.makeText(context, "당첨번호를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("WinningNumbers", "Error loading winning numbers", e)
                Toast.makeText(context, "당첨번호를 가져오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun displayWinningNumbers(round: Int, date: String, numbers: List<Int>) {
        Log.d("WinningNumbers", "UI 업데이트: 회차=$round, 날짜=$date, 번호=$numbers")
        drawNumberText.text = "제 ${round}회 당첨번호"
        drawDateText.text = "추첨일 : $date"

        numbers.forEachIndexed { index, number ->
            numberViews[index].text = number.toString()
            Log.d("WinningNumbers", "번호 ${index + 1}: $number")
        }
    }
} 