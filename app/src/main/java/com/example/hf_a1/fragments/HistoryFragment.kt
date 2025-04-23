package com.example.hf_a1.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.hf_a1.MainActivity
import com.example.hf_a1.adapters.HistoryItemAdapter
import com.example.hf_a1.database.AppDatabase
import com.example.hf_a1.models.LottoHistory
import com.example.hf_a1.databinding.FragmentHistoryBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: HistoryItemAdapter
    private lateinit var database: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        database = AppDatabase.getDatabase(requireContext())
        setupRecyclerView()
        setupClickListeners()
        observeHistory()
    }

    private fun setupRecyclerView() {
        adapter = HistoryItemAdapter(childFragmentManager)
        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@HistoryFragment.adapter
        }
    }

    private fun setupClickListeners() {
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
                parentFragmentManager.beginTransaction()
                    .replace(com.example.hf_a1.R.id.fragmentContainer, WinningNumbersFragment())
                    .addToBackStack(null)
                    .commit()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "당첨번호 화면으로 이동할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeHistory() {
        viewLifecycleOwner.lifecycleScope.launch {
            database.lottoHistoryDao().getAllHistory().collectLatest { historyList ->
                adapter.submitList(historyList)
                binding.emptyView.visibility = if (historyList.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun deleteHistory(history: LottoHistory) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                database.lottoHistoryDao().delete(history)
                Toast.makeText(requireContext(), "히스토리가 삭제되었습니다", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "히스토리 삭제 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 