package com.example.hf_a1.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationSet
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.example.hf_a1.MainActivity
import com.example.hf_a1.R
import com.example.hf_a1.database.AppDatabase
import com.example.hf_a1.models.LottoHistory
import com.example.hf_a1.databinding.FragmentGeneratedNumbersBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlin.random.Random
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class GeneratedNumbersFragment : Fragment() {
    private val numberSets = mutableListOf<List<Int>>()
    private lateinit var setViews: List<LinearLayout>
    private lateinit var shareButtons: List<ImageButton>
    private lateinit var adButton: Button
    private lateinit var regenerateButton: Button
    private var _binding: FragmentGeneratedNumbersBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "LottoPrefs"
    private val KEY_REMAINING_DRAWS = "remaining_draws"
    private val KEY_LAST_RESET_DATE = "last_reset_date"
    private var rewardedAd: RewardedAd? = null
    private lateinit var database: AppDatabase
    private var isAdWatched: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGeneratedNumbersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, 0)
        checkAndResetDailyDraws()
        
        // AdMob 초기화
        MobileAds.initialize(requireContext()) {}
        loadRewardedAd()
        
        setViews = listOf(
            binding.setA,
            binding.setB,
            binding.setC,
            binding.setD,
            binding.setE
        )
        
        shareButtons = listOf(
            binding.shareSetA,
            binding.shareSetB,
            binding.shareSetC,
            binding.shareSetD,
            binding.shareSetE
        )
        
        adButton = binding.adButton
        regenerateButton = binding.regenerateButton
        
        database = AppDatabase.getDatabase(requireContext())
        
        generateAndSaveNumbers()
        
        adButton.setOnClickListener {
            showRewardedAdForRemainingNumbers()
        }

        regenerateButton.setOnClickListener {
            showRewardedAd()
        }

        // 공유 버튼 클릭 리스너 설정
        shareButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                shareNumbers(index)
            }
        }

        // 하단 탭 클릭 이벤트
        binding.historyButton.setOnClickListener {
            try {
                val fragmentManager: FragmentManager = requireActivity().supportFragmentManager
                val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.fragmentContainer, HistoryFragment())
                fragmentTransaction.addToBackStack(null)
                fragmentTransaction.commit()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "히스토리 화면으로 이동할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.homeButton.setOnClickListener {
            try {
                val intent = Intent(requireContext(), MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "홈 화면으로 이동할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        binding.winningButton.setOnClickListener {
            try {
                val fragmentManager: FragmentManager = requireActivity().supportFragmentManager
                val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.fragmentContainer, WinningNumbersFragment())
                fragmentTransaction.addToBackStack(null)
                fragmentTransaction.commit()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "당첨번호 화면으로 이동할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun generateAndSaveNumbers() {
        // 기존 번호 세트 초기화
        numberSets.clear()
        
        // 새로운 번호 5세트 생성
        repeat(5) {
            val numbers = generateLottoNumbers()
            numberSets.add(numbers)
        }
        
        // UI 업데이트
        for (i in 0..2) {
            displayNumbers(setViews[i], numberSets[i])
        }
        
        // D, E 세트는 광고를 봤을 때만 표시
        for (i in 3..4) {
            if (isAdWatched) {
                displayNumbers(setViews[i], numberSets[i])
            } else {
                displayHiddenNumbers(setViews[i])
            }
        }
        
        // 광고 버튼 상태 업데이트
        adButton.visibility = if (isAdWatched) View.GONE else View.VISIBLE
        regenerateButton.visibility = if (isAdWatched) View.VISIBLE else View.GONE
        
        // 히스토리에 저장
        saveGeneratedNumbers(numberSets, isAdWatched)
    }
    
    private fun generateLottoNumbers(): List<Int> {
        val numbers = mutableListOf<Int>()
        while (numbers.size < 6) {
            val number = Random.nextInt(1, 46)
            if (!numbers.contains(number)) {
                numbers.add(number)
            }
        }
        return numbers.sorted()
    }
    
    private fun getNumberBackground(number: Int): Int {
        return when (number) {
            in 1..10 -> R.drawable.circle_1_10
            in 11..20 -> R.drawable.circle_11_20
            in 21..30 -> R.drawable.circle_21_30
            in 31..40 -> R.drawable.circle_31_40
            else -> R.drawable.circle_41_45
        }
    }
    
    private fun createNumberView(number: Int): TextView {
        return TextView(requireContext()).apply {
            text = String.format("%02d", number)
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(2f, 1f, 1f, ContextCompat.getColor(context, android.R.color.black))
            background = ContextCompat.getDrawable(context, getNumberBackground(number))
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.lotto_ball_size),
                resources.getDimensionPixelSize(R.dimen.lotto_ball_size)
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.lotto_ball_margin)
                marginEnd = resources.getDimensionPixelSize(R.dimen.lotto_ball_margin)
            }
        }
    }

    private fun createHiddenNumberView(): TextView {
        return TextView(requireContext()).apply {
            text = "?"
            textSize = 16f
            setTextColor(ContextCompat.getColor(context, android.R.color.white))
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(2f, 1f, 1f, ContextCompat.getColor(context, android.R.color.black))
            background = ContextCompat.getDrawable(context, R.drawable.circle_hidden)
            gravity = android.view.Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.lotto_ball_size),
                resources.getDimensionPixelSize(R.dimen.lotto_ball_size)
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.lotto_ball_margin)
                marginEnd = resources.getDimensionPixelSize(R.dimen.lotto_ball_margin)
            }
        }
    }
    
    private fun displayNumbers(setView: LinearLayout, numbers: List<Int>) {
        setView.removeAllViews()
        numbers.forEach { number ->
            val numberView = createNumberView(number)
            setView.addView(numberView)
            
            // 애니메이션 적용
            val scaleAnimation = AnimationUtils.loadAnimation(context, R.anim.scale_in)
            val fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in)
            
            val animationSet = AnimationSet(true).apply {
                addAnimation(scaleAnimation)
                addAnimation(fadeInAnimation)
            }
            numberView.startAnimation(animationSet)
        }
    }
    
    private fun displayHiddenNumbers(setView: LinearLayout) {
        setView.removeAllViews()
        repeat(6) {
            val hiddenNumberView = createHiddenNumberView()
            setView.addView(hiddenNumberView)
            
            val scaleAnimation = AnimationUtils.loadAnimation(context, R.anim.scale_in)
            val fadeInAnimation = AnimationUtils.loadAnimation(context, R.anim.fade_in)
            
            val animationSet = AnimationSet(true).apply {
                addAnimation(scaleAnimation)
                addAnimation(fadeInAnimation)
            }
            hiddenNumberView.startAnimation(animationSet)
        }
    }
    
    private fun showRemainingNumbers() {
        for (i in 3..4) {
            displayNumbers(setViews[i], numberSets[i])
        }
        adButton.visibility = View.GONE
        regenerateButton.visibility = View.VISIBLE
    }

    private fun shareNumbers(setIndex: Int) {
        val numbers = numberSets[setIndex]
        val shareText = "로또 번호 ${setIndex + 1}세트: ${numbers.joinToString(", ")}"
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        
        startActivity(Intent.createChooser(shareIntent, "로또 번호 공유하기"))
        Toast.makeText(requireContext(), "로또 번호를 공유합니다", Toast.LENGTH_SHORT).show()
    }

    private fun checkAndResetDailyDraws() {
        val lastResetDate = sharedPreferences.getLong(KEY_LAST_RESET_DATE, 0)
        val currentDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        if (lastResetDate < currentDate) {
            // 새 날짜가 되면 횟수를 1로 초기화
            sharedPreferences.edit().apply {
                putInt(KEY_REMAINING_DRAWS, 1)
                putLong(KEY_LAST_RESET_DATE, currentDate)
                apply()
            }
        }
    }

    private fun canGenerateNumbers(): Boolean {
        return sharedPreferences.getInt(KEY_REMAINING_DRAWS, 1) > 0
    }

    private fun decreaseRemainingDraws() {
        val currentDraws = sharedPreferences.getInt(KEY_REMAINING_DRAWS, 1)
        sharedPreferences.edit().putInt(KEY_REMAINING_DRAWS, currentDraws - 1).apply()
    }

    private fun increaseRemainingDraws() {
        val currentDraws = sharedPreferences.getInt(KEY_REMAINING_DRAWS, 1)
        sharedPreferences.edit().putInt(KEY_REMAINING_DRAWS, currentDraws + 1).apply()
    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(requireContext(), "ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                rewardedAd = ad
            }

            override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                rewardedAd = null
            }
        })
    }

    private fun showRewardedAd() {
        val ad = rewardedAd
        if (ad != null) {
            ad.show(requireActivity()) { rewardItem ->
                // 광고 시청 완료 후 새로운 번호 생성
                generateAndSaveNumbers()
                increaseRemainingDraws()
                regenerateButton.visibility = View.GONE
                adButton.visibility = View.VISIBLE
                loadRewardedAd() // 다음 광고 로드
            }
        } else {
            Toast.makeText(requireContext(), "광고를 불러오는 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            loadRewardedAd()
        }
    }

    private fun showRewardedAdForRemainingNumbers() {
        val ad = rewardedAd
        if (ad != null) {
            ad.show(requireActivity()) { rewardItem ->
                // 광고 시청 완료 후 나머지 번호 표시
                isAdWatched = true
                showRemainingNumbers()
                // 데이터베이스 업데이트
                saveGeneratedNumbers(numberSets, true)
                loadRewardedAd() // 다음 광고 로드
            }
        } else {
            Toast.makeText(requireContext(), "광고를 불러오는 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            loadRewardedAd()
        }
    }

    private fun updateNumberSetViews() {
        // A, B, C 세트는 항상 표시
        setViews[0].visibility = View.VISIBLE
        setViews[1].visibility = View.VISIBLE
        setViews[2].visibility = View.VISIBLE

        // D, E 세트는 광고를 봤을 때만 표시
        setViews[3].visibility = if (isAdWatched) View.VISIBLE else View.GONE
        setViews[4].visibility = if (isAdWatched) View.VISIBLE else View.GONE

        // 광고 버튼 상태 업데이트
        adButton.visibility = if (isAdWatched) View.GONE else View.VISIBLE
        regenerateButton.visibility = if (isAdWatched) View.VISIBLE else View.GONE
    }

    private fun calculateLottoRound(): Int {
        val referenceDate = Calendar.getInstance().apply {
            set(2002, 11, 1) // 2002년 12월 1일 (0-based month)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val currentDate = Calendar.getInstance()
        
        // 현재 시간 로깅
        val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault())
        Log.d("LottoRound", "Reference date: ${dateFormat.format(referenceDate.time)}")
        Log.d("LottoRound", "Current time: ${dateFormat.format(currentDate.time)}")
        
        // 주 단위로 차이 계산
        val diffInMillis = currentDate.timeInMillis - referenceDate.timeInMillis
        val diffInWeeks = (diffInMillis / (7 * 24 * 60 * 60 * 1000)).toInt()
        
        // 현재 시간이 토요일 저녁 8시 45분 이후인지 확인
        val currentDayOfWeek = currentDate.get(Calendar.DAY_OF_WEEK)
        val currentHour = currentDate.get(Calendar.HOUR_OF_DAY)
        val currentMinute = currentDate.get(Calendar.MINUTE)
        
        // 계산 과정 로깅
        Log.d("LottoRound", "Day of week: $currentDayOfWeek (${if(currentDayOfWeek == Calendar.SATURDAY) "토요일" else if(currentDayOfWeek == Calendar.SUNDAY) "일요일" else "평일"})")
        Log.d("LottoRound", "Current time: $currentHour:$currentMinute")
        
        // 토요일(7) 저녁 8시 45분 이후부터는 다음 회차로 계산
        val isAfterDrawing = when {
            currentDayOfWeek > Calendar.SATURDAY -> true  // 일요일
            currentDayOfWeek < Calendar.SATURDAY -> false // 월~금요일
            else -> { // 토요일인 경우
                currentHour > 20 || (currentHour == 20 && currentMinute >= 45)
            }
        }
        
        val calculatedRound = diffInWeeks + 1 + (if (isAfterDrawing) 1 else 0)
        Log.d("LottoRound", "Weeks since start: $diffInWeeks")
        Log.d("LottoRound", "Base round (diffInWeeks + 1): ${diffInWeeks + 1}")
        Log.d("LottoRound", "Is after drawing: $isAfterDrawing")
        Log.d("LottoRound", "Final calculated round: $calculatedRound")
        
        return calculatedRound
    }

    private fun saveGeneratedNumbers(numbers: List<List<Int>>, isAdWatched: Boolean = false) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val calendar = Calendar.getInstance()
                val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                val generatedDate = dateFormat.format(calendar.time)
                
                // 현재 회차 계산
                val currentRound = calculateLottoRound()

                // 모든 세트를 세미콜론으로 구분하여 하나의 문자열로 결합
                val numbersStr = numbers.joinToString(";") { set ->
                    set.joinToString(",")
                }

                val history = LottoHistory(
                    roundNumber = currentRound,
                    numbers = numbersStr,
                    generatedDate = generatedDate,
                    isWinning = false,
                    winningRank = 0,
                    isAdWatched = isAdWatched,
                    timestamp = System.currentTimeMillis() // 타임스탬프 추가
                )

                // 데이터베이스에 저장
                database.lottoHistoryDao().insert(history)
                
                // 저장 성공 로그
                Log.d("GeneratedNumbersFragment", "Numbers saved to history: Round=$currentRound, Numbers=$numbersStr")
                
                // 저장 성공 메시지 표시
                Toast.makeText(requireContext(), "번호가 저장되었습니다", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("GeneratedNumbersFragment", "Error saving numbers", e)
                Toast.makeText(requireContext(), "번호 저장 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }
} 