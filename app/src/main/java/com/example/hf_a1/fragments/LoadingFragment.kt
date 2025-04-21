package com.example.hf_a1.fragments

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.hf_a1.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

class LoadingFragment : Fragment() {
    private lateinit var loadingText: TextView
    private val loadingMessages = listOf(
        "로또 기계 켜는중",
        "로또 볼 고르는중",
        "멀티탭 찾는중",
        "점심 메뉴 고르는중",
        "지갑 열어보는중",
        "BGM 트는중"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_loading, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadingText = view.findViewById(R.id.loadingText)
        startLoadingAnimation()
    }

    private fun startLoadingAnimation() {
        viewLifecycleOwner.lifecycleScope.launch {
            var totalElapsedTime = 0L
            val singleMessageDuration = 2500L // 한 메시지당 총 시간 (0.5초 + 1.5초 + 0.5초)
            val minTransitionTime = 3000L // 최소 3초 후 전환 가능
            val maxTransitionTime = (loadingMessages.size - 1) * singleMessageDuration // 마지막 메시지 전까지
            val transitionTime = Random.nextLong(minTransitionTime, maxTransitionTime)
            var shouldTransition = false

            for (message in loadingMessages) {
                if (shouldTransition) break

                // 텍스트 설정
                loadingText.text = message
                
                // 슬라이드 인 애니메이션 (0.5초)
                ObjectAnimator.ofFloat(loadingText, "translationY", -50f, 0f).apply {
                    duration = 500
                    interpolator = AccelerateDecelerateInterpolator()
                    start()
                }
                ObjectAnimator.ofFloat(loadingText, "alpha", 0f, 1f).apply {
                    duration = 500
                    start()
                }

                // 메시지 표시 (1.5초)
                delay(1500)

                // 전환 시간이 되었는지 확인
                totalElapsedTime += 2000 // 현재까지 진행된 시간 업데이트
                if (totalElapsedTime >= transitionTime) {
                    shouldTransition = true
                } else {
                    // 슬라이드 아웃 애니메이션 (0.5초)
                    ObjectAnimator.ofFloat(loadingText, "translationY", 0f, 50f).apply {
                        duration = 500
                        interpolator = AccelerateDecelerateInterpolator()
                        start()
                    }
                    ObjectAnimator.ofFloat(loadingText, "alpha", 1f, 0f).apply {
                        duration = 500
                        start()
                    }

                    delay(500) // 아웃 애니메이션 완료 대기
                    totalElapsedTime += 500
                }
            }
            
            // 결과 화면으로 전환
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, GeneratedNumbersFragment())
                .commit()
        }
    }
} 