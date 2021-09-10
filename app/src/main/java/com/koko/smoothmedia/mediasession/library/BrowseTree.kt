package com.koko.smoothmedia.mediasession.library

import android.content.Context
import android.media.browse.MediaBrowser
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
        val albumMetadata = albumMediaMetadataCompat()

        val allSongsMetadata = mutableListOf<MediaMetadataCompat>()

        musicSource.forEach { mediaItem ->
            rootList += mediaItem


        }
        Log.i(TAG, "All Songs: ${rootList}")
        mediaIdToChildren[SMOOTH_ALBUMS_ROOT] = rootList.buildAlbums().toMutableList()
        mediaIdToChildren[SMOOTH_BROWSABLE_ROOT] = rootList
    }

    /**
     * Provide access to the list of children with the `get` operator.
     * i.e.: `browseTree\[UAMP_BROWSABLE_ROOT\]`
     */
    operator fun get(mediaId: String) = mediaIdToChildren[mediaId]

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
     * Build album children
     */
    private fun MutableList<MediaMetadataCompat>.buildAlbums(): List<MediaMetadataCompat> {
        Log.i(TAG, "buildAlbumChildren(): Called")
        //get the all the album names including duplicate
        val albumNames = this.map { it.album }
        val albumsUniqueNames = albumNames.getAlbumsUniqueNames()
        return this.getAlbumMediaMetadataList(albumsUniqueNames)
    }

    /**
     * Check if a string is contained in a list of metadata.
     */
    private fun MutableList<MediaMetadataCompat>.contains(string: String?): Boolean {
        val names = this.map {
            it.album
        }
        return names.contains(string)

    }

    /**
     * get the list of album media metadata
     */
    private fun MutableList<MediaMetadataCompat>.getAlbumMediaMetadataList(specialListOfNames: MutableList<String>):
            MutableList<MediaMetadataCompat> {
        val albumMediaMetadataList = mutableListOf<MediaMetadataCompat>()
        mapIndexed { index, mediaMetadataCompat ->
            if (specialListOfNames.contains(mediaMetadataCompat.album) &&
                !albumMediaMetadataList.contains(mediaMetadataCompat.album)
            ) {
                val item = MediaMetadataCompat.Builder().apply {
                    id = index.toString()
                    album = mediaMetadataCompat.album
                    title = mediaMetadataCompat.title
                    albumArtUri = mediaMetadataCompat.albumArtUri.toString()
                    flag = MediaItem.FLAG_BROWSABLE
                }.build()
                albumMediaMetadataList.add(item)
            }

        }
        return albumMediaMetadataList
    }

    /**
     * loop through a list and check if a string appears more than once. If it does add just one
     * appearance of the string to a new list and return the new list
     */
    private fun List<String?>.getAlbumsUniqueNames(): MutableList<String> {
        val specialListOfNames = mutableListOf<String>()
        this.mapIndexed { index, s ->
            if (index == 0) {
                specialListOfNames.add(s!!)
            } else {
                if (specialListOfNames.contains(s!!)) {

                } else {
                    specialListOfNames.add(s)
                }
            }

        }
        return specialListOfNames
    }


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
private val TAG = "BrowseTree"

