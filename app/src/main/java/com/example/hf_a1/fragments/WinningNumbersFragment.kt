package com.example.hf_a1.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.hf_a1.R
import com.example.hf_a1.network.LottoService
import com.example.hf_a1.network.LottoResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

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
    
    private var currentCall: Call<LottoResponse>? = null
    private val retrofit by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl("https://www.dhlottery.co.kr")
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
        fetchLatestWinningNumbers()
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

        // Initialize number views
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
            // 메인화면으로 돌아가기
            requireActivity().supportFragmentManager.popBackStack()
        }

        winningButton.setOnClickListener {
            // TODO: Refresh winning numbers
        }
    }

    private fun fetchLatestWinningNumbers() {
        val latestRound = calculateLatestRound()
        Log.d("WinningNumbers", "Fetching round: $latestRound")
        
        currentCall = service.getWinningNumbers(latestRound)
        currentCall?.enqueue(object : Callback<LottoResponse> {
            override fun onResponse(call: Call<LottoResponse>, response: Response<LottoResponse>) {
                if (!isAdded) return
                
                if (response.isSuccessful) {
                    response.body()?.let { 
                        Log.d("WinningNumbers", "Success: $it")
                        updateUI(it)
                    } ?: run {
                        Log.e("WinningNumbers", "Empty response body")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("WinningNumbers", "Error: ${response.code()}, $errorBody")
                }
            }

            override fun onFailure(call: Call<LottoResponse>, t: Throwable) {
                if (!isAdded) return
                
                if (!call.isCanceled) {
                    Log.e("WinningNumbers", "Network failure", t)
                }
            }
        })
    }

    private fun updateUI(response: LottoResponse) {
        drawNumberText.text = "제 ${response.drwNo}회 당첨번호"
        drawDateText.text = "추첨일 : ${response.drwNoDate}"
        
        val numbers = listOf(
            response.drwtNo1, response.drwtNo2, response.drwtNo3,
            response.drwtNo4, response.drwtNo5, response.drwtNo6
        ).sorted()
        
        numbers.forEachIndexed { index, number ->
            numberViews[index].apply {
                text = String.format("%02d", number)
                setBackgroundResource(getBackgroundForNumber(number))
            }
        }

        // Calculate and display next draw info
        updateNextDrawInfo(response.drwNoDate)
    }

    private fun getBackgroundForNumber(number: Int): Int {
        return when (number) {
            in 1..10 -> R.drawable.circle_1_10
            in 11..20 -> R.drawable.circle_11_20
            in 21..30 -> R.drawable.circle_21_30
            in 31..40 -> R.drawable.circle_31_40
            else -> R.drawable.circle_41_45
        }
    }

    private fun updateNextDrawInfo(lastDrawDate: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val lastDraw = sdf.parse(lastDrawDate)
        val calendar = Calendar.getInstance().apply {
            time = lastDraw
            add(Calendar.DATE, 7)  // Next draw is 7 days after
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 45)
        }

        val now = Calendar.getInstance()
        val diffInMillis = calendar.timeInMillis - now.timeInMillis
        val days = diffInMillis / (24 * 60 * 60 * 1000)
        val hours = (diffInMillis % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
        val minutes = (diffInMillis % (60 * 60 * 1000)) / (60 * 1000)

        remainingTimeText.text = "[ $days ]일 [ $hours ]시간 [ $minutes ]분\n남았습니다."
        nextDrawDateText.text = "추첨일 : ${sdf.format(calendar.time)}"
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

    override fun onDestroyView() {
        super.onDestroyView()
        currentCall?.cancel()
    }
} 