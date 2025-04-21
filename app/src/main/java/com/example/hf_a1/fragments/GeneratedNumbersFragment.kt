package com.example.hf_a1.fragments

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
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
import com.example.hf_a1.MainActivity
import com.example.hf_a1.R
import com.example.hf_a1.databinding.FragmentGeneratedNumbersBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlin.random.Random
import java.util.Calendar

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        
        generateNewNumbers()
        
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

    private fun generateNewNumbers() {
        numberSets.clear()
        repeat(5) {
            numberSets.add(generateLottoNumbers())
        }
        
        for (i in 0..2) {
            displayNumbers(setViews[i], numberSets[i])
        }
        
        for (i in 3..4) {
            displayHiddenNumbers(setViews[i])
        }
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
            textSize = 20f
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
            textSize = 20f
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
    
    private fun displayNumbers(container: LinearLayout, numbers: List<Int>) {
        container.removeAllViews()
        numbers.forEachIndexed { index, number ->
            val numberView = createNumberView(number)
            container.addView(numberView)
            
            // 애니메이션 적용
            val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
            val scaleIn = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_in)
            
            val animationSet = AnimationSet(true)
            animationSet.addAnimation(fadeIn)
            animationSet.addAnimation(scaleIn)
            
            // 각 볼마다 100ms씩 지연시켜 순차적으로 나타나도록 함
            animationSet.startOffset = (index * 100).toLong()
            
            numberView.startAnimation(animationSet)
        }
    }
    
    private fun displayHiddenNumbers(container: LinearLayout) {
        container.removeAllViews()
        repeat(6) {
            container.addView(createHiddenNumberView())
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
                generateNewNumbers()
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
                showRemainingNumbers()
                loadRewardedAd() // 다음 광고 로드
            }
        } else {
            Toast.makeText(requireContext(), "광고를 불러오는 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            loadRewardedAd()
        }
    }
} 