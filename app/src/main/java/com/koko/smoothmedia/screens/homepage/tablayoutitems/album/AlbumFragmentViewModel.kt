package com.koko.smoothmedia.screens.homepage.tablayoutitems.album

import android.app.Application
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.*
import com.koko.smoothmedia.dataclass.Song
import com.koko.smoothmedia.mediasession.EMPTY_PLAYBACK_STATE
import com.koko.smoothmedia.mediasession.NOTHING_PLAYING
import com.koko.smoothmedia.mediasession.extension.METADATA_KEY_ITEM_COUNT
import com.koko.smoothmedia.mediasession.extension.id
import com.koko.smoothmedia.mediasession.library.SMOOTH_ALBUMS_ROOT
import com.koko.smoothmedia.mediasession.mediaconnection.MusicServiceConnection

class AlbumFragmentViewModel(
    val app: Application,
    musicServiceConnection: MusicServiceConnection
) : AndroidViewModel(app) {
    private val TAG = "AlbumFragmentViewModel"
    private val _songsList = MutableLiveData<List<Song>?>()
    val songsList: LiveData<List<Song>?> = _songsList
    private var playbackState: PlaybackStateCompat = EMPTY_PLAYBACK_STATE
    private val subscriptionCallback = object : SubscriptionCallback() {
        override fun onError(parentId: String) {

        }

        override fun onChildrenLoaded(
            parentId: String,
            children: MutableList<MediaBrowserCompat.MediaItem>
        ) {
            Log.i(TAG, "onChildrenLoaded: Children: $children")
            val list = children.map {
                val description = it.description
                Log.i(
                    TAG, "onChildrenLoaded: ItemCount: ${
                        it.flags
                    }"
                )
                Log.i(TAG, "Extra: ${description.extras?.getParcelableArray(METADATA_KEY_ITEM_COUNT)}")
                val bundle = description.extras!!

                val load = bundle.classLoader
                load.parent
                Log.i(TAG, "classLoader: ${bundle}")
               val loader = bundle.keySet().forEach {
                   Log.i(TAG, "Keyset: $it")
               }

                Log.i(TAG, "Extra1: ${description.title}")

                Song(
                    title = description.subtitle.toString(),
                    albumArtUri = description.iconUri,
                    itemCount = description.title.toString().toLong()


                )

            }

            _songsList.postValue(list)


        }
    }

    init {
        musicServiceConnection.subscribe(
            SMOOTH_ALBUMS_ROOT,
            subscriptionCallback
        )
    }


    private val playbackStateObserver = Observer<PlaybackStateCompat> {
        playbackState = it ?: EMPTY_PLAYBACK_STATE
        val metadata = musicServiceConnection.nowPlaying.value ?: NOTHING_PLAYING
        if (metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) != null) {
            _songsList.postValue(updateMetadata(metadata))

        }
    }
    private val mediaMetadataObserver = Observer<MediaMetadataCompat> {
        val playbackState = musicServiceConnection.playbackState.value ?: EMPTY_PLAYBACK_STATE
        val metadata = it ?: NOTHING_PLAYING
        if (!metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID).isNullOrEmpty()) {

            Log.i(TAG, "mediaMetadataObserver: called")
            Log.i(
                TAG,
                "mediaMetadataObserver: id: ${metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)}"
            )

            _songsList?.postValue(updateMetadata(metadata))


        }
    }
    private val musicServiceConnection = musicServiceConnection.also {
//        it.subscribe(musicServiceConnection.rootMedia, subscriptionCallback)
        it.playbackState.observeForever(playbackStateObserver)
        it.nowPlaying.observeForever(mediaMetadataObserver)
    }

    private fun updateMetadata(mediaMetadata: MediaMetadataCompat): List<Song> {
        return songsList?.value?.map {
            if (mediaMetadata.id == it.id) {
                Log.i(TAG, "updateMetadata: Matching ${it.isPlaying}")
                Log.i(TAG, "updateMetadata: Item ${it}")
                it.copy(isPlaying = true)

            } else {
                it.copy(isPlaying = false)
            }
        }?.toList() ?: emptyList()

    }

    /**
     * subscribe to the service
     */
    fun subscribe() {
//        Log.i(
//            TAG,
//            "subscribe called: $parentId"
//        )
//        musicServiceConnection.subscribe(SMOOTH_ALBUMS_ROOT, subscriptionCallback)

    }

    class Factory(
        private val application: Application,
        private val musicServiceConnection: MusicServiceConnection
    ) :
        ViewModelProvider.NewInstanceFactory() {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AlbumFragmentViewModel::class.java)) {
                return AlbumFragmentViewModel(application, musicServiceConnection) as T
            }
            throw IllegalArgumentException("Unknown Model Class")
        }
    }
}