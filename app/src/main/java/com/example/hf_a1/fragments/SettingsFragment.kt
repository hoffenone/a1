package com.example.hf_a1.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.example.hf_a1.R

class SettingsFragment : Fragment() {
    private lateinit var frequencySpinner: Spinner
    private lateinit var daySpinner: Spinner
    private lateinit var timeSpinner: Spinner
    private lateinit var btnSaveSettings: Button
    private lateinit var themeRadioGroup: RadioGroup
    private lateinit var lightTheme: RadioButton
    private lateinit var darkTheme: RadioButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        initializeViews(view)
        setupSpinners()
        loadSavedSettings()
        setupSaveButton()
        setupThemeSelection()
    }

    private fun initializeViews(view: View) {
        frequencySpinner = view.findViewById(R.id.frequencySpinner)
        daySpinner = view.findViewById(R.id.daySpinner)
        timeSpinner = view.findViewById(R.id.timeSpinner)
        btnSaveSettings = view.findViewById(R.id.btnSaveSettings)
        themeRadioGroup = view.findViewById(R.id.themeRadioGroup)
        lightTheme = view.findViewById(R.id.lightTheme)
        darkTheme = view.findViewById(R.id.darkTheme)
    }

    private fun setupSpinners() {
        // 빈도 스피너 설정
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.frequency_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            frequencySpinner.adapter = adapter
        }

        // 요일 스피너 설정
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.day_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            daySpinner.adapter = adapter
        }

        // 시간 스피너 설정
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.time_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            timeSpinner.adapter = adapter
        }

        // 빈도 선택에 따른 요일 스피너 표시/숨김 처리
        frequencySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                daySpinner.visibility = if (position == 1) View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupThemeSelection() {
        // 저장된 테마 설정 불러오기
        val sharedPrefs = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        val currentTheme = sharedPrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        
        // 라디오 버튼 초기 상태 설정
        when (currentTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> lightTheme.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> darkTheme.isChecked = true
        }

        // 테마 변경 리스너 설정
        themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.lightTheme -> {
                    saveThemeSettings(AppCompatDelegate.MODE_NIGHT_NO)
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    requireActivity().recreate()  // 액티비티 재생성
                }
                R.id.darkTheme -> {
                    saveThemeSettings(AppCompatDelegate.MODE_NIGHT_YES)
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    requireActivity().recreate()  // 액티비티 재생성
                }
            }
        }
    }

    private fun saveThemeSettings(themeMode: Int) {
        val sharedPrefs = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putInt("theme_mode", themeMode)
            apply()
        }
    }

    private fun loadSavedSettings() {
        val notificationPrefs = requireActivity().getSharedPreferences("NotificationSettings", Context.MODE_PRIVATE)
        val themePrefs = requireActivity().getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        
        // 알림 설정 불러오기
        val savedFrequency = notificationPrefs.getInt("frequency", 0)
        val savedDay = notificationPrefs.getInt("day", 0)
        val savedTime = notificationPrefs.getInt("time", 0)

        frequencySpinner.setSelection(savedFrequency)
        daySpinner.setSelection(savedDay)
        timeSpinner.setSelection(savedTime)

        // 테마 설정 불러오기
        val savedTheme = themePrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        when (savedTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> lightTheme.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> darkTheme.isChecked = true
        }
    }

    private fun setupSaveButton() {
        btnSaveSettings.setOnClickListener {
            saveSettings()
            Toast.makeText(context, "설정이 저장되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveSettings() {
        val sharedPrefs = requireActivity().getSharedPreferences("NotificationSettings", Context.MODE_PRIVATE)
        with(sharedPrefs.edit()) {
            putInt("frequency", frequencySpinner.selectedItemPosition)
            putInt("day", daySpinner.selectedItemPosition)
            putInt("time", timeSpinner.selectedItemPosition)
            apply()
        }
    }
} 