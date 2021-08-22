package com.koko.smoothmedia.mediasession.services

import android.app.*
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.koko.smoothmedia.MainActivity
import com.koko.smoothmedia.R
import com.koko.smoothmedia.mediasession.extension.*
import com.koko.smoothmedia.mediasession.library.BrowseTree
import com.koko.smoothmedia.mediasession.library.InbuiltMusicSource
import com.koko.smoothmedia.mediasession.library.MusicSource
import com.koko.smoothmedia.other.Constants.ACTION_SHOW_AUDIO_FRAGMENT
import com.koko.smoothmedia.other.Constants.NOTIFICATION_CHANNEL_ID
import com.koko.smoothmedia.other.Constants.NOTIFICATION_CHANNEL_NAME
import com.koko.smoothmedia.other.Constants.NOTIFICATION_ID
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
    val TAG = "AudiService"
    private lateinit var currentPlayer: Player

    // private lateinit var mediaSource: MusicSource
    private lateinit var notificationManager: SmoothNotificationManager
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    protected lateinit var mediaSession: MediaSessionCompat
    protected lateinit var mediaSessionConnector: MediaSessionConnector
    private lateinit var stateBuilder: PlaybackStateCompat.Builder
    private var currentPlaylistItems: List<MediaMetadataCompat> = emptyList()
    private lateinit var mediaSource: MusicSource

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


    override fun onCreate() {
        super.onCreate()
        // Build a PendingIntent that can be used to launch the UI.
        val sessionActivityPendingIntent =
            packageManager?.getLaunchIntentForPackage(packageName)?.let { sessionIntent ->
                PendingIntent.getActivity(this, 0, sessionIntent, 0)
            }
        // Create a new MediaSession.
        setupMediaSession(sessionActivityPendingIntent)

        //setup player
        setupPlayer(exoPlayer)

        //notification listener
        notificationManager = SmoothNotificationManager(this, mediaSession.sessionToken,
        PlayerNotificationListener())


        mediaSource = InbuiltMusicSource()
        //load the songs
        serviceScope.launch {
            mediaSource.load(applicationContext)
        }


        /**
         * The notification manager will use our player and media session to decide when to post
         * notifications. When notifications are posted or removed our listener will be called, this
         * allows us to promote the service to foreground (required so that we're not killed if
         * the main UI is not visible).
         */
        notificationManager = SmoothNotificationManager(
            this,
            mediaSession.sessionToken,
            PlayerNotificationListener()
        )


    }

    private fun setupPlayer(player: Player) {
        currentPlayer= exoPlayer
        if(player != null){
            val playbackState = player.playbackState
            if(currentPlaylistItems.isEmpty()){
                currentPlayer.stop()
                currentPlayer.clearMediaItems()
            }else if(playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED){
                preparePlaylist(
                    metadataList = currentPlaylistItems,
                    itemToPlay = currentPlaylistItems[player.currentWindowIndex],
                    playWhenReady = player.playWhenReady,
                    playbackStartPositionMs = player.currentPosition
                )
            }
        }
        mediaSessionConnector.setPlayer(player)

    }

    private fun setupMediaSession(sessionActivityPendingIntent: PendingIntent?) {
        Log.i(TAG,"setupMediaSession: Called")
        mediaSession = MediaSessionCompat(this, TAG)
            .apply {
                setSessionActivity(sessionActivityPendingIntent)
                isActive = true
                // Enable callbacks from MediaButtons and TransportControls
//                setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
////                        or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
////                )
                // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
                stateBuilder = PlaybackStateCompat.Builder()
                    .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                setPlaybackState(stateBuilder.build())
                //set the session call back
                // setCallback(MyMediaSessionCallback())
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
        Log.i(TAG, "$clientPackageName: ClientPackageName")
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
        result.detach()
        var myList = mutableListOf<MediaItem>()

        Log.i(TAG, "$parentId: Root")

        val mediaItems = mutableListOf<MediaItem>()
        if (parentId == "/") {

            Log.i(TAG, "$parentId: Root(/)")
            //MediaItem(MediaDescriptionCompat.fromMediaDescription())
//            val resultSent= mediaSource.whenReady {  ready->
//
//            }

            //serviceScope.launch {
            val resultSent = mediaSource.whenReady { successful ->
                if (successful) {
                    val children = browseTree[parentId]?.map { item ->
                        MediaItem(item.description, item.flag)
                    }?.toMutableList()
                    result.sendResult(children)
                }

            }

//                myList =
//                    InbuiltMusicSource().updateCatalog(applicationContext)!!.map {
//                        MediaItem(it.description, it.flag)
//                    }.toMutanbleList()
//
//
//                Log.i(TAG, "$parentId: Root(/): Songs: ${myList}")
//                result.sendResult(myList)
            //  }


        } else {
            result.sendResult(mediaItems)
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
        val initialWindowIndex = if(itemToPlay==null)0 else metadataList.indexOf(itemToPlay)
        currentPlaylistItems = metadataList
        currentPlayer.playWhenReady = playWhenReady
        currentPlayer.stop()
        currentPlayer.clearMediaItems()
        val mediaSource = metadataList.toMediaSource(dataSourceFactory)
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        exoPlayer.seekTo(initialWindowIndex,playbackStartPositionMs)

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
     * Listen for notification events.
     */
    private inner class PlayerNotificationListener :
        PlayerNotificationManager.NotificationListener {
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

                startForeground(notificationId, notification)
                isForegroundService = true
            }
        }

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
        )=false

        override fun getSupportedPrepareActions(): Long =
            PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_PREPARE or
                    PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID

        override fun onPrepare(playWhenReady: Boolean) {
            Log.i(TAG, "onPrepare: called")
            Log.i(TAG, "onPrepare: called, PlaywhenReady: $playWhenReady")
            Log.i(TAG, "onPrepare: called, currentList: $currentPlaylistItems")
            val mediaMetadataCompats = mutableListOf<MediaMetadataCompat>()
            mediaSource.whenReady {
                if(it){

                    mediaSource.forEach {
                        mediaMetadataCompats.add(it)
                    }
                }
            }
            currentPlaylistItems=mediaMetadataCompats
            Log.i(TAG, "onPrepare: called, currentList: $currentPlaylistItems")
            onPrepareFromMediaId(currentPlaylistItems[1].description.mediaId!!,playWhenReady,null)

        }


        override fun onPrepareFromMediaId(
            mediaId: String,
            playWhenReady: Boolean,
            extras: Bundle?
        ) {
            Log.i(TAG, "onPrepareFromMediaId: called")
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
        override fun onPlaybackStateChanged(state: Int) {

            Log.i(TAG, "Player.Listener.onPlaybackStateChanged: Called")
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

    inner class MyMediaSessionCallback : MediaSessionCompat.Callback() {
        override fun onPlay() {
            super.onPlay()
            Log.i(TAG, "onPlay")
        }

        override fun onStop() {
            super.onStop()
            Log.i(TAG, "onSTop")
        }

        override fun onPause() {
            super.onPause()
            Log.i(TAG, "onPause")
        }

        override fun onSkipToNext() {
            super.onSkipToNext()
        }

        override fun onSkipToPrevious() {
            super.onSkipToPrevious()
        }
    }

}

private const val SMOOTH_USER_AGENT = "uamp.next"
val MEDIA_DESCRIPTION_EXTRAS_START_PLAYBACK_POSITION_MS = "playback_start_position_ms"