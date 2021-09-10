package com.koko.smoothmedia.mediasession.services

import android.app.*
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.metadata.Metadata
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.koko.smoothmedia.mediasession.extension.*
import com.koko.smoothmedia.mediasession.library.BrowseTree
import com.koko.smoothmedia.mediasession.library.InbuiltMusicSource
import com.koko.smoothmedia.mediasession.library.MusicSource
import com.koko.smoothmedia.mediasession.library.SMOOTH_ALBUMS_ROOT
import kotlinx.coroutines.*

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
    val TAG = "AudioService"
    //private lateinit var currentPlayer: Player

    // private lateinit var mediaSource: MusicSource
    private lateinit var notificationManager: SmoothNotificationManager

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    protected lateinit var mediaSession: MediaSessionCompat
    protected lateinit var mediaSessionConnector: MediaSessionConnector


    private var currentPlaylistItems: List<MediaMetadataCompat> = emptyList()
    private lateinit var mediaSource: MusicSource
    private lateinit var storage: PersistentStorage

    private val smoothMediaAudioAttributes = AudioAttributes.Builder()
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()
    private val playerEventListener = PlayerEventListener()
    private var isForegroundService = false

    /**
     * This must be `by lazy` because the source won't initially be ready.
     * See [MusicService.onLoadChildren] to see where it's accessed (and first
     * constructed).
     */
    private val browseTree: BrowseTree by lazy {
        BrowseTree(applicationContext, mediaSource)
    }
    private val dataSourceFactory: DefaultDataSourceFactory by lazy {
        DefaultDataSourceFactory(
            /* context= */ this,
            Util.getUserAgent(/* context= */ this, SMOOTH_USER_AGENT), /* listener= */
            null
        )
    }

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

    /**
     * Initialise the MediaSession and set up the player[exoPlayer].
     * Register the Notification manager
     */
    override fun onCreate() {
        super.onCreate()
        // Build a PendingIntent that can be used to launch the UI.
        val sessionActivityPendingIntent =
            packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
                PendingIntent.getActivity(this, 0, sessionIntent, 0)
            }
        // Create a new MediaSession.
        setupMediaSession(sessionActivityPendingIntent)
        /**
         * The notification manager will use our player and media session to decide when to post
         * notifications. When notifications are posted or removed our listener will be called, this
         * allows us to promote the service to foreground (required so that we're not killed if
         * the main UI is not visible).
         */
        notificationManager = SmoothNotificationManager(
            this,
            mediaSession,
            PlayerNotificationListener()
        )

        //setup player
        setupPlayer(exoPlayer)


        //initialise the media source
        mediaSource = InbuiltMusicSource()
        //load the songs
        serviceScope.launch {
            mediaSource.load(applicationContext)
        }



        //display the notification
        notificationManager.showNotificationForPlayer(exoPlayer)
        storage = PersistentStorage.getInstance(applicationContext)


    }

    /**
     * Set up the player and load the list of songs[MediaMetadataCompat]s onto the player
     */
    private fun setupPlayer(player: Player) {
        Log.i(TAG, "setupPlayer: Called")

        /*set the player that the connector should use. Remember the connector connects Media session
        and player together*/

        mediaSessionConnector.setPlayer(player)
//        player.prepare()
//        player.stop()

        // currentPlayer = exoPlayer
        //get the playback state
        val playbackState = player.playbackState
        if (currentPlaylistItems.isEmpty()) {
            player.stop()
            player.clearMediaItems()
        } else if (playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED) {
            preparePlaylist(
                metadataList = currentPlaylistItems,/*set the list*/
                itemToPlay = currentPlaylistItems[player.currentWindowIndex],/*get the current window*/
                playWhenReady = player.playWhenReady,
                playbackStartPositionMs = player.currentPosition
            )
        }


    }

    /**
     * Set up the Media session
     */
    private fun setupMediaSession(sessionActivityPendingIntent: PendingIntent?) {
        Log.i(TAG, "setupMediaSession: Called")
        mediaSession = MediaSessionCompat(this, TAG)
            .apply {
                setSessionActivity(sessionActivityPendingIntent)
                /* make the session active by setting this to true */
                isActive = true
                /**
                 * In order for [MediaBrowserCompat.ConnectionCallback.onConnected] to be called,
                 * a [MediaSessionCompat.Token] needs to be set on the [MediaBrowserServiceCompat].
                 *
                 * It is possible to wait to set the session token, if required for a specific use-case.
                 * However, the token *must* be set by the time [MediaBrowserServiceCompat.onGetRoot]
                 * returns, or the connection will fail silently. (The system will not even call
                 * [MediaBrowserCompat.ConnectionCallback.onConnectionFailed].)
                 */
            }
        sessionToken = mediaSession.sessionToken
        //connect the media session to the controller
        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(SmoothPlaybackPrepare())
        mediaSessionConnector.setQueueNavigator(SmoothQueueNavigator(mediaSession))
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
        Log.i(TAG, " onGetRoot: Called")
        Log.i(TAG, "$clientUid: ClientUid")
        return BrowserRoot("/", null)
    }

    /**
     * Returns (via the [result] parameter) a list of [MediaItem]s that are child
     * items of the provided [parentMediaId]. See [BrowseTree] for more details on
     * how this is build/more details about the relationships.
     */
    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaItem>>
    ) {
        /**
         * Detach this message from the current thread and allow the {@link #sendResult}
         * call to happen later.
         */
        result.detach()

        Log.i(TAG, "onLoadChildren: Called  $parentId: Root")

        val mediaItems = mutableListOf<MediaItem>()
        if (parentId == "/" || parentId == SMOOTH_ALBUMS_ROOT) {
            Log.i(TAG, "$parentId: Root(/)")
            /* Wait for the list of songs to be ready before sending it to the caller */
            mediaSource.whenReady { successful ->
                if (successful) {
                    val children = browseTree[parentId]?.map { item ->
                        MediaItem(item.description, item.flag)
                    }?.toMutableList()
                    currentPlaylistItems = browseTree[parentId]?.map {
                        it
                    }!!


                    /**
                     * Send the result back to the caller.
                     */
                    result.sendResult(children)
                }
            }
        } else {
            result.sendResult(mediaItems)
        }


    }

    /**
     * Save the recently playing song
     */
    private fun saveRecentSongToStorage() {

        // Obtain the current song details *before* saving them on a separate thread, otherwise
        // the current player may have been unloaded by the time the save routine runs.
        val description = currentPlaylistItems[exoPlayer.currentWindowIndex].description
        val position = exoPlayer.currentPosition

        serviceScope.launch {
            storage.saveRecentSong(
                description,
                position
            )
        }
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
     * Load the supplied list of songs and the song to play into the current player.
     */
    private fun preparePlaylist(
        metadataList: List<MediaMetadataCompat>,
        itemToPlay: MediaMetadataCompat?,
        playWhenReady: Boolean,
        playbackStartPositionMs: Long
    ) {
        Log.i(TAG, "preparePlaylist: Called")
        val initialWindowIndex = if (itemToPlay == null) 0 else metadataList.indexOf(itemToPlay)
        currentPlaylistItems = metadataList
        exoPlayer.playWhenReady = playWhenReady
       //build a [ConcatenatingMediaSource]
        val mediaSource = metadataList.toMediaSource(dataSourceFactory)

        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.seekTo(initialWindowIndex, playbackStartPositionMs)

    }

    /**
     * This is the code that causes SmoothMedia to stop playing when swiping the activity away from
     * recents. The choice to do this is app specific. Some apps stop playback, while others allow
     * playback to continue and allow users to stop it with the notification.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
    }

    /**
     * Listen for notification events.
     */
    private inner class PlayerNotificationListener :
        PlayerNotificationManager.NotificationListener {

        /**
         * Called when the notification is posted
         * Started the service here
         */
        override fun onNotificationPosted(
            notificationId: Int,
            notification: Notification,
            ongoing: Boolean
        ) {
            if (ongoing && !isForegroundService) {

                ContextCompat.startForegroundService(
                    applicationContext,
                    Intent(applicationContext, this@AudioService.javaClass)
                )
                Log.i(TAG, "NotificationID: $notificationId")

                startForeground(notificationId, notification)

                isForegroundService = true
            }
        }

        /**
         * Called when the notification is cancelled
         * Stop the service here
         */
        override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
            stopForeground(true)
            isForegroundService = false
            stopSelf()
        }
    }

    private inner class SmoothQueueNavigator(
        mediaSession: MediaSessionCompat
    ) : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat =
            currentPlaylistItems[windowIndex].description
    }

    /**
     * Connection callback for preparing what to play
     */
    private inner class SmoothPlaybackPrepare : MediaSessionConnector.PlaybackPreparer {
        override fun onCommand(
            player: Player,
            controlDispatcher: ControlDispatcher,
            command: String,
            extras: Bundle?,
            cb: ResultReceiver?
        ) = false

        override fun getSupportedPrepareActions(): Long =
            PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_PREPARE or
                    PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                    PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID


        override fun onPrepare(playWhenReady: Boolean) {
            Log.i(TAG, "onPrepare: called, PlaywhenReady: $playWhenReady")


            val recentSong = storage.loadRecentSong() ?: return

            onPrepareFromMediaId(
                recentSong.mediaId!!, playWhenReady, recentSong.description.extras
            )


        }


        override fun onPrepareFromMediaId(
            mediaId: String,
            playWhenReady: Boolean,
            extras: Bundle?
        ) {
            Log.i(TAG, "onPrepareFromMediaId: called")
            // Log.i(TAG, "onPrepareFromMediaId: PLAYWHENREADY $playWhenReady")
            mediaSource.whenReady {
                val itemToPlay: MediaMetadataCompat? = mediaSource.find { item ->
                    item.id == mediaId
                }
                if (itemToPlay == null) {
                } else {

                    val playbackStartPositionMs = extras?.getLong(
                        MEDIA_DESCRIPTION_EXTRAS_START_PLAYBACK_POSITION_MS,
                        C.TIME_UNSET
                    ) ?: C.TIME_UNSET
                    preparePlaylist(
                        buildPlayList(itemToPlay),
                        itemToPlay,
                        playWhenReady,
                        playbackStartPositionMs
                    )
                }
            }
        }

        /**
         * Builds a playlist based on a [MediaMetadataCompat].
         *
         * TODO: Support building a playlist by artist, genre, etc...
         *
         * @param item Item to base the playlist on.
         * @return a [List] of [MediaMetadataCompat] objects representing a playlist.
         */

        private fun buildPlayList(itemToPlay: MediaMetadataCompat): List<MediaMetadataCompat> =
            mediaSource.filter { it.album == itemToPlay.album }.sortedBy { it.trackNumber }


        override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) {
            TODO("Not yet implemented")
        }

        override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) {
            TODO("Not yet implemented")
        }


    }

    /**
     * [ExoPlayer] events listener class that handles changes in the events
     */
    private inner class PlayerEventListener : Player.Listener {
        override fun onMetadata(metadata: Metadata) {
            super.onMetadata(metadata)

        }

        override fun onPlaybackStateChanged(playbackState: Int) {

            Log.i(TAG, "Player.Listener.onPlaybackStateChanged: Called, STATE: $playbackState")
            when (playbackState) {
                Player.STATE_BUFFERING,
                Player.STATE_READY -> {
                    //display the notification
                    notificationManager.showNotificationForPlayer(exoPlayer)
                    if (playbackState == Player.STATE_READY) {
                        // When playing/paused save the current media item in persistent
                        // storage so that playback can be resumed between device reboots.
                        // Search for "media resumption" for more information.
                        saveRecentSongToStorage()


                    }

                }

                else -> {
                    Log.i(
                        TAG,
                        "Player.Listener.onPlaybackStateChanged: Called, Hide STATE: $playbackState"
                    )
                    //hide the notification
                    notificationManager.hideNotification()

                }


            }
        }

        /**
         * Player changes to a new media item on the playlist
         */
        override fun onMediaItemTransition(
            mediaItem: com.google.android.exoplayer2.MediaItem?,
            reason: Int
        ) {
            Log.i(TAG, "onMediaItemTransition: Cal")

        }
    }


}

private const val SMOOTH_USER_AGENT = "uamp.next"
val MEDIA_DESCRIPTION_EXTRAS_START_PLAYBACK_POSITION_MS = "playback_start_position_ms"
