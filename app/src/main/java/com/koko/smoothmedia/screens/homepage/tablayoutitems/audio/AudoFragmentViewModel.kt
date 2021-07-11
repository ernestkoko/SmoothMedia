package com.koko.smoothmedia.screens.homepage.tablayoutitems.audio


import android.app.Application
import android.content.ContentUris
import android.os.Build
import android.provider.MediaStore
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.koko.smoothmedia.dataclass.SongData

import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.TimeUnit

private const val TAG = "HomeScreen VM"

/**
 * A view model class for the Home Screen
 */
@Suppress("KDocUnresolvedReference")
class AudioFragmentViewModel(val app: Application) : AndroidViewModel(app), Player.Listener {
    private var player: SimpleExoPlayer? = SimpleExoPlayer.Builder(app.applicationContext).build()
    private val mediaSession = MediaSessionCompat(app.applicationContext, TAG)
    private lateinit var mStateBuilder: PlaybackStateCompat.Builder
    private lateinit var concatPlayer: ConcatenatingMediaSource

    //list of songs
    private val _songsList = MutableLiveData<List<SongData>>()
    val songsList: LiveData<List<SongData>> get() = _songsList
    private val _isPlayingSong = MutableLiveData<Boolean>()
    val isPlayingSong: LiveData<Boolean> get() = _isPlayingSong

    //enable us to cancel coroutines
    private var viewModelJob = Job()

    //Dispatchers.MAIN launches the coroutine on the main thread
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)


    fun launchQuerySongs() {

        uiScope.launch {
            queryAudio()
        }
    }

    init {
        initialiseMediaSession()
        checkIfPlaying()
    }

    private fun checkIfPlaying() {
        _isPlayingSong.value = player!!.isPlaying
    }

    //Query the songs
    private suspend fun queryAudio() {
        val songs = mutableListOf<SongData>()
        //TODO display progress indicator
        withContext(Dispatchers.IO) {
            /**
             * A key concept when working with Android [ContentProvider]s is something called
             * "projections". A projection is the list of columns to request from the provider,
             * and can be thought of (quite accurately) as the "SELECT ..." clause of a SQL
             * statement.
             *
             * It's not _required_ to provide a projection. In this case, one could pass `null`
             * in place of `projection` in the call to [ContentResolver.query], but requesting
             * more data than is required has a performance impact.
             *
             * For this sample, we only use a few columns of data, and so we'll request just a
             * subset of columns.
             */
            val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                arrayOf(

                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DATE_ADDED,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.TITLE

                )
            } else {
                arrayOf(
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DATE_ADDED,
                    MediaStore.Audio.Media.TITLE

                )
            }

            getApplication<Application>().contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null, null, null, null
            )?.use { cursor ->
                /**
                 * In order to retrieve the data from the [Cursor] that's returned, we need to
                 * find which index matches each column that we're interested in.
                 *
                 * There are two ways to do this. The first is to use the method
                 * [Cursor.getColumnIndex] which returns -1 if the column ID isn't found. This
                 * is useful if the code is programmatically choosing which columns to request,
                 * but would like to use a single method to parse them into objects.
                 *
                 * In our case, since we know exactly which columns we'd like, and we know
                 * that they must be included (since they're all supported from API 1), we'll
                 * use [Cursor.getColumnIndexOrThrow]. This method will throw an
                 * [IllegalArgumentException] if the column named isn't found.
                 *
                 * In either case, while this method isn't slow, we'll want to cache the results
                 * to avoid having to look them up for each row.
                 */
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val dateModifiedColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val displayNameColumn =
                    cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val duration = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)

                Log.i(TAG, "Found ${cursor.count} images")
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(displayNameColumn)
                    val dateAdded =
                        Date(TimeUnit.SECONDS.toMillis(cursor.getLong(dateModifiedColumn)))
                    val duratn = cursor.getLong(duration)
                    val title = cursor.getString(titleColumn)

                    val contentUri =
                        ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val song = SongData(id, contentUri, title, dateAdded, displayName, duratn)
                    songs.add(song)


                }

            }

        }
        //TODO end the  display progress indicator
        _songsList.value = songs
        //prepare the player
        preparePlayer()


    }

    /**
     * a function that plays the song
     */
    private fun preparePlayer() {
        //remove every item on the player list
        player!!.removeMediaItems(0, _songsList.value!!.size)
        //declare a mutable list of MediaItem
        val list = mutableListOf<MediaItem>()
        //loop through [_songsList] to get each song
        for (song in _songsList.value!!) {
            /*
             add the song to the first position on the list if the given uri is same as the
             contentUri in the list else just add the song to the list.
             */
            list.add(MediaItem.fromUri(song.contentUri))
        }
        //give the list to the player
        player!!.setMediaItems(list)
        //prepare the player
        player!!.prepare()
        //play songs from the list, starting from the first position
        // player!!.playWhenReady = true
        player!!.addListener(this)

        //startService(uri.toString())
        mStateBuilder.setState(PlaybackStateCompat.STATE_PLAYING, player!!.currentPosition, 1f)

    }

//    fun startService(uri: String) {
//        intent.also {
//            val bundle = Bundle()
//
//
//
//
//            it.putExtra("bundle", uri)
//           // app.startService(it)
//        }
//    }

//    fun stopService() {
//        intent.also {
//            app.stopService(it)
//        }
//    }

    /**
     * [nextSong] selects the next song on the list if it exists
     */
    fun nextSong() {
        if (player!!.hasNext()) {
            player!!.next()
        }


    }

    /**
     * [previousSong] selects the previous songs on the list if it ecists
     */
    fun previousSong() {
        if (player!!.hasPrevious()) {
            player!!.previous()
        }

    }

    /**
     * function that stops the play back
     */
    fun stopPlayBack() {
        checkIfPlaying()
        // stopService()
        if (player!!.isPlaying) {

            player!!.stop()
        } else {
            player!!.prepare()
            player!!.playWhenReady = true
            // player!!.play()
        }

    }

    /**
     * function that pauses the playback
     */
    fun pausePlayBack() {
        if (player!!.isPlaying) {
            player!!.pause()
        }


    }

    /**
     * function that resumes that playback from the pause position
     */
    fun resumePlayBack() {


    }


    /**
     * Triggers when an item(song) on the list is clicked
     */
    fun onSongClicked(it: SongData) {
        //startService(it.contentUri.toString())
//        preparePlayer(it.contentUri)
        //check if the exact song to play is in the list, if present jump to the position of the song
        //and play
        for (song in _songsList.value!!) {
            if (song == it) {
                player!!.seekTo(_songsList.value!!.indexOf(song), C.TIME_UNSET)
                if (!player!!.isPlaying) {
                    player!!.let {
                        it.prepare()
                        it.playWhenReady = true
                    }
                }
                return
            }

        }

    }

    private fun initialiseMediaSession() {
        // Do not let MediaButtons restart the player when the app is not visible.
        mediaSession.setMediaButtonReceiver(null)
        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player.

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player.
        mStateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE
            )
        mediaSession.setPlaybackState(mStateBuilder.build())
        // MySessionCallback has methods that handle callbacks from a media controller.
        mediaSession.setCallback(MySessionCallback())
        mediaSession.isActive = true


    }

    /**
     * [onCleared] is called before the view model is destroyed
     * cancel the Job
     */
    override fun onCleared() {
        super.onCleared()
        //release the player
        player!!.release()
        player = null
        viewModelJob.cancel()
    }

    /**
     * The below methods are called for listening to the Player
     */
    override fun onTracksChanged(
        trackGroups: TrackGroupArray,
        trackSelections: TrackSelectionArray
    ) {
        Log.i(TAG, "onTracksChanged: Called")
        Log.i(TAG, "onTracksChanged: ${trackSelections.all}")
        super.onTracksChanged(trackGroups, trackSelections)
    }

    override fun onEvents(player: Player, events: Player.Events) {

        Log.i(TAG, "onEvents: Called ${events.javaClass.name}")


        super.onEvents(player, events)
    }

    override fun onPlaybackStateChanged(state: Int) {
        Log.i(TAG, "onPlaybackStateChanged: Called ..${state}")
        Log.i(TAG, "onPlaybackStateChanged: Called ..${player!!.currentWindowIndex}")

        super.onPlaybackStateChanged(state)

    }


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
}