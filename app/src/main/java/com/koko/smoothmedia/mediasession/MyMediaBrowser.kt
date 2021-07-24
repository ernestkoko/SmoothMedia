package com.koko.smoothmedia.mediasession

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.MutableLiveData
import com.koko.smoothmedia.mediasession.extension.id


class MyMediaBrowser(context: Context, serviceComponentName: ComponentName) {

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)

    val isConnected = MutableLiveData<Boolean>()
        .apply { postValue(false) }
    private val mediaBrowser =
        MediaBrowserCompat(
            context,
            serviceComponentName,
            mediaBrowserConnectionCallback,
            null
        ).apply { connect() }
    val rootMediaId: String get() = mediaBrowser.root
    val playbackState = MutableLiveData<PlaybackStateCompat>()
        .apply { postValue(EMPTY_PLAYBACK_STATE) }
    val nowPlaying = MutableLiveData<MediaMetadataCompat>()
        .apply { postValue(NOTHING_PLAYING) }
    private lateinit var mediaController: MediaControllerCompat

    /**
     *
     *Subscribe to the media browser with the parent id and the [subscriptionCallback]
     * @param parentId The id of the parent media item whose list of children
     *            will be subscribed.
     * @param subscriptionCallback The callback to receive the list of children.
     */

    fun subScribe(parentId: String, subscriptionCallback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.subscribe(parentId, subscriptionCallback)
    }

    /**
     * Unsubscribe from the browser
     */
    fun unSubScribe(
        parentId: String,
        subscriptionCallback: MediaBrowserCompat.SubscriptionCallback
    ) {
        mediaBrowser.unsubscribe(parentId, subscriptionCallback)
    }

    fun sendCommand(
        command: String,
        parameters: Bundle?,
        resultCallback: (Int, Bundle?) -> Unit
    ): Boolean = if (mediaBrowser.isConnected) {
        mediaController.sendCommand(
            command,
            parameters,
            object : ResultReceiver(Handler(Looper.getMainLooper())) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    resultCallback(resultCode, resultData)

                }

            })
        true
    } else {
        false
    }

    private inner class MediaBrowserConnectionCallback(private val context: Context) :
        MediaBrowserCompat.ConnectionCallback() {

        /**
         * Invoked after [MediaBrowserCompat.connect] when the request has successfully
         * completed.
         */
        override fun onConnected() {
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            }
            isConnected.postValue(true)

        }

        /**
         * Invoked when the client is disconnected from the media browser.
         */
        override fun onConnectionSuspended() {
            isConnected.postValue(false)
        }

        /**
         * Invoked when the connection to the media browser failed.
         */
        override fun onConnectionFailed() {
            isConnected.postValue(false)
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            playbackState.postValue(state ?: EMPTY_PLAYBACK_STATE)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            nowPlaying.postValue(
                if (metadata?.id == null) {

                    NOTHING_PLAYING
                } else {
                    metadata
                }
            )
        }

        override fun onSessionDestroyed() {
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }

    /**
     * This runs the when the [MyMediaBrowser] is instantiated
     */
    companion object {
        //For singleton instantiation
        @Volatile
        private var instance: MyMediaBrowser? = null
        fun getInstance(context: Context, serviceComponentName: ComponentName) =
            instance ?: synchronized(this) {
                instance ?: MyMediaBrowser(context, serviceComponentName).also {
                    instance = it
                }
            }
    }
}

@Suppress("PropertyName")
val EMPTY_PLAYBACK_STATE: PlaybackStateCompat = PlaybackStateCompat.Builder()
    .setState(PlaybackStateCompat.STATE_NONE, 0, 0f)
    .build()

@Suppress("PropertyName")
val NOTHING_PLAYING: MediaMetadataCompat = MediaMetadataCompat.Builder()
    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, "")
    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 0)
    .build()

