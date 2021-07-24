package com.koko.smoothmedia.mediasession.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.koko.smoothmedia.MainActivity
import com.koko.smoothmedia.R
import com.koko.smoothmedia.other.Constants.ACTION_SHOW_AUDIO_FRAGMENT
import com.koko.smoothmedia.other.Constants.NOTIFICATION_CHANNEL_ID
import com.koko.smoothmedia.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.koko.smoothmedia.other.Constants.NOTIFICATION_ID
import com.koko.smoothmedia.other.SmoothNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

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
class AudioService : MediaBrowserServiceCompat() {
    val TAG = "AudiService"
    private lateinit var currentPlayer: Player

    // private lateinit var mediaSource: MusicSource
    private lateinit var notificationManager: SmoothNotificationManager
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    protected lateinit var mediaSession: MediaSessionCompat
    protected lateinit var mediaSessionConnector: MediaSessionConnector
    private var currentPlaylistItems: List<MediaMetadataCompat> = emptyList()

    private val smoothMediaAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()
    private val playerEventListener = PlayerEventListener()

    /**
     * Configure ExoPlayer to handle audio focus for us.
     * See [Player.AudioComponent.setAudioAttributes] for details.
     */
    private val exoPlayer: ExoPlayer by lazy {
        SimpleExoPlayer.Builder(this).build().apply {
            setAudioAttributes(smoothMediaAudioAttributes, true)
            setHandleAudioBecomingNoisy(true)
            addListener(playerEventListener)

        }
    }


    override fun onCreate() {
        super.onCreate()

    }

    /**
     * Returns the "root" media ID that the client should request to get the list of
     * [MediaItem]s to browse/play.
     */

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        TODO("Not yet implemented")
    }

    /**
     * Returns (via the [result] parameter) a list of [MediaItem]s that are child
     * items of the provided [parentMediaId]. See [BrowseTree] for more details on
     * how this is build/more details about the relationships.
     */
    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {

        TODO("Not yet implemented")
    }

    /**
    Cancel every task and free all the resources when the Service class is destroyed
     */
    override fun onDestroy() {
        mediaSession.apply {
            isActive = false
        }
        //cancel the coroutine
        serviceJob.cancel()
        //free exoplayer resources
        exoPlayer.removeListener(playerEventListener)
        exoPlayer.release()
    }

    /**
     * Returns a list of [MediaItem]s that match the given search query
     */
    override fun onSearch(
        query: String,
        extras: Bundle?,
        result: Result<MutableList<MediaItem>>
    ) {
        super.onSearch(query, extras, result)
    }

    /**
     * This is the code that causes SmoothMedia to stop playing when swiping the activity away from
     * recents. The choice to do this is app specific. Some apps stop playback, while others allow
     * playback to continue and allow users to stop it with the notification.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
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

    /**
     * [ExoPlayer] events listener class that handles changes in the events
     */
    private inner class PlayerEventListener : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_BUFFERING,
                Player.STATE_READY -> {
                    //display the notification
                    notificationManager.showNotificationForPlayer(currentPlayer)

                }
                else -> {
                    //hide the notification
                    notificationManager.hideNotification()

                }


            }
        }
    }

}