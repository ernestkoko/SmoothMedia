package com.koko.smoothmedia.mediasession.services

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val TAG = "PersistenceStorage"

class PersistentStorage private constructor(val context: Context) {

    /**
     * Store any data which must persist between restarts, such as the most recently played song.
     */
    private var preferences: SharedPreferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        //ensure one instance is provided

        @Volatile
        private var instance: PersistentStorage? = null

        fun getInstance(context: Context) =
            instance ?: synchronized(this) {
                instance ?: PersistentStorage(context).also { instance = it }
            }



    }

    suspend fun saveRecentSong(description: MediaDescriptionCompat, position: Long) {
        Log.i(TAG, "saveRecentSong: Called")

        withContext(Dispatchers.IO) {

            /**
             * After booting, Android will attempt to build static media controls for the most
             * recently played song. Artwork for these media controls should not be loaded
             * from the network as it may be too slow or unavailable immediately after boot. Instead
             * we convert the iconUri to point to the Glide on-disk cache.
             */
            /**
             * After booting, Android will attempt to build static media controls for the most
             * recently played song. Artwork for these media controls should not be loaded
             * from the network as it may be too slow or unavailable immediately after boot. Instead
             * we convert the iconUri to point to the Glide on-disk cache.
             */
//            val localIconUri = Glide.with(context).asFile().load(description.iconUri)
//               // .submit(NOTIFICATION_LARGE_ICON_SIZE, NOTIFICATION_LARGE_ICON_SIZE).get()
//                .asAlbumArtContentUri()

            preferences.edit()
                .putString(RECENT_SONG_MEDIA_ID_KEY, description.mediaId)
                .putString(RECENT_SONG_TITLE_KEY, description.title.toString())
                .putString(RECENT_SONG_SUBTITLE_KEY, description.subtitle.toString())
                // .putString(RECENT_SONG_ICON_URI_KEY, localIconUri.toString())
                .putLong(RECENT_SONG_POSITION_KEY, position)
                .apply()
        }
    }

    fun loadRecentSong(): MediaBrowserCompat.MediaItem? {
        Log.i(TAG, "loadRecentSong: Called")
        val mediaId = preferences.getString(RECENT_SONG_MEDIA_ID_KEY, null)
        return if (mediaId == null) {
            null
        } else {
            val extras = Bundle().also {
                val position = preferences.getLong(RECENT_SONG_POSITION_KEY, 0L)
                it.putLong(MEDIA_DESCRIPTION_EXTRAS_START_PLAYBACK_POSITION_MS, position)
            }

            MediaBrowserCompat.MediaItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId(mediaId)
                    .setTitle(preferences.getString(RECENT_SONG_TITLE_KEY, ""))
                    .setSubtitle(preferences.getString(RECENT_SONG_SUBTITLE_KEY, ""))
                    // .setIconUri(Uri.parse(preferences.getString(RECENT_SONG_ICON_URI_KEY, "")))
                    .setExtras(extras)
                    .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            )
        }
    }

    /**
     * Save the present tab before navigating
     */


     fun savePresentTab(num: Int) {

            preferences.edit().putInt(PRESENT_TAB_KEY, num).apply()


    }

    /**
     * Get the last tab that was saved
     */
    fun getPresentTab(): Int {
        return preferences.getInt(PRESENT_TAB_KEY, 0)
    }

    /**
     * Load playlists
     */
    fun loadPlayLists() {

    }

    suspend fun createPlaylist(name: String, mediaItemTitle: String) {
        val set = preferences.getStringSet(PLAYLIST_KEY, mutableSetOf())
        withContext(Dispatchers.IO) {
            //  preferences.edit().putStringSet(PLAYLIST_KEY ,set?.add(name!!)).apply()
        }


    }
//    fun getListOfPlaylist(list:List<String>): List<String>{
//        preferences.getStringSet()
//
//    }

}

private const val PRESENT_TAB_KEY = "present_tab_key"
private const val PLAYLIST_KEY = "PLAYLIST_KEY"
private const val PREFERENCES_NAME = "smooth_media"
private const val RECENT_SONG_MEDIA_ID_KEY = "recent_song_media_id"
private const val RECENT_SONG_TITLE_KEY = "recent_song_title"
private const val RECENT_SONG_SUBTITLE_KEY = "recent_song_subtitle"
private const val RECENT_SONG_ICON_URI_KEY = "recent_song_icon_uri"
private const val RECENT_SONG_POSITION_KEY = "recent_song_position"