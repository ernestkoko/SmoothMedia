package com.koko.smoothmedia.mediasession.mediaconnection

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.media.MediaBrowserServiceCompat
import com.koko.smoothmedia.mediasession.extension.id
import com.koko.smoothmedia.mediasession.mediaconnection.MusicServiceConnection.MediaBrowserConnectionCallback

/**
 * Class that manages a connection to a [MediaBrowserServiceCompat] instance, typically a
 * [MusicService] or one of its subclasses.
 *
 * Typically it's best to construct/inject dependencies either using DI or, as UAMP does,
 * using [InjectorUtils] in the app module. There are a few difficulties for that here:
 * - [MediaBrowserCompat] is a final class, so mocking it directly is difficult.
 * - A [MediaBrowserConnectionCallback] is a parameter into the construction of
 *   a [MediaBrowserCompat], and provides callbacks to this class.
 * - [MediaBrowserCompat.ConnectionCallback.onConnected] is the best place to construct
 *   a [MediaControllerCompat] that will be used to control the [MediaSessionCompat].
 *
 *  Because of these reasons, rather than constructing additional classes, this is treated as
 *  a black box (which is why there's very little logic here).
 *
 *  This is also why the parameters to construct a [MusicServiceConnection] are simple
 *  parameters, rather than private properties. They're only required to build the
 *  [MediaBrowserConnectionCallback] and [MediaBrowserCompat] objects.
 */
private val TAG = "MusicServiceConnection"

class MusicServiceConnection(context: Context, serviceComponentName: ComponentName) {
    val isConnected = MutableLiveData<Boolean>().apply {
        postValue(false)
    }
    val rootMedia: String get() = mediaBrowser.root
    val playbackState = MutableLiveData<PlaybackStateCompat>()
        .apply { postValue(EMPTY_PLAYBACK_STATE) }
    val nowPlaying = MutableLiveData<MediaMetadataCompat>()
        .apply { postValue(NOTHING_PLAYING) }
    val transportControl: MediaControllerCompat.TransportControls
        get() = mediaController.transportControls


    /**
     * initialise the [MediaBrowserCompat.ConnectionCallback]
     */
    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)

    /**
     * initialise the [MediaBrowserCompat]
     */
    private val mediaBrowser = MediaBrowserCompat(
        context,
        serviceComponentName,
        mediaBrowserConnectionCallback, null
    ).apply {
        this.connect()
    }
    private lateinit var mediaController: MediaControllerCompat

    /**
     * [subscribe] subscribes the [mediaBrowser] using the [parentId] and subscription callback
     * [callback]
     */
    fun subscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.subscribe(parentId, callback)
    }

    /**
     * [unsubscribe] unsubscribes the [mediaBrowser]
     */
    fun unsubscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.unsubscribe(parentId, callback)
    }

    fun sendCommand(command: String, parameters: Bundle?) =
        sendCommand(command, parameters) { _, _ -> }

    fun sendCommand(
        command: String,
        parameter: Bundle?,
        resultCallback: ((Int, Bundle?) -> Unit)
    ) = if (mediaBrowser.isConnected) {
        Log.i(TAG, "onSendCommand: Called")
        mediaController.sendCommand(
            command,
            parameter,
            object : ResultReceiver(Handler(Looper.getMainLooper())) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    resultCallback(resultCode, resultData)
                }
            })
        true
    } else {
        Log.i(TAG, "onSendCommand: Called, false")
        false

    }

    private inner class MediaBrowserConnectionCallback(private val context: Context) :
        MediaBrowserCompat.ConnectionCallback() {

        /**
         * Invoked after [MediaBrowserCompat.connect] when the request has successfully
         * completed.
         */
        override fun onConnected() {
            Log.i(TAG, "MediaBrowserConnectionCallback.onConnection")
            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken)
                .apply {
                    //register the controller call back
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
            Log.i(TAG, "MediaControllerCallback.onPlaybackStateChanged: State: ${state!!}")

            playbackState.postValue(state ?: EMPTY_PLAYBACK_STATE)
        }


        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            Log.i(
                TAG,
                "MediaControllerCallback.onPlaybackStateChanged: metadata: ${metadata!!.description.title}"
            )
            // When ExoPlayer stops we will receive a callback with "empty" metadata. This is a
            // metadata object which has been instantiated with default values. The default value
            // for media ID is null so we assume that if this value is null we are not playing
            // anything.
            nowPlaying.postValue(
                if (metadata?.id == null) {
                    NOTHING_PLAYING
                } else {
                    metadata
                }
            )
        }

        override fun onQueueChanged(queue: MutableList<MediaSessionCompat.QueueItem>?) {
            super.onQueueChanged(queue)
        }

        /**
         * Normally if a [MediaBrowserServiceCompat] drops its connection the callback comes via
         * [MediaControllerCompat.Callback] (here). But since other connection status events
         * are sent to [MediaBrowserCompat.ConnectionCallback], we catch the disconnect here and
         * send it on to the other callback.
         */

        override fun onSessionDestroyed() {
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }


    }

    companion object {
        //for singleton instantiation
        @Volatile
        private var instance: MusicServiceConnection? = null
        fun getInstance(context: Context, serviceComponentName: ComponentName) =
            instance ?: synchronized(this) {
                instance ?: MusicServiceConnection(context, serviceComponentName)
                    .also { musicServiceConnection ->
                        instance = musicServiceConnection
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
