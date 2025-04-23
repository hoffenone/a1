package com.example.hf_a1.receivers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.hf_a1.MainActivity
import com.example.hf_a1.R

class LottoNotificationReceiver : BroadcastReceiver() {
    companion object {
        private const val CHANNEL_ID_DRAW = "draw_notification"
        private const val CHANNEL_ID_PURCHASE = "purchase_notification"
        private const val NOTIFICATION_ID_DRAW = 1001
        private const val NOTIFICATION_ID_PURCHASE = 1003
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 앱 실행을 위한 PendingIntent
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        when (intent.action) {
            "DRAW_NOTIFICATION" -> {
                val notification = NotificationCompat.Builder(context, CHANNEL_ID_DRAW)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("로또 추첨 결과")
                    .setContentText("이번 주 로또 당첨번호가 공개되었습니다.")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()

                notificationManager.notify(NOTIFICATION_ID_DRAW, notification)
            }
            "PURCHASE_NOTIFICATION" -> {
                val notification = NotificationCompat.Builder(context, CHANNEL_ID_PURCHASE)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("로또 구매 알림")
                    .setContentText("내일이 로또 추첨일입니다. 로또를 구매하셨나요?")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build()

                notificationManager.notify(NOTIFICATION_ID_PURCHASE, notification)
            }
        }
    }
} 