package com.example.hf_a1.fragments

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.hf_a1.BuildConfig
import com.example.hf_a1.R
import com.example.hf_a1.databinding.FragmentSettingsBinding
import com.example.hf_a1.receivers.LottoNotificationReceiver
import java.util.*

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var alarmManager: AlarmManager

    companion object {
        private const val PREFS_NAME = "LottoSettings"
        private const val KEY_DRAW_NOTIFICATION = "draw_notification"
        private const val KEY_WINNING_NOTIFICATION = "winning_notification"
        private const val KEY_PURCHASE_NOTIFICATION = "purchase_notification"
        
        private const val CHANNEL_ID_DRAW = "draw_notification"
        private const val CHANNEL_ID_WINNING = "winning_notification"
        private const val CHANNEL_ID_PURCHASE = "purchase_notification"
        
        private const val REQUEST_CODE_DRAW = 1001
        private const val REQUEST_CODE_WINNING = 1002
        private const val REQUEST_CODE_PURCHASE = 1003
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        
        setupNotificationChannels()
        setupUI()
        loadSettings()
    }

    private fun setupNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // 추첨 결과 알림 채널
            NotificationChannel(
                CHANNEL_ID_DRAW,
                "추첨 결과 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "매주 토요일 오후 10시에 로또 추첨 결과를 알려드립니다."
                notificationManager.createNotificationChannel(this)
            }

            // 당첨 확인 알림 채널
            NotificationChannel(
                CHANNEL_ID_WINNING,
                "당첨 확인 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "당첨번호가 공개되면 알려드립니다."
                notificationManager.createNotificationChannel(this)
            }

            // 구매 알림 채널
            NotificationChannel(
                CHANNEL_ID_PURCHASE,
                "로또 구매 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "로또 추첨 하루 전에 알려드립니다."
                notificationManager.createNotificationChannel(this)
            }
        }
    }

    private fun setupUI() {
        // 뒤로가기 버튼
        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // 알림 스위치 리스너 설정
        binding.drawResultSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(KEY_DRAW_NOTIFICATION, isChecked).apply()
            if (isChecked) {
                scheduleDrawNotification()
            } else {
                cancelDrawNotification()
            }
        }

        binding.winningCheckSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(KEY_WINNING_NOTIFICATION, isChecked).apply()
            if (isChecked) {
                scheduleWinningNotification()
            } else {
                cancelWinningNotification()
            }
        }

        binding.purchaseReminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(KEY_PURCHASE_NOTIFICATION, isChecked).apply()
            if (isChecked) {
                schedulePurchaseNotification()
            } else {
                cancelPurchaseNotification()
            }
        }

        // 앱 버전 표시
        binding.versionText.text = "버전 ${BuildConfig.VERSION_NAME}"

        // 앱 정보 관련 클릭 리스너
        binding.privacyPolicy.setOnClickListener {
            // TODO: 개인정보 처리방침 화면으로 이동
            Toast.makeText(requireContext(), "개인정보 처리방침", Toast.LENGTH_SHORT).show()
        }

        binding.termsOfService.setOnClickListener {
            // TODO: 서비스 이용약관 화면으로 이동
            Toast.makeText(requireContext(), "서비스 이용약관", Toast.LENGTH_SHORT).show()
        }

        binding.openSourceLicenses.setOnClickListener {
            // TODO: 오픈소스 라이선스 화면으로 이동
            Toast.makeText(requireContext(), "오픈소스 라이선스", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSettings() {
        binding.drawResultSwitch.isChecked = sharedPreferences.getBoolean(KEY_DRAW_NOTIFICATION, false)
        binding.winningCheckSwitch.isChecked = sharedPreferences.getBoolean(KEY_WINNING_NOTIFICATION, false)
        binding.purchaseReminderSwitch.isChecked = sharedPreferences.getBoolean(KEY_PURCHASE_NOTIFICATION, false)
    }

    private fun scheduleDrawNotification() {
        val calendar = Calendar.getInstance().apply {
            // 매주 토요일 오후 10시로 설정
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            set(Calendar.HOUR_OF_DAY, 22)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            
            // 이미 지난 시간이면 다음 주로 설정
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        val intent = Intent(requireContext(), LottoNotificationReceiver::class.java).apply {
            action = "DRAW_NOTIFICATION"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            REQUEST_CODE_DRAW,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY * 7,
            pendingIntent
        )
    }

    private fun scheduleWinningNotification() {
        // 당첨 번호가 공개되면 발송되는 알림은 서버에서 푸시 알림으로 처리
    }

    private fun schedulePurchaseNotification() {
        val calendar = Calendar.getInstance().apply {
            // 매주 금요일 오후 6시로 설정
            set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            
            // 이미 지난 시간이면 다음 주로 설정
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        val intent = Intent(requireContext(), LottoNotificationReceiver::class.java).apply {
            action = "PURCHASE_NOTIFICATION"
        }

        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            REQUEST_CODE_PURCHASE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY * 7,
            pendingIntent
        )
    }

    private fun cancelDrawNotification() {
        val intent = Intent(requireContext(), LottoNotificationReceiver::class.java).apply {
            action = "DRAW_NOTIFICATION"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            REQUEST_CODE_DRAW,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    private fun cancelWinningNotification() {
        // 서버 푸시 알림 설정 해제
    }

    private fun cancelPurchaseNotification() {
        val intent = Intent(requireContext(), LottoNotificationReceiver::class.java).apply {
            action = "PURCHASE_NOTIFICATION"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            REQUEST_CODE_PURCHASE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 