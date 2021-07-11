package com.koko.smoothmedia.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleService
import com.google.android.exoplayer2.SimpleExoPlayer
import com.koko.smoothmedia.MainActivity
import com.koko.smoothmedia.R
import com.koko.smoothmedia.other.Constants.ACTION_SHOW_AUDIO_FRAGMENT
import com.koko.smoothmedia.other.Constants.NOTIFICATION_CHANNEL_ID
import com.koko.smoothmedia.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.koko.smoothmedia.other.Constants.NOTIFICATION_ID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * While using [Service] class, the service runs on the main thread so it might freeze the ui
 * You have to explicitly create a new thread for the service to run in
 * The Android framework also provides the IntentService subclass of Service that uses a worker
 * thread to handle all of the start requests, one at a time. Using this class is not recommended
 * for new apps as it will not work well starting with Android 8 Oreo, due to the introduction of
 * Background execution limits. Moreover, it's deprecated starting with Android 11.
 * You can use JobIntentService as a replacement for IntentService that is compatible with newer
 * versions of Android.
 *
 */
class AudioService : LifecycleService() {
    val TAG = "AudiService"
    private lateinit var player: SimpleExoPlayer


    /**
     * [onStartCommand] is used to get the intent that started this service
     * The service is starting, due to a call to startService()
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val data = intent?.getStringExtra("bundle")



        //We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY
    }


    private suspend fun playMusic() {

        withContext(Dispatchers.IO) {


        }

    }

    /**
     * Called when there are clients that want to bind to this service
     * if no client return null
     */
    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    /**
     * [onDestroy] is called anytime the service is destroyed or stopped
     */
    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Service stopped")
    }

    private fun startMyForegroundService() {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false).setOngoing(true)
            .setSmallIcon(R.drawable.exo_ic_default_album_image)
            .setContentTitle("Running Smooth media").setContentText("000000000 ")
            .setContentIntent(getMainActivityPendingIntent())
        //start the foreground
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
    }

    private fun getMainActivityPendingIntent() =
        PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).also {
                it.action = ACTION_SHOW_AUDIO_FRAGMENT
            },
            FLAG_UPDATE_CURRENT
        )

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)

    }
}