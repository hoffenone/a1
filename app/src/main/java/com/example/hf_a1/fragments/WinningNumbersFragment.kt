package com.example.hf_a1.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.hf_a1.R
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.util.*
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import android.util.Log
import com.example.hf_a1.network.LottoService
import com.example.hf_a1.network.LottoResponse

class WinningNumbersFragment : Fragment() {
    private lateinit var roundTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var numbersTextView: TextView
    private lateinit var bonusNumberTextView: TextView
    private lateinit var totalPrizeTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorView: TextView
    private lateinit var retryButton: Button
    
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
        fetchLatestWinningNumbers()
    }

    private fun initializeViews(view: View) {
        roundTextView = view.findViewById(R.id.roundTextView)
        dateTextView = view.findViewById(R.id.dateTextView)
        numbersTextView = view.findViewById(R.id.numbersTextView)
        bonusNumberTextView = view.findViewById(R.id.bonusNumberTextView)
        totalPrizeTextView = view.findViewById(R.id.totalPrizeTextView)
        progressBar = view.findViewById(R.id.progressBar)
        errorView = view.findViewById(R.id.errorView)
        retryButton = view.findViewById(R.id.retryButton)

        retryButton.setOnClickListener {
            fetchLatestWinningNumbers()
        }
    }

    private fun fetchLatestWinningNumbers() {
        showLoading()
        hideError()
        
        currentCall?.cancel()
        
        val latestRound = calculateLatestRound()
        Log.d("WinningNumbers", "Fetching round: $latestRound")
        
        currentCall = service.getWinningNumbers(latestRound)
        currentCall?.enqueue(object : Callback<LottoResponse> {
            override fun onResponse(call: Call<LottoResponse>, response: Response<LottoResponse>) {
                if (!isAdded) return
                
                hideLoading()
                if (response.isSuccessful) {
                    response.body()?.let { 
                        Log.d("WinningNumbers", "Success: $it")
                        updateUI(it)
                        showContent()
                    } ?: run {
                        Log.e("WinningNumbers", "Empty response body")
                        showError("데이터를 불러올 수 없습니다.")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e("WinningNumbers", "Error: ${response.code()}, $errorBody")
                    showError("서버 오류가 발생했습니다. (${response.code()})")
                }
            }

            override fun onFailure(call: Call<LottoResponse>, t: Throwable) {
                if (!isAdded) return
                
                hideLoading()
                if (!call.isCanceled) {
                    Log.e("WinningNumbers", "Network failure", t)
                    showError("네트워크 오류가 발생했습니다.\n${t.message}")
                }
            }
        })
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        hideContent()
        hideError()
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
    }

    private fun showContent() {
        roundTextView.visibility = View.VISIBLE
        dateTextView.visibility = View.VISIBLE
        numbersTextView.visibility = View.VISIBLE
        bonusNumberTextView.visibility = View.VISIBLE
        totalPrizeTextView.visibility = View.VISIBLE
    }

    private fun hideContent() {
        roundTextView.visibility = View.GONE
        dateTextView.visibility = View.GONE
        numbersTextView.visibility = View.GONE
        bonusNumberTextView.visibility = View.GONE
        totalPrizeTextView.visibility = View.GONE
    }

    private fun showError(message: String) {
        errorView.text = message
        errorView.visibility = View.VISIBLE
        retryButton.visibility = View.VISIBLE
        hideContent()
    }

    private fun hideError() {
        errorView.visibility = View.GONE
        retryButton.visibility = View.GONE
    }

    private fun updateUI(response: LottoResponse) {
        roundTextView.text = "${response.drwNo}회 당첨번호"
        dateTextView.text = "추첨일: ${response.drwNoDate}"
        
        val numbers = listOf(
            response.drwtNo1, response.drwtNo2, response.drwtNo3,
            response.drwtNo4, response.drwtNo5, response.drwtNo6
        ).sorted()
        
        // 번호를 원 안에 표시하는 스타일 적용
        val numbersText = numbers.joinToString("  ") { number ->
            String.format("%02d", number)
        }
        numbersTextView.text = numbersText
        
        bonusNumberTextView.text = "보너스 번호: ${response.bnusNo}"
        totalPrizeTextView.text = "1등 당첨금: ${formatPrize(response.firstWinamnt)}원"
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

    private fun formatPrize(amount: Long): String {
        return String.format("%,d", amount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentCall?.cancel() // 진행 중인 네트워크 요청 취소
    }
} 