package com.example.hf_a1

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.hf_a1.fragments.GenerateFragment
import com.example.hf_a1.fragments.HistoryFragment
import com.example.hf_a1.fragments.SettingsFragment
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.AdRequest
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.example.hf_a1.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupAds()
        setupBottomNavigation(savedInstanceState)
    }

    private fun setupAds() {
        try {
            MobileAds.initialize(this) { initializationStatus ->
                val statusMap = initializationStatus.adapterStatusMap
                for ((className, status) in statusMap) {
                    Log.d("Ads", "Adapter name: $className, Description: ${status.description}")
                }
            }

            val requestConfig = RequestConfiguration.Builder()
                .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
                .build()
            MobileAds.setRequestConfiguration(requestConfig)
        } catch (e: Exception) {
            Log.e("MainActivity", "광고 초기화 실패", e)
        }
    }

    private fun setupBottomNavigation(savedInstanceState: Bundle?) {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_generate -> replaceFragment(GenerateFragment())
                R.id.nav_history -> replaceFragment(HistoryFragment())
                R.id.nav_settings -> replaceFragment(SettingsFragment())
                else -> false
            }.let { true }
        }

        if (savedInstanceState == null) {
            replaceFragment(GenerateFragment())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        try {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.fade_in,
                    R.anim.fade_out
                )
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        } catch (e: Exception) {
            Log.e("MainActivity", "Fragment 전환 실패", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // 리소스 정리
    }
}

