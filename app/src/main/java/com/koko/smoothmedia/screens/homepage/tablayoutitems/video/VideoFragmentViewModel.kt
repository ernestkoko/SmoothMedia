package com.koko.smoothmedia.screens.homepage.tablayoutitems.video

import android.app.Application
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.koko.smoothmedia.dataclass.Song
import com.koko.smoothmedia.mediasession.library.SMOOTH_ALBUMS_ROOT
import com.koko.smoothmedia.mediasession.mediaconnection.MusicServiceConnection
import com.koko.smoothmedia.screens.homepage.tablayoutitems.audio.AudioFragmentViewModel


class VideoFragmentViewModel(
    val app: Application,
    musicServiceConnection: MusicServiceConnection
) : AndroidViewModel(app) {

    private val musicServiceConnection = musicServiceConnection.also {
//        it.subscribe(musicServiceConnection.rootMedia, subscriptionCallback)
//        it.playbackState.observeForever(playbackStateObserver)
//        it.nowPlaying.observeForever(mediaMetadataObserver)
    }

    /**
     * subscribe to the service
     */
    fun subscribe() {
        Log.i(TAG, "subscribe called: ")
        musicServiceConnection.subscribe(SMOOTH_ALBUMS_ROOT, subscriptionCallback)

    }
    private val subscriptionCallback = object : MediaBrowserCompat.SubscriptionCallback() {
        //called when there is error in loading the children
        override fun onError(parentId: String) {
            Log.i(TAG, "onError: $parentId")
        }

        //this returns the list of children from the media browser service compact
        override fun onChildrenLoaded(
            parentId: String,
            children: MutableList<MediaBrowserCompat.MediaItem>
        ) {
            Log.i(TAG, "List of Children: $children")
            val lisChildren = children.map { child ->

//                val description = child.description
//                Song(
//                    description.mediaId!!,
//                    description.mediaUri,
//                    description.title.toString(),
//                    isPlaying = false,
//                    albumName = description.extras!!.getString(
//                        MediaMetadataCompat.METADATA_KEY_ALBUM,
//                        "Album not set"
//                    ),
//
//                    artistName = description.subtitle.toString(),
//                    albumArtUri = description.iconUri,
//
//
//                    )
            }
           // _songsList?.postValue(lisChildren)

        }
    }


    class Factory(
        private val application: Application,
        private val musicServiceConnection: MusicServiceConnection
    ) :
        ViewModelProvider.NewInstanceFactory() {
        @Suppress("unchecked_cast")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(VideoFragmentViewModel::class.java)) {
                return VideoFragmentViewModel(application, musicServiceConnection) as T
            }
            throw IllegalArgumentException("Unknown Model Class")
        }
    }
}
private val TAG="VideoFragmentViewModel"