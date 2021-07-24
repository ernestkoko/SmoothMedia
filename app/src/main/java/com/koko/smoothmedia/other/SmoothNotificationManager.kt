package com.koko.smoothmedia.other

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.koko.smoothmedia.other.Constants.NOTIFICATION_CHANNEL_ID
import com.koko.smoothmedia.other.Constants.NOTIFICATION_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class SmoothNotificationManager(
    private val context: Context, sessionToken: MediaSessionCompat.Token,
    notificationListener: PlayerNotificationManager.NotificationListener
) {
    private var player: Player? = null
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var notificationManager: PlayerNotificationManager
    private val platformNotificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        val mediaController = MediaControllerCompat(context, sessionToken)
        notificationManager = PlayerNotificationManager.Builder(
            context,
            NOTIFICATION_ID, NOTIFICATION_CHANNEL_ID,
            DescriptorAdapter(mediaController)
        ).build().apply {
            setMediaSessionToken(sessionToken)
        }
    }

    /**
     * [hideNotification] hides the notification manager
     */
    fun hideNotification() {
        notificationManager.setPlayer(null)
    }

    /**
     * [showNotificationForPlayer] displays the notification manager
     */
    fun showNotificationForPlayer(player: Player) {
        notificationManager.setPlayer(player)
    }

    /**
     * [DescriptorAdapter] Starts, updates and cancels a media style notification reflecting the player state. The actions
     * included in the notification can be customized along with their drawables, as described below.
     */
    private inner class DescriptorAdapter(val controllerCompat: MediaControllerCompat) :
        PlayerNotificationManager.MediaDescriptionAdapter {
        override fun getCurrentContentTitle(player: Player): CharSequence {
            return controllerCompat.metadata.description.title.toString()
        }

        override fun createCurrentContentIntent(player: Player): PendingIntent? {
            return controllerCompat.sessionActivity
        }

        override fun getCurrentContentText(player: Player): CharSequence? {
            return controllerCompat.metadata.description.subtitle.toString()
        }

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            TODO("Not yet implemented")
        }

    }
}