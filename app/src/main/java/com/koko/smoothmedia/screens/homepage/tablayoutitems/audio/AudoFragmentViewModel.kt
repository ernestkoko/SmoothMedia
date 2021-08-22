package com.koko.smoothmedia.screens.homepage.tablayoutitems.audio


import android.app.Application
import android.content.ContentUris
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.*
import androidx.lifecycle.Observer
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.koko.smoothmedia.R
import com.koko.smoothmedia.dataclass.MediaItemData
import com.koko.smoothmedia.dataclass.Song
import com.koko.smoothmedia.dataclass.SongData
import com.koko.smoothmedia.mediasession.EMPTY_PLAYBACK_STATE
import com.koko.smoothmedia.mediasession.NOTHING_PLAYING
import com.koko.smoothmedia.mediasession.extension.id
import com.koko.smoothmedia.mediasession.extension.isPlaying
import com.koko.smoothmedia.mediasession.mediaconnection.MusicServiceConnection
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.TimeUnit

private const val TAG = "HomeScreen VM"

/**
 * A view model class for the Home Screen
 */
@Suppress("KDocUnresolvedReference")
class AudioFragmentViewModel(
    val app: Application,
    musicServiceConnection: MusicServiceConnection
) : AndroidViewModel(app) {

    /**
     * Use a backing property so consumers of mediaItems only get a [LiveData] instance so
     * they don't inadvertently modify it.
     */
    private val _mediaItems = MutableLiveData<List<MediaItemData>>()
    val mediaItems: LiveData<List<MediaItemData>> = _mediaItems
   private val _songsListan = MutableLiveData<List<Song>>()
    val songsListan : LiveData<List<Song>>
    get() = _songsListan


    private val subscriptionCallback = object : SubscriptionCallback() {
        //called when there is error in loading the children
        override fun onError(parentId: String) {
            Log.i(TAG, "onError: $parentId")
        }

        //this returns the list of children from the media browser service compact
        override fun onChildrenLoaded(
            parentId: String,
            children: List<MediaBrowserCompat.MediaItem>
        ) {
            Log.i(TAG, "List of Children: $children")
            Log.i(TAG, "Parent Id: $parentId")

            val lisChildren = children.map { child->

                val description = child.description
                Song(description.mediaId!!,description.mediaUri,description.title.toString())
            }
            Log.i(TAG, "List of Songs: ${lisChildren}")
            _songsList.postValue(lisChildren)

//            val itemsList = children.map { child ->
//                val subtitle = child.description.subtitle ?: ""
//
//                Song(
//
//                )
////                MediaItemData(
////                    child.mediaId!!,
////                    child.description.title.toString(),
////                    subtitle.toString(),
////                    child.description.iconUri!!,
////                    child.isBrowsable,
////                    getResourceForMediaId(child.mediaId!!)
////                )
//            }
//            Log.i(TAG, "List of Items: $itemsList")
//            _songsListan.postValue(itemsList)
       }
    }

    /**
     * When the session's [PlaybackStateCompat] changes, the [mediaItems] need to be updated
     * so the correct [MediaItemData.playbackRes] is displayed on the active item.
     * (i.e.: play/pause button or blank)
     */
    private val playbackStateObserver = Observer<PlaybackStateCompat> {
        val playbackState = it ?: EMPTY_PLAYBACK_STATE
        val metadata = musicServiceConnection.nowPlaying.value ?: NOTHING_PLAYING
        if (metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) != null) {
            _mediaItems.postValue(updateState(playbackState, metadata))
        }
    }

    /**
     * When the session's [MediaMetadataCompat] changes, the [mediaItems] need to be updated
     * as it means the currently active item has changed. As a result, the new, and potentially
     * old item (if there was one), both need to have their [MediaItemData.playbackRes]
     * changed. (i.e.: play/pause button or blank)
     */
    private val mediaMetadataObserver = Observer<MediaMetadataCompat> {
        val playbackState = musicServiceConnection.playbackState.value ?: EMPTY_PLAYBACK_STATE
        val metadata = it ?: NOTHING_PLAYING
        if (metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) != null) {
            _mediaItems.postValue(updateState(playbackState, metadata))
        }
    }

    val rootMediaId: LiveData<String> =
        Transformations.map(musicServiceConnection.isConnected) { isConnected ->
            if (isConnected) {
                musicServiceConnection.rootMedia

            } else {
                null
            }
        }

    /**
     * Because there's a complex dance between this [ViewModel] and the [MusicServiceConnection]
     * (which is wrapping a [MediaBrowserCompat] object), the usual guidance of using
     * [Transformations] doesn't quite work.
     *
     * Specifically there's three things that are watched that will cause the single piece of
     * [LiveData] exposed from this class to be updated.
     *
     * [subscriptionCallback] (defined above) is called if/when the children of this
     * ViewModel's [mediaId] changes.
     *
     * [MusicServiceConnection.playbackState] changes state based on the playback state of
     * the player, which can change the [MediaItemData.playbackRes]s in the list.
     *
     * [MusicServiceConnection.nowPlaying] changes based on the item that's being played,
     * which can also change the [MediaItemData.playbackRes]s in the list.
     */
    private val musicServiceConnection = musicServiceConnection.also {
//        it.subscribe(musicServiceConnection.rootMedia, subscriptionCallback)
        it.playbackState.observeForever(playbackStateObserver)
        it.nowPlaying.observeForever(mediaMetadataObserver)
    }

    /**
     * subscribe to the service
     */
    fun subscribe(parentId: String){
        Log.i(TAG, "subscribe called: $parentId")
        musicServiceConnection.subscribe(parentId, subscriptionCallback)

    }


    private fun getResourceForMediaId(mediaId: String): Int {
        val isActive = mediaId == musicServiceConnection.nowPlaying.value?.id
        val isPlaying = musicServiceConnection.playbackState.value?.isPlaying ?: false
        return when {
            !isActive -> NO_RES
            isPlaying -> R.drawable.ic_pause_black_24dp
            else -> R.drawable.ic_play_arrow_black_24dp
        }
    }

    private fun updateState(
        playbackState: PlaybackStateCompat,
        mediaMetadata: MediaMetadataCompat
    ): List<MediaItemData> {

        val newResId = when (playbackState.isPlaying) {
            true -> R.drawable.ic_pause_black_24dp
            else -> R.drawable.ic_play_arrow_black_24dp
        }

        return mediaItems.value?.map {
            val useResId = if (it.mediaId == mediaMetadata.id) newResId else NO_RES
            it.copy(playbackRes = useResId)
        } ?: emptyList()
    }


    ///my former code

    private var player: SimpleExoPlayer? = SimpleExoPlayer.Builder(app.applicationContext).build()
    private val mediaSession = MediaSessionCompat(app.applicationContext, TAG)
    private lateinit var mStateBuilder: PlaybackStateCompat.Builder
    private lateinit var concatPlayer: ConcatenatingMediaSource

    //list of songs
    private val _songsList = MutableLiveData<List<Song>>()
    val songsList: LiveData<List<Song>> get() = _songsList
    private val _isPlayingSong = MutableLiveData<Boolean>()
    val isPlayingSong: LiveData<Boolean> get() = _isPlayingSong











    /**
     * [nextSong] selects the next song on the list if it exists
     */
    fun nextSong() {



    }

    /**
     * [previousSong] selects the previous songs on the list if it ecists
     */
    fun previousSong() {


    }

    /**
     * function that stops the play back
     */
    fun startStopPlayback() {
        Log.i(TAG,"startStopPlayback: Called")


        musicServiceConnection.transportControl.play()

    }





    /**
     * Triggers when an item(song) on the list is clicked
     */
    fun onSongClicked(it: Song) {


    }


    /**
     * [onCleared] is called before the view model is destroyed
     * cancel the Job
     */
    override fun onCleared() {
        super.onCleared()

        // And then, finally, unsubscribe the media ID that was being watched.
        //TODO
        musicServiceConnection.unsubscribe(musicServiceConnection.rootMedia, subscriptionCallback)


    }

    /**
     * The below methods are called for listening to the Player
     */
//    override fun onTracksChanged(
//        trackGroups: TrackGroupArray,
//        trackSelections: TrackSelectionArray
//    ) {
//        Log.i(TAG, "onTracksChanged: Called")
//        Log.i(TAG, "onTracksChanged: ${trackSelections.all}")
//        super.onTracksChanged(trackGroups, trackSelections)
//    }

//    override fun onEvents(player: Player, events: Player.Events) {
//
//        Log.i(TAG, "onEvents: Called ${events.javaClass.name}")
//
//
//        super.onEvents(player, events)
//    }

//    override fun onPlaybackStateChanged(state: Int) {
//        Log.i(TAG, "onPlaybackStateChanged: Called ..${state}")
//        Log.i(TAG, "onPlaybackStateChanged: Called ..${player!!.currentWindowIndex}")
//
//        super.onPlaybackStateChanged(state)
//
//    }


    /**
     * Media Session Callbacks, where all external clients control the player.
     */
    inner class MySessionCallback : MediaSessionCompat.Callback() {
        override fun onPlay() {
            Log.i(TAG, "onPlay: Called")
            player!!.playWhenReady = true
        }

        override fun onPause() {
            Log.i(TAG, "onPause: Called")
            player!!.playWhenReady = false
        }

        override fun onPrepare() {
            super.onPrepare()
        }

        override fun onSkipToNext() {

        }

        override fun onSkipToPrevious() {
            player!!.seekTo(0)
        }
    }

    class Factory(
        private val application: Application,
        private val musicServiceConnection: MusicServiceConnection
    ) :
        ViewModelProvider.NewInstanceFactory() {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AudioFragmentViewModel::class.java)) {
                return AudioFragmentViewModel(application, musicServiceConnection) as T
            }
            throw IllegalArgumentException("Unknown Model Class")
        }
    }
}

private const val NO_RES = 0