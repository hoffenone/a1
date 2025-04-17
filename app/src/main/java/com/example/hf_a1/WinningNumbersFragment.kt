package com.example.hf_a1

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class WinningNumbersFragment : Fragment() {
    private lateinit var drawNumberText: TextView
    private lateinit var drawDateText: TextView
    private lateinit var numberViews: List<TextView>
    private lateinit var remainingTimeText: TextView
    private lateinit var nextDrawDateText: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_winning_numbers, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 뷰 초기화
        drawNumberText = view.findViewById(R.id.drawNumberText)
        drawDateText = view.findViewById(R.id.drawDateText)
        remainingTimeText = view.findViewById(R.id.remainingTimeText)
        nextDrawDateText = view.findViewById(R.id.nextDrawDateText)

        // 당첨번호 TextView 초기화
        numberViews = listOf(
            view.findViewById(R.id.number1),
            view.findViewById(R.id.number2),
            view.findViewById(R.id.number3),
            view.findViewById(R.id.number4),
            view.findViewById(R.id.number5),
            view.findViewById(R.id.number6)
        )

        // 네비게이션 버튼 클릭 리스너 설정
        view.findViewById<View>(R.id.historyButton).setOnClickListener {
            // 히스토리 화면으로 이동
        }
        view.findViewById<View>(R.id.homeButton).setOnClickListener {
            // 홈 화면으로 이동
        }

        // 설정 버튼 클릭 리스너
        view.findViewById<ImageButton>(R.id.settingsButton).setOnClickListener {
            // 설정 화면으로 이동
        }

        // 데이터 로드 및 표시
        loadWinningNumbers()
        updateNextDrawInfo()
    }

    private fun loadWinningNumbers() {
        // TODO: API에서 최신 당첨번호 데이터를 가져오는 로직 구현
        // 임시 데이터
        val numbers = listOf(1, 13, 24, 35, 41, 45)
        displayWinningNumbers(1167, "2025/04/12", numbers)
    }

    private fun displayWinningNumbers(round: Int, date: String, numbers: List<Int>) {
        drawNumberText.text = "제 ${round}회 당첨번호"
        drawDateText.text = "추첨일 : $date"

        numbers.forEachIndexed { index, number ->
            numberViews[index].text = number.toString()
        }
    }

    private fun updateNextDrawInfo() {
        // TODO: 다음 추첨일까지 남은 시간 계산 로직 구현
        // 임시 데이터
        remainingTimeText.text = "[ 6 ]일 [ 12 ]시간 [ 30 ]분\n남았습니다."
        nextDrawDateText.text = "추첨일 : 2025/04/19"
    }
} 