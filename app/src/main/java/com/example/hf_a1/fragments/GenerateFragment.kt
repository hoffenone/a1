package com.example.hf_a1.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.hf_a1.R
import com.example.hf_a1.viewmodel.LottoViewModel
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

class GenerateFragment : Fragment() {
    private lateinit var viewModel: LottoViewModel
    private lateinit var btnToday: Button
    private lateinit var btnWord: Button
    private lateinit var btnFreeText: Button
    private lateinit var resultText: TextView
    private lateinit var blurredText: TextView
    private lateinit var btnReveal: Button
    private lateinit var wordGrid: GridLayout
    private lateinit var freeTextLayout: View
    private lateinit var inputSentence: EditText
    private lateinit var btnGenerateFromInput: Button
    private var rewardedAd: RewardedAd? = null
    private lateinit var tvRemainingGenerations: TextView
    private lateinit var btnAddGeneration: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_generate, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[LottoViewModel::class.java]
        viewModel.initialize(requireContext())
        
        initializeViews(view)
        setupObservers()
        setupClickListeners()
        loadRewardedAd()
    }

    private fun initializeViews(view: View) {
        btnToday = view.findViewById(R.id.btnToday)
        btnWord = view.findViewById(R.id.btnWordMethod)
        btnFreeText = view.findViewById(R.id.btnFreeText)
        resultText = view.findViewById(R.id.resultText)
        blurredText = view.findViewById(R.id.blurredText)
        btnReveal = view.findViewById(R.id.btnReveal)
        wordGrid = view.findViewById(R.id.wordGrid)
        freeTextLayout = view.findViewById(R.id.freeTextLayout)
        inputSentence = view.findViewById(R.id.inputSentence)
        btnGenerateFromInput = view.findViewById(R.id.btnGenerateFromInput)
        tvRemainingGenerations = view.findViewById(R.id.tvRemainingGenerations)
        btnAddGeneration = view.findViewById(R.id.btnAddGeneration)
    }

    private fun setupObservers() {
        viewModel.remainingGenerations.observe(viewLifecycleOwner) { count ->
            tvRemainingGenerations.text = "남은 생성 횟수: $count"
        }

        // UI 상태 관찰
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            wordGrid.visibility = if (state.isWordGridVisible) View.VISIBLE else View.GONE
            freeTextLayout.visibility = if (state.isFreeTextVisible) View.VISIBLE else View.GONE
            
            if (state.isResultVisible) {
                if (resultText.text.isEmpty()) {
                    displayLottoResults()
                }
            } else {
                resultText.text = ""
                blurredText.text = ""
            }

            blurredText.visibility = if (state.isBlurredVisible) View.VISIBLE else View.GONE
            btnReveal.visibility = if (state.isRevealButtonVisible) View.VISIBLE else View.GONE
        }

        // 현재 로또 번호 세트 관찰
        viewModel.currentLottoSets.observe(viewLifecycleOwner) { sets ->
            if (sets.isNotEmpty()) {
                displayLottoResults()
            }
        }
    }

    private fun setupClickListeners() {
        btnToday.setOnClickListener {
            if (viewModel.useGeneration()) {
                resetUI()
                generateLottoImmediately()
            } else {
                showNoGenerationsDialog()
            }
        }

        btnWord.setOnClickListener {
            resetUI()
            showWordSelectionUI()
        }

        btnFreeText.setOnClickListener {
            resetUI()
            showFreeTextInputUI()
        }

        btnReveal.setOnClickListener {
            if (rewardedAd != null) {
                rewardedAd?.show(requireActivity()) {
                    viewModel.setAdWatched(true)
                    displayBlurredNumbers()
                    loadRewardedAd()
                }
            } else {
                Toast.makeText(context, "광고를 아직 불러오는 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                loadRewardedAd()
            }
        }

        btnAddGeneration.setOnClickListener {
            if (rewardedAd != null) {
                rewardedAd?.show(requireActivity()) {
                    viewModel.addGeneration()
                    loadRewardedAd()
                }
            } else {
                Toast.makeText(context, "광고를 아직 불러오는 중입니다. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                loadRewardedAd()
            }
        }
    }

    private fun resetUI() {
        viewModel.updateUIState() // 모든 값이 false로 초기화
        viewModel.reset()
    }

    private fun generateLottoImmediately() {
        viewModel.generateNewSets()
        displayLottoResults()
    }

    private fun showWordSelectionUI() {
        viewModel.updateUIState(isWordGridVisible = true)
        setupWordGrid()
    }

    private fun showFreeTextInputUI() {
        viewModel.updateUIState(isFreeTextVisible = true)
        inputSentence.setText("")

        btnGenerateFromInput.setOnClickListener {
            if (!viewModel.useGeneration()) {
                showNoGenerationsDialog()
                return@setOnClickListener
            }

            val sentence = inputSentence.text.toString().trim()
            if (sentence.length < MIN_TEXT_LENGTH) {
                Toast.makeText(context, 
                    "문장을 ${MIN_TEXT_LENGTH}자 이상 입력해주세요.", 
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            viewModel.generateFromText(sentence)
            displayLottoResults()
        }
    }

    private fun displayLottoResults() {
        val visibleSets = viewModel.getVisibleLottoSets()
        val blurredSets = viewModel.getBlurredLottoSets()

        val visibleText = visibleSets.mapIndexed { idx, set ->
            "세트 ${idx + 1}: ${set.joinToString(", ")}"
        }.joinToString("\n")

        val blurredTextStr = blurredSets.mapIndexed { idx, _ ->
            "세트 ${idx + 4}: ** ** ** ** ** **"
        }.joinToString("\n")

        resultText.text = visibleText
        blurredText.text = blurredTextStr
    }

    private fun displayBlurredNumbers() {
        val blurredSets = viewModel.getBlurredLottoSets()
        val revealed = blurredSets.mapIndexed { idx, set ->
            "세트 ${idx + 4}: ${set.joinToString(", ")}"
        }.joinToString("\n")
        blurredText.text = revealed
    }

    private fun loadRewardedAd() {
        val adRequest = AdRequest.Builder().build()

        RewardedAd.load(
            requireContext(),
            AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d("GenerateFragment", "광고 로딩 성공")
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    rewardedAd = null
                    Log.e("GenerateFragment", "광고 로딩 실패: ${adError.message}")
                }
            }
        )
    }

    private fun showNoGenerationsDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("생성 횟수 부족")
            .setMessage("오늘의 생성 횟수를 모두 사용했습니다.\n광고를 시청하여 추가 횟수를 얻으시겠습니까?")
            .setPositiveButton("광고 보기") { _, _ ->
                btnAddGeneration.performClick()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun setupWordGrid() {
        wordGrid.removeAllViews()
        
        val wordList = listOf(
            "고양이가", "아침에", "버스를", "기다리며", "웃고있다",
            "사과를", "던졌다", "행운을", "만들었다", "걷고있다",
            "할머니가", "비행기를", "타고", "구름을", "가로질렀다"
        )

        for (word in wordList) {
            val button = Button(context).apply {
                text = word
                textSize = 16f
                setOnClickListener {
                    handleWordClick(word, this)
                }
            }
            val params = GridLayout.LayoutParams().apply {
                width = GridLayout.LayoutParams.WRAP_CONTENT
                height = GridLayout.LayoutParams.WRAP_CONTENT
                setMargins(12, 12, 12, 12)
            }
            wordGrid.addView(button, params)
        }
    }

    private fun handleWordClick(word: String, button: Button) {
        if (!viewModel.useGeneration()) {
            showNoGenerationsDialog()
            return
        }
        viewModel.selectWord(word)
        button.isEnabled = false
        button.alpha = 0.5f

        if (viewModel.isSentenceComplete) {
            displayLottoResults()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rewardedAd = null
        // 메모리 누수 방지
    }

    companion object {
        private const val AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val MIN_TEXT_LENGTH = 2
        private const val MAX_HISTORY_SIZE = 50
    }
} 