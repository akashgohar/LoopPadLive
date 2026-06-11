package com.looppadlive.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.looppadlive.MainActivity

class AudioPlaybackService : Service() {

    companion object {
        private const val CHANNEL_ID = "looppad_playback"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.looppadlive.STOP"
    }

    inner class LocalBinder : Binder() {
        fun getService(): AudioPlaybackService = this@AudioPlaybackService
    }

    private val binder = LocalBinder()
    lateinit var audioEngine: AudioEngine
        private set

    override fun onCreate() {
        super.onCreate()
        audioEngine = AudioEngine(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                audioEngine.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            else -> startForeground(NOTIFICATION_ID, buildNotification())
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        audioEngine.release()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "LoopPad Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "LoopPad live performance audio"
            setSound(null, null)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, AudioPlaybackService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("LoopPad Live")
            .setContentText("Performance running")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopIntent)
            .setSilent(true)
            .setOngoing(true)
            .build()
    }

    fun updateNotification(sceneName: String, bpm: Int) {
        val nm = getSystemService(NotificationManager::class.java)
        val notification = buildNotification()
        nm.notify(NOTIFICATION_ID, notification)
    }
}
