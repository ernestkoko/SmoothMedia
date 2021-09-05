package com.koko.smoothmedia.mediasession.services


import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.koko.smoothmedia.R
import kotlinx.coroutines.*


const val NOW_PLAYING_CHANNEL_ID = "com.example.android.smooth.media.NOW_PLAYING"
const val NOW_PLAYING_NOTIFICATION_ID = 0xb339 // Arbitrary number used to identify our notification

/**
 * A wrapper class for ExoPlayer's PlayerNotificationManager. It sets up the notification shown to
 * the user during audio playback and provides track metadata, such as track title and icon image.
 */
class SmoothNotificationManager(
    private val context: Context,
    mediaSession: MediaSessionCompat,
    notificationListener: PlayerNotificationManager.NotificationListener
) {
    private val TAG = "SmoothNotificationManager"

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val notificationManager: PlayerNotificationManager


    init {
        //val mediaController = MediaControllerCompat(context, sessionToken)


        notificationManager = PlayerNotificationManager.Builder(
            context,
            NOW_PLAYING_NOTIFICATION_ID,
            NOW_PLAYING_CHANNEL_ID,
        ).apply {
            setSmallIconResourceId(R.drawable.ic_snotification)
            setNotificationListener(notificationListener)
            setMediaDescriptionAdapter(DescriptionAdapter(mediaSession.controller))
            //if these are not set the Notification will fail with error: Bad Notification
            setChannelNameResourceId(R.string.notification_channel)
            setChannelDescriptionResourceId(R.string.notification_channel_description)
        }.build()
        //set the token to be used by the notification manager
        notificationManager.setMediaSessionToken(mediaSession.sessionToken)

        //set the features to be displayed to the user by the notification
        notificationManager.setUseFastForwardAction(true)
        notificationManager.setUseRewindAction(true)
        notificationManager.setUseNextAction(true)

        notificationManager.setUseChronometer(true)
        notificationManager.setUsePreviousActionInCompactView(true)
        notificationManager.setUseNextActionInCompactView(true)


    }

    /**
     * hide the notification when the null player is passed to the notification manager
     */
    fun hideNotification() {
        notificationManager.setPlayer(null)
    }

    /**
     * display the notification when a [Player] is passed to the notification manager
     */
    fun showNotificationForPlayer(player: Player) {
        notificationManager.setPlayer(player)

    }

    /**
     * A class that manages what is displayed on the notification[ e.g title, subtitle...]
     */
    private inner class DescriptionAdapter(private val controller: MediaControllerCompat) :
        PlayerNotificationManager.MediaDescriptionAdapter {

        var currentIconUri: Uri? = null
        var currentBitmap: Bitmap? = null


        override fun createCurrentContentIntent(player: Player): PendingIntent? =
            controller.sessionActivity

        override fun getCurrentContentText(player: Player) =
            controller.metadata.description.subtitle.toString()

        override fun getCurrentContentTitle(player: Player) =
            controller.metadata.description.title.toString()

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            val iconUri = controller.metadata.description.iconUri
            return if (currentIconUri != iconUri || currentBitmap == null) {

                /**
                 *Cache the bitmap for the current song so that successive calls to
                `getCurrentLargeIcon` don't cause the bitmap to be recreated.
                 */

                currentIconUri = iconUri
                serviceScope.launch {
                    currentBitmap = iconUri?.let {
                        resolveUriAsBitmap(it)
                    }
                    currentBitmap?.let { callback.onBitmap(it) }
                }
                null
            } else {
                currentBitmap
            }
        }

        private suspend fun resolveUriAsBitmap(uri: Uri): Bitmap? {
            return withContext(Dispatchers.IO) {
                /**
                 * Block on downloading artwork. Wrap it in try catch block in case the album art
                 *does not exist
                 */

                try {
                    Glide.with(context).applyDefaultRequestOptions(glideOptions)
                        .asBitmap()
                        .load(uri)
                        .submit(NOTIFICATION_LARGE_ICON_SIZE, NOTIFICATION_LARGE_ICON_SIZE)
                        .get()
                } catch (e: Exception) {
                    null
                }
            }
        }
    }
}

const val NOTIFICATION_LARGE_ICON_SIZE = 144 // px

private val glideOptions = RequestOptions()
    .fallback(R.drawable.exo_ic_default_album_image)
    .diskCacheStrategy(DiskCacheStrategy.DATA)

private const val MODE_READ_ONLY = "r"

