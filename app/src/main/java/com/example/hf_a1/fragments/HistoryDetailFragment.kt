package com.example.hf_a1.fragments

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.hf_a1.R
import com.example.hf_a1.databinding.FragmentHistoryDetailBinding
import com.example.hf_a1.data.AppDatabase
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class HistoryDetailFragment : Fragment() {
    private var _binding: FragmentHistoryDetailBinding? = null
    private val binding get() = _binding!!
    
    private var roundNumber: Int = 0
    private var generatedDate: String = ""
    private var numbers: List<List<Int>> = emptyList()
    private var isAdWatched: Boolean = false
    private var rewardedAd: RewardedAd? = null

    companion object {
        private const val ARG_ROUND_NUMBER = "round_number"
        private const val ARG_GENERATED_DATE = "generated_date"
        private const val ARG_NUMBERS = "numbers"
        private const val ARG_IS_AD_WATCHED = "is_ad_watched"

        fun newInstance(
            roundNumber: Int,
            generatedDate: String,
            numbers: List<List<Int>>,
            isAdWatched: Boolean
        ): HistoryDetailFragment {
            return HistoryDetailFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ROUND_NUMBER, roundNumber)
                    putString(ARG_GENERATED_DATE, generatedDate)
                    putSerializable(ARG_NUMBERS, numbers as ArrayList)
                    putBoolean(ARG_IS_AD_WATCHED, isAdWatched)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            roundNumber = it.getInt(ARG_ROUND_NUMBER)
            generatedDate = it.getString(ARG_GENERATED_DATE) ?: ""
            numbers = it.getSerializable(ARG_NUMBERS) as? List<List<Int>> ?: emptyList()
            isAdWatched = it.getBoolean(ARG_IS_AD_WATCHED)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 뒤로가기 버튼 설정
        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        setupUI()
    }

    private fun setupUI() {
        binding.roundNumberText.text = "제 ${roundNumber}회"
        
        // 날짜 포맷 설정
        val dateFormat = SimpleDateFormat("yyyy/MM/dd (E)", Locale.KOREA)
        val drawDate = SimpleDateFormat("yyyy/MM/dd", Locale.KOREA).parse(generatedDate)?.let {
            val calendar = Calendar.getInstance()
            calendar.time = it
            
            // 토요일(7)까지 남은 일수 계산
            var daysUntilSaturday = Calendar.SATURDAY - calendar.get(Calendar.DAY_OF_WEEK)
            if (daysUntilSaturday <= 0) {
                daysUntilSaturday += 7  // 이미 토요일이 지났으면 다음 주 토요일
            }
            
            calendar.add(Calendar.DATE, daysUntilSaturday)
            dateFormat.format(calendar.time)
        } ?: ""
        
        binding.generatedDateText.text = "생성일 : ${dateFormat.format(SimpleDateFormat("yyyy/MM/dd", Locale.KOREA).parse(generatedDate))}"
        binding.drawDateText.text = "추첨일 : $drawDate"
        
        setupNumbersDisplay()

        // 공유 버튼 설정
        binding.shareButton.setOnClickListener {
            captureAndShareContent()
        }

        // 광고 버튼 설정 - 생성 시점에 광고를 보지 않았을 때만 표시
        if (!isAdWatched) {
            binding.watchAdButton.visibility = View.VISIBLE
            binding.watchAdButton.setOnClickListener {
                showRewardedAd()
            }
        } else {
            binding.watchAdButton.visibility = View.GONE
        }

        // AdMob 초기화 및 광고 로드
        MobileAds.initialize(requireContext()) {}
        loadRewardedAd()
    }

    private fun setupNumbersDisplay() {
        binding.numbersContainer.removeAllViews()
        
        // 첫 3개 세트는 항상 표시
        for (i in 0..2) {
            if (i < numbers.size) {
                createNumberSetLayout(numbers[i], "세트 ${('A' + i)}")
            }
        }

        // D, E 세트는 광고 시청 여부에 따라 처리
        if (isAdWatched) {
            // 광고를 이미 시청한 경우 모든 번호 표시
            for (i in 3..4) {
                if (i < numbers.size) {
                    createNumberSetLayout(numbers[i], "세트 ${('A' + i)}")
                }
            }
            binding.watchAdButton.visibility = View.GONE
        } else {
            // 광고를 시청하지 않은 경우 물음표로 표시
            for (i in 3..4) {
                if (i < numbers.size) {
                    createHiddenNumberSetLayout("세트 ${('A' + i)}")
                }
            }
            binding.watchAdButton.visibility = View.VISIBLE
        }
    }

    private fun createNumberSetLayout(numbers: List<Int>, title: String) {
        val numberSetLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
        }

        // 세트 제목과 번호를 포함하는 레이아웃
        val setContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // 세트 제목
        val titleView = TextView(requireContext()).apply {
            text = "세트 ${title.last()}"
            textSize = 16f
            setTypeface(null, Typeface.NORMAL)
            setTextColor(resources.getColor(R.color.black, null))
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.set_title_width),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        setContainer.addView(titleView)

        // 번호들을 담을 컨테이너
        val numbersLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            weightSum = 6f
        }

        numbers.forEach { number ->
            val numberView = TextView(requireContext()).apply {
                text = String.format("%02d", number)
                textSize = 16f
                setTypeface(null, Typeface.NORMAL)
                setTextColor(resources.getColor(R.color.black, null))
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }
            numbersLayout.addView(numberView)
        }

        setContainer.addView(numbersLayout)
        numberSetLayout.addView(setContainer)
        binding.numbersContainer.addView(numberSetLayout)
    }

    private fun createHiddenNumberSetLayout(title: String) {
        val numberSetLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 24)
            }
        }

        // 세트 제목과 번호를 포함하는 레이아웃
        val setContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // 세트 제목
        val titleView = TextView(requireContext()).apply {
            text = title
            textSize = 16f
            setTypeface(null, Typeface.NORMAL)
            setTextColor(resources.getColor(R.color.black, null))
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.set_title_width),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        setContainer.addView(titleView)

        // 번호들을 담을 컨테이너
        val numbersLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            weightSum = 6f
        }

        repeat(6) {
            val numberView = TextView(requireContext()).apply {
                text = "?"
                textSize = 16f
                setTypeface(null, Typeface.NORMAL)
                setTextColor(resources.getColor(R.color.black, null))
                gravity = android.view.Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }
            numbersLayout.addView(numberView)
        }

        setContainer.addView(numbersLayout)
        numberSetLayout.addView(setContainer)
        binding.numbersContainer.addView(numberSetLayout)
    }

    private fun getNumberColor(number: Int): Int {
        return ContextCompat.getColor(requireContext(), when (number) {
            in 1..10 -> R.color.lotto_1_10
            in 11..20 -> R.color.lotto_11_20
            in 21..30 -> R.color.lotto_21_30
            in 31..40 -> R.color.lotto_31_40
            else -> R.color.lotto_41_45
        })
    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            requireContext(),
            "ca-app-pub-3940256099942544/5224354917", // 테스트 광고 ID
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    rewardedAd = null
                }
            }
        )
    }

    private fun showRewardedAd() {
        rewardedAd?.let { ad ->
            ad.show(requireActivity()) { rewardItem ->
                // 광고 시청 완료 후 처리
                isAdWatched = true
                
                // Update database using coroutines
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val db = AppDatabase.getDatabase(requireContext())
                        db.lottoHistoryDao().updateAdWatchedStatus(roundNumber, true)
                        setupNumbersDisplay()
                        Toast.makeText(requireContext(), "숫자가 공개되었습니다!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "데이터 업데이트 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                
                loadRewardedAd() // 다음 광고 로드
            }
        } ?: run {
            Toast.makeText(requireContext(), "광고를 불러오는 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            loadRewardedAd()
        }
    }

    private fun captureAndShareContent() {
        // 공유 전에 헤더 숨기기
        binding.headerLayout.visibility = View.GONE
        
        // 공유할 내용만 캡처
        val shareContent = binding.shareContentLayout
        
        // 레이아웃을 비트맵으로 변환
        shareContent.isDrawingCacheEnabled = true
        shareContent.buildDrawingCache()
        val bitmap = Bitmap.createBitmap(shareContent.drawingCache)
        shareContent.isDrawingCacheEnabled = false
        
        // 캡처 후 원래대로 복구
        binding.headerLayout.visibility = View.VISIBLE
        
        // 비트맵을 파일로 저장
        val imagesFolder = File(requireContext().cacheDir, "images")
        imagesFolder.mkdirs()
        
        val file = File(imagesFolder, "lotto_numbers_${System.currentTimeMillis()}.png")
        
        try {
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
            
            // 파일을 공유 가능한 Uri로 변환
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                file
            )
            
            // 공유 인텐트 생성
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri as Parcelable)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(intent, "로또 번호 공유하기"))
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "이미지 저장 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 