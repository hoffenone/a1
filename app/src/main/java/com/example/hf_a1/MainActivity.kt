package com.example.hf_a1

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hf_a1.databinding.ActivityMainBinding
import com.example.hf_a1.fragments.LoadingFragment
import com.example.hf_a1.fragments.WinningNumbersFragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private val PREFS_NAME = "LottoPrefs"
    private val KEY_REMAINING_DRAWS = "remaining_draws"
    private val KEY_LAST_RESET_DATE = "last_reset_date"
    private var rewardedAd: RewardedAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences(PREFS_NAME, 0)
        checkAndResetDailyDraws()
        updateRemainingDrawsText()

        // AdMob 초기화
        MobileAds.initialize(this) {}
        loadRewardedAd()

        setupClickListeners()
        setupFragmentManager()
    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(this, "ca-app-pub-3940256099942544/5224354917", adRequest, object : RewardedAdLoadCallback() {
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
            ad.show(this) { rewardItem ->
                // 광고 시청 완료 후 생성 횟수 증가
                increaseRemainingDraws()
                updateRemainingDrawsText()
                loadRewardedAd() // 다음 광고 로드
                Toast.makeText(this, "생성 횟수가 1회 증가했습니다!", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "광고를 불러오는 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            loadRewardedAd()
        }
    }

    private fun increaseRemainingDraws() {
        val currentDraws = sharedPreferences.getInt(KEY_REMAINING_DRAWS, 1)
        sharedPreferences.edit().putInt(KEY_REMAINING_DRAWS, currentDraws + 1).apply()
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

    private fun updateRemainingDrawsText() {
        val remainingDraws = sharedPreferences.getInt(KEY_REMAINING_DRAWS, 1)
        binding.remainingDrawsText.text = "오늘 남은 횟수 : $remainingDraws 회"
    }

    override fun onResume() {
        super.onResume()
        updateRemainingDrawsText()
    }

    private fun setupClickListeners() {
        binding.settingsButton.setOnClickListener {
            // TODO: 설정 화면으로 이동
        }

        binding.adButton.setOnClickListener {
            showRewardedAd()
        }

        binding.historyButton.setOnClickListener {
            // TODO: 히스토리 화면으로 이동
        }

        binding.makeNumberButton.setOnClickListener {
            binding.fragmentContainer.visibility = View.VISIBLE
            hideMainUI()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, LoadingFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.winningButton.setOnClickListener {
            // 당첨 번호 확인 화면으로 이동
            binding.fragmentContainer.visibility = View.VISIBLE
            hideMainUI()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, WinningNumbersFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupFragmentManager() {
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                binding.fragmentContainer.visibility = View.GONE
                showMainUI()
            }
        }
    }

    private fun hideMainUI() {
        binding.lottoMachineImage.visibility = View.GONE
        binding.titleText.visibility = View.GONE
        binding.settingsButton.visibility = View.GONE
        binding.remainingDrawsText.visibility = View.GONE
        binding.adButton.visibility = View.GONE
        binding.navigationContainer.visibility = View.GONE
    }

    private fun showMainUI() {
        binding.lottoMachineImage.visibility = View.VISIBLE
        binding.titleText.visibility = View.VISIBLE
        binding.settingsButton.visibility = View.VISIBLE
        binding.remainingDrawsText.visibility = View.VISIBLE
        binding.adButton.visibility = View.VISIBLE
        binding.navigationContainer.visibility = View.VISIBLE
    }
}

