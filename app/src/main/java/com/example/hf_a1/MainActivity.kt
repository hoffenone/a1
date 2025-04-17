package com.example.hf_a1

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.hf_a1.databinding.ActivityMainBinding
import com.example.hf_a1.fragments.WinningNumbersFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        setupFragmentManager()
    }

    private fun setupClickListeners() {
        binding.settingsButton.setOnClickListener {
            // TODO: 설정 화면으로 이동
        }

        binding.adButton.setOnClickListener {
            // TODO: 광고 보기 기능 구현
        }

        binding.historyButton.setOnClickListener {
            // TODO: 히스토리 화면으로 이동
        }

        binding.makeNumberButton.setOnClickListener {
            // TODO: 번호 생성
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

