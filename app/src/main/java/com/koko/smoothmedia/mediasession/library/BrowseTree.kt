package com.koko.smoothmedia.mediasession.library

import android.content.Context
import android.media.browse.MediaBrowser
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import com.koko.smoothmedia.R
import com.koko.smoothmedia.mediasession.extension.*

/**
 * Represents a tree of media that's used by [MusicService.onLoadChildren].
 *
 * [BrowseTree] maps a media id (see: [MediaMetadataCompat.METADATA_KEY_MEDIA_ID]) to one (or
 * more) [MediaMetadataCompat] objects, which are children of that media id.
 *
 * For example, given the following conceptual tree:
 * root
 *  +-- Albums
 *  |    +-- Album_A
 *  |    |    +-- Song_1
 *  |    |    +-- Song_2
 *  ...
 *  +-- Artists
 *  ...
 *
 *  Requesting `browseTree["root"]` would return a list that included "Albums", "Artists", and
 *  any other direct children. Taking the media ID of "Albums" ("Albums" in this example),
 *  `browseTree["Albums"]` would return a single item list "Album_A", and, finally,
 *  `browseTree["Album_A"]` would return "Song_1" and "Song_2". Since those are leaf nodes,
 *  requesting `browseTree["Song_1"]` would return null (there aren't any children of it).
 */
class BrowseTree(
    val context: Context,
    musicSource: MusicSource,
    val recentMediaId: String? = null
) {
    private val mediaIdToChildren = mutableMapOf<String, MutableList<MediaMetadataCompat>>()

    init {
        Log.i(TAG, "BrowseTree Init: Called")

        val rootList = mediaIdToChildren[SMOOTH_BROWSABLE_ROOT] ?: mutableListOf()
        val recommendedMetadata = recommendedMediaMetadataCompat()
        val albumMetadata = albumMediaMetadataCompat()

//        rootList += recommendedMetadata
//        rootList += albumMetadata
        //declare variable to return all songs
        val allSongsMetadata = mutableListOf<MediaMetadataCompat>()

        //mediaIdToChildren[SMOOTH_BROWSABLE_ROOT] = rootList
        musicSource.forEach { mediaItem ->
            //Log.i(TAG, "MediaItem: ${mediaItem}")
           // allSongsMetadata.add(mediaItem)

            rootList +=mediaItem
            //Log.i(TAG, "MediaItem: ${rootList}")

//            val albumMediaId = mediaItem.album.urlEncoded
//            val albumChildren = mediaIdToChildren[albumMediaId] ?: buildAlbumRoot(mediaItem)
//            albumChildren += mediaItem
//            // Add the first track of each album to the 'Recommended' category
//            if (mediaItem.trackNumber == 1L) {
//                val recommendedChildren = mediaIdToChildren[SMOOTH_RECOMMENDED_ROOT]
//                    ?: mutableListOf()
//                recommendedChildren += mediaItem
//                mediaIdToChildren[SMOOTH_RECOMMENDED_ROOT] = recommendedChildren
//            }
//
//            // If this was recently played, add it to the recent root.
//            if (mediaItem.id == recentMediaId) {
//                mediaIdToChildren[SMOOTH_RECENT_ROOT] = mutableListOf(mediaItem)
//            }

        }
        Log.i(TAG, "All Songs: ${rootList}")
        mediaIdToChildren[SMOOTH_BROWSABLE_ROOT]= rootList
    }

    private fun albumMediaMetadataCompat() = MediaMetadataCompat.Builder().apply {
        id = SMOOTH_ALBUMS_ROOT
        title = context.getString(R.string.album_title)
        albumArtUri = RESOURCE_ROOT_URI +
                context.resources.getResourceEntryName(R.drawable.ic_album)
        flag = MediaBrowser.MediaItem.FLAG_BROWSABLE
    }.build()

    private fun recommendedMediaMetadataCompat() = MediaMetadataCompat.Builder().apply {
        id = SMOOTH_RECOMMENDED_ROOT
        title = context.getString(R.string.recommended_title)
        albumArtUri = RESOURCE_ROOT_URI +
                context.resources.getResourceEntryName(R.drawable.ic_recommended)
        flag = MediaItem.FLAG_BROWSABLE
    }.build()

    /**
     * Provide access to the list of children with the `get` operator.
     * i.e.: `browseTree\[UAMP_BROWSABLE_ROOT\]`
     */
    operator fun get(mediaId: String) = mediaIdToChildren[mediaId]

    /**
     * Builds a node, under the root, that represents an album, given
     * a [MediaMetadataCompat] object that's one of the songs on that album,
     * marking the item as [MediaItem.FLAG_BROWSABLE], since it will have child
     * node(s) AKA at least 1 song.
     */
    private fun buildAlbumRoot(mediaItem: MediaMetadataCompat): MutableList<MediaMetadataCompat> {
        val albumMetadata = MediaMetadataCompat.Builder().apply {
            id = mediaItem.album.urlEncoded
            title = mediaItem.album
            artist = mediaItem.artist
            albumArt = mediaItem.albumArt
           // albumArtUri = mediaItem.albumArtUri.toString()
            flag = MediaItem.FLAG_BROWSABLE
        }.build()

        // Adds this album to the 'Albums' category.
        val rootList = mediaIdToChildren[SMOOTH_ALBUMS_ROOT] ?: mutableListOf()
        rootList += albumMetadata
        mediaIdToChildren[SMOOTH_ALBUMS_ROOT] = rootList

        // Insert the album's root with an empty list for its children, and return the list.
        return mutableListOf<MediaMetadataCompat>().also {
            mediaIdToChildren[albumMetadata.id!!] = it
        }
    }
}

const val SMOOTH_BROWSABLE_ROOT = "/"
const val SMOOTH_EMPTY_ROOT = "@empty@"
const val SMOOTH_RECOMMENDED_ROOT = "__RECOMMENDED__"
const val SMOOTH_ALBUMS_ROOT = "__ALBUMS__"
const val SMOOTH_RECENT_ROOT = "__RECENT__"

const val MEDIA_SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"

const val RESOURCE_ROOT_URI = "android.resource://com.example.android.uamp.next/drawable/"
private val TAG ="BrowseTree"

