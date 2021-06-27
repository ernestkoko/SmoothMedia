package com.koko.smoothmedia.screens.homepage.tablayoutitems.audio

import android.app.Application
import android.content.ContentUris

import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.MediaItem


import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ConcatenatingMediaSource

import com.koko.smoothmedia.dataclass.SongData
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.TimeUnit

private const val TAG = "HomeScreen VM"

/**
 * A view model class for the Home Screen
 */
@Suppress("KDocUnresolvedReference")
class AudioFragmentViewModel(val app: Application) : AndroidViewModel(app) {
    //list of songs
    private val _songsList = MutableLiveData<List<SongData>>()
    val songsList: LiveData<List<SongData>> get() = _songsList

    //enable us to cancel coroutines
    private var viewModelJob = Job()

    //Dispatchers.MAIN launches the coroutine on the main thread
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)
    private var player: SimpleExoPlayer? = SimpleExoPlayer.Builder(app.applicationContext).build()
    private lateinit var concatPlayer: ConcatenatingMediaSource

    fun launchQuerySongs(){
        uiScope.launch {
            queryAudio()
        }
    }

    //Query the songs
    private suspend fun queryAudio() {
        val songs = mutableListOf<SongData>()
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
        _songsList.value = songs
    }

    /**
     * a function that plays the song
     */
    fun playSong(uri: Uri) {
        //remove every item on the player list
        player!!.removeMediaItems(0, _songsList.value!!.size)
        //declare a mutable list of MediaItem
        val list = mutableListOf<MediaItem>()
        //loop through [_sonsList] to get each song
        for(song in _songsList.value!!){
            /*
             add the song to the first position on the list if the given uri is same as the
             contentUri in the list else just add the song to the list
             */
            when(uri){

                song.contentUri->list.add(0, MediaItem.fromUri(uri))
                else ->list.add(MediaItem.fromUri(song.contentUri))
            }
        }
        //give the list to the player
        player!!.setMediaItems(list)
        //prepare the player
        player!!.prepare()
        //play songs from the list, starting from the first position
        player!!.play()

    }

    /**
     * [nextSong] selects the next song on the list if it exists
     */
   fun  nextSong(){
       if(player!!.hasNext()){
           player!!.next()
       }


    }

    /**
     * [previousSong] selects the previous songs on the list if it ecists
     */
    fun previousSong(){
        if(player!!.hasPrevious()){
            player!!.previous()
        }
    }

    /**
     * function that stops the play back
     */
    fun stopPlayBack() {
        if(player!!.isPlaying) {
            player!!.stop()
        }

    }

    /**
     * function that pauses the playback
     */
    fun pausePlayBack() {
        if(player!!.isPlaying) {
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
        playSong(it.contentUri)
    }
    /**
     * cancel the Job
     */
    override fun onCleared() {
        super.onCleared()
        //release the player
        player!!.release()
        player = null
        viewModelJob.cancel()
    }
}