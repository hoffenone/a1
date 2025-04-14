package com.example.hf_a1.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.hf_a1.R
import com.example.hf_a1.viewmodel.LottoViewModel

class HistoryFragment : Fragment() {
    private lateinit var viewModel: LottoViewModel
    private lateinit var historyText: TextView
    private lateinit var clearButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[LottoViewModel::class.java]
        viewModel.initialize(requireContext())
        historyText = view.findViewById(R.id.historyText)
        clearButton = view.findViewById(R.id.btnClearHistory)
        
        setupClearButton()
        // LiveData 관찰
        viewModel.historyData.observe(viewLifecycleOwner) { history ->
            updateHistoryDisplay(history)
        }
        
        // 초기 데이터 로드
        loadHistory()
    }

    private fun loadHistory() {
        val history = viewModel.loadHistory()
        updateHistoryDisplay(history)
    }

    private fun updateHistoryDisplay(history: List<List<Int>>) {
        if (history.isEmpty()) {
            Toast.makeText(context, "저장된 번호가 없습니다.", Toast.LENGTH_SHORT).show()
            historyText.text = "저장된 번호가 없습니다."
        } else {
            val historyTextStr = history.mapIndexed { idx, set ->
                "히스토리 ${idx + 1}: ${set.joinToString(", ")}"
            }.joinToString("\n")
            historyText.text = historyTextStr
        }
    }

    private fun setupClearButton() {
        clearButton.setOnClickListener {
            showClearConfirmationDialog()
        }
    }

    private fun showClearConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("히스토리 비우기")
            .setMessage("저장된 모든 번호를 삭제하시겠습니까?")
            .setPositiveButton("확인") { _, _ ->
                viewModel.clearHistory()
                Toast.makeText(context, "히스토리가 비워졌습니다.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("취소", null)
            .show()
    }
} 