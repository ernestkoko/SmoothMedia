package com.koko.smoothmedia

import android.app.Application
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.*
import com.koko.smoothmedia.mediasession.extension.*
import com.koko.smoothmedia.mediasession.mediaconnection.EMPTY_PLAYBACK_STATE
import com.koko.smoothmedia.mediasession.mediaconnection.MusicServiceConnection
import com.koko.smoothmedia.mediasession.mediaconnection.NOTHING_PLAYING

private const val TAG = "MainActivityViewModel"

class MainActivityViewModel(
    val app: Application, musicServiceConnection: MusicServiceConnection
) : ViewModel() {
    /**
     * Utility class used to represent the metadata necessary to display the
     * media item currently being played.
     */
    data class NowPlayingMetadata(
        val id: String,
        val albumArtUri: Uri,
        val title: String?,
        val subtitle: String?,
        val duration: String
    ) {

        companion object {
            /**
             * Utility method to convert milliseconds to a display of minutes and seconds
             */
            fun timestampToMSS(context: Context, position: Long): String {
                val totalSeconds = Math.floor(position / 1E3).toInt()
                val minutes = totalSeconds / 60
                val remainingSeconds = totalSeconds - (minutes * 60)
                return if (position < 0) context.getString(R.string.duration_unknown)
                else context.getString(R.string.duration_format).format(minutes, remainingSeconds)
            }
        }
    }

    private val _animatePlayPauseButton = MutableLiveData<Boolean>()
    val animatePlayPauseButton: LiveData<Boolean>
        get() = _animatePlayPauseButton
    private val _animateNextButton = MutableLiveData<Boolean>()
    val animateNextButton: LiveData<Boolean>
        get() = _animateNextButton
    private val _animatePreviousButton = MutableLiveData<Boolean>()
    val animatePreviousButton: LiveData<Boolean>
        get() = _animatePreviousButton

    private var playbackState: PlaybackStateCompat = EMPTY_PLAYBACK_STATE
    val mediaMetadata = MutableLiveData<NowPlayingMetadata>()
    val mediaPosition = MutableLiveData<Long>().apply {
        postValue(0L)
    }
    val mediaButtonRes = MutableLiveData<Int>().apply {
        postValue(R.drawable.ic_album_black_24dp)
    }

    private var updatePosition = true
    private val handler = Handler(Looper.getMainLooper())
    val rootMediaId: LiveData<String> =
        Transformations.map(musicServiceConnection.isConnected) { isConnected ->
            if (isConnected) {
                //call prepare to load the last song that was played
                    musicServiceConnection.subscribe("/", subscriptionCallBack)

                preparePlayer()


                musicServiceConnection.rootMedia

            } else {
                null
            }
        }

    val subscriptionCallBack= object: MediaBrowserCompat.SubscriptionCallback(){
        override fun onChildrenLoaded(
            parentId: String,
            children: MutableList<MediaBrowserCompat.MediaItem>
        ) {
            Log.i(TAG, "children: $children")
        }
    }
    /**
     * [preparePlayer]
     */
    private fun preparePlayer() {
        val playbackState = musicServiceConnection.playbackState.value
        if (!playbackState!!.isPrepared) musicServiceConnection.transportControl.prepare()


    }

    /**
     * When the session's [PlaybackStateCompat] changes, the [mediaItems] need to be updated
     * so the correct [MediaItemData.playbackRes] is displayed on the active item.
     * (i.e.: play/pause button or blank)
     */
    private val playbackStateObserver = Observer<PlaybackStateCompat> {
        playbackState = it ?: EMPTY_PLAYBACK_STATE
        val metadata = musicServiceConnection.nowPlaying.value ?: NOTHING_PLAYING
        updateState(playbackState, metadata)
    }

    /**
     * When the session's [MediaMetadataCompat] changes, the [mediaItems] need to be updated
     * as it means the currently active item has changed. As a result, the new, and potentially
     * old item (if there was one), both need to have their [MediaItemData.playbackRes]
     * changed. (i.e.: play/pause button or blank)
     */
    private val mediaMetadataObserver = Observer<MediaMetadataCompat> {
        updateState(playbackState, it)
    }

    /**
     * Because there's a complex dance between this [ViewModel] and the [MusicServiceConnection]
     * (which is wrapping a [MediaBrowserCompat] object), the usual guidance of using
     * [Transformations] doesn't quite work.
     *
     * Specifically there's three things that are watched that will cause the single piece of
     * [LiveData] exposed from this class to be updated.
     *
     * [MusicServiceConnection.playbackState] changes state based on the playback state of
     * the player, which can change the [MediaItemData.playbackRes]s in the list.
     *
     * [MusicServiceConnection.nowPlaying] changes based on the item that's being played,
     * which can also change the [MediaItemData.playbackRes]s in the list.
     */
    private val musicServiceConnection = musicServiceConnection.also {
        it.playbackState.observeForever(playbackStateObserver)
        it.nowPlaying.observeForever(mediaMetadataObserver)
        checkPlaybackPosition()
    }

    /**
     * Internal function that recursively calls itself every [POSITION_UPDATE_INTERVAL_MILLIS] ms
     * to check the current playback position and updates the corresponding LiveData object when it
     * has changed.
     */
    private fun checkPlaybackPosition(): Boolean = handler.postDelayed({
        val currPosition = playbackState.currentPlayBackPosition
        if (mediaPosition.value != currPosition)
            mediaPosition.postValue(currPosition)
        if (updatePosition)
            checkPlaybackPosition()
    }, POSITION_UPDATE_INTERVAL_MILLIS)

    /**
     * Function that controls play and pause from the [activity_main] xml file
     */
    fun playPauseFun() {
        Log.i(TAG, " PlayPause Clicked")
        _animatePlayPauseButton.value = true
        if (musicServiceConnection.playbackState.value!!.isPlaying) {
            musicServiceConnection.transportControl.pause()
        } else
            musicServiceConnection.transportControl.play()


    }

    fun doneWithAnimation() {
        _animatePlayPauseButton.value = false
        _animateNextButton.value = false
        _animatePreviousButton.value = false
    }

    fun next() {
        _animateNextButton.value = true
        musicServiceConnection.transportControl.skipToNext()
    }

    fun previous() {
        _animatePreviousButton.value = true

        musicServiceConnection.transportControl.skipToPrevious()
    }

    /**
     * Since we use [LiveData.observeForever] above (in [musicServiceConnection]), we want
     * to call [LiveData.removeObserver] here to prevent leaking resources when the [ViewModel]
     * is not longer in use.
     *
     * For more details, see the kdoc on [musicServiceConnection] above.
     */
    override fun onCleared() {
        super.onCleared()

        // Remove the permanent observers from the MusicServiceConnection.
        musicServiceConnection.playbackState.removeObserver(playbackStateObserver)
        musicServiceConnection.nowPlaying.removeObserver(mediaMetadataObserver)

        // Stop updating the position
        updatePosition = false
    }

    private fun updateState(
        playbackState: PlaybackStateCompat,
        mediaMetadata: MediaMetadataCompat
    ) {
        Log.i(TAG, "updateState: called: Duration: ${mediaMetadata.duration}")
        // Only update media item once we have duration available
        if (mediaMetadata.duration != 0L && mediaMetadata.id != null) {
            val nowPlayingMetadata = NowPlayingMetadata(
                mediaMetadata.id!!,
                mediaMetadata.albumArtUri,
                mediaMetadata.title?.trim(),
                mediaMetadata.displaySubtitle?.trim(),
                NowPlayingMetadata.timestampToMSS(app, mediaMetadata.duration)
            )
            this.mediaMetadata.postValue(nowPlayingMetadata)
        }

        // Update the media button resource ID
        mediaButtonRes.postValue(
            when (playbackState.isPlaying) {
                true -> R.drawable.ic_pause_notification
                else -> R.drawable.ic_play_notification
            }
        )
    }


    class Factory(
        private val application: Application,
        private val musicServiceConnection: MusicServiceConnection
    ) : ViewModelProvider.NewInstanceFactory() {
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainActivityViewModel::class.java)) {
                return MainActivityViewModel(application, musicServiceConnection) as T
            }
            throw IllegalArgumentException("Unknown ViewModel")
        }
    }
}

private const val POSITION_UPDATE_INTERVAL_MILLIS = 100L