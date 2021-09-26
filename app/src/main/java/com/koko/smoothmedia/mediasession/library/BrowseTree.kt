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
    private val albumListOfMap = mutableListOf<Map<String, MediaMetadataCompat>>()
    private lateinit var albumsUniqueNames: MutableList<String>

    init {
        Log.i(TAG, "BrowseTree Init: Called")
        val rootList = mediaIdToChildren[SMOOTH_BROWSABLE_ROOT] ?: mutableListOf()
        val albumMetadata = albumMediaMetadataCompat()

        val allSongsMetadata = mutableListOf<MediaMetadataCompat>()

        musicSource.forEach { mediaItem ->
            rootList += mediaItem


        }
        Log.i(TAG, "All Songs: ${rootList}")
        mediaIdToChildren[SMOOTH_BROWSABLE_ROOT] = rootList
        mediaIdToChildren[SMOOTH_ALBUMS_ROOT] = rootList.buildAlbums().toMutableList()
        albumsUniqueNames.map {

            Log.i(
                TAG,
                "IT: $it, Map: ${albumListOfMap.loopThroughListOfMapAndReturnMatchingItems(it)}"
            )
            mediaIdToChildren[it] = albumListOfMap.loopThroughListOfMapAndReturnMatchingItems(it)
        }


        Log.i(TAG, "Key: ${mediaIdToChildren.keys}")
        // mediaIdToChildren[albumsUniqueNames.getAStringFromStringList()]
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
        //initialise the list of album names
        albumsUniqueNames = albumNames.getAlbumsUniqueNames()
        return this.getAlbumMediaMetadataList(albumsUniqueNames)
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
     * get the list of album media metadata
     */
    private fun MutableList<MediaMetadataCompat>.getAlbumMediaMetadataList(
        specialListOfNames:
        MutableList<String>
    ): MutableList<MediaMetadataCompat> {

        //list of media metadata to return
        val albumMediaMetadataList = mutableListOf<MediaMetadataCompat>()

        //loop through the list of media metadata
        this.mapIndexed { index, mediaMetadataCompat ->
            val listIterator = albumMediaMetadataList.listIterator()
            val mySet = mutableSetOf<MediaMetadataCompat>()
            if (specialListOfNames.contains(mediaMetadataCompat.album) &&
                !albumMediaMetadataList.containsAlbumName(mediaMetadataCompat.album)
            ) {
                Log.i(TAG, "getAlbumMediaMetadataList: FirstCondition")

                val item = MediaMetadataCompat.Builder()
                    .apply {
                        id = index.toString()
                        album = mediaMetadataCompat.album

                        title = mediaMetadataCompat.itemCount.plus(1).toString()
                        itemCount = mediaMetadataCompat.itemCount.plus(1)
                        albumArtUri = mediaMetadataCompat.albumArtUri.toString()
                        downloadStatus = mediaMetadataCompat.itemCount.plus(1)
                        flag = MediaItem.FLAG_BROWSABLE

                    }.build()
                item.description.extras?.putLong(METADATA_KEY_ITEM_COUNT, 1)

                albumMediaMetadataList.add(item)
                mySet.add(item)
            }
            /* if the given list contains the album name and the lis we are populating contains the same media
            * album name */
            else {
                Log.i(TAG, "getAlbumMediaMetadataList: SecondCondition")
                /*  loop through the list we are populating and modify the item using iterator  */





                Log.i(TAG, "getAlbumMediaMetadataList: SecondCondition: albumSame")




                for((index, item) in listIterator.withIndex()){
                    if(item.album == mediaMetadataCompat.album){
                        Log.i(TAG, "AlbumSame")
                        Log.i(TAG, "AlbumSame : Count: ${item.itemCount}")
                        val newMetadata = MediaMetadataCompat.Builder().apply {
                            id = index.toString()
                            album = mediaMetadataCompat.album
                            title = item.itemCount .plus(1).toString()
                            itemCount = item.itemCount .plus(1)
                            downloadStatus = item.itemCount.plus(1)
                            albumArtUri = mediaMetadataCompat.albumArtUri.toString()
                            flag = MediaItem.FLAG_BROWSABLE
                        }.build()
                        mediaMetadataCompat.description.extras?.putLong(
                            METADATA_KEY_ITEM_COUNT,
                            mediaMetadataCompat.itemCount + 1
                        )
                        Log.i(TAG, "AlbumSame : Count: ${newMetadata.itemCount}")
                        //modify the metadata
                        listIterator.set(newMetadata)

                    }

                }


                // albumMediaMetadataList.add(index, newMetadata)


            }


            //check for the unique names and add them to their respective album
            if (specialListOfNames.contains(mediaMetadataCompat.album)) {

                albumListOfMap.add(mapOf(mediaMetadataCompat.album!! to mediaMetadataCompat))

            }


        }
        albumMediaMetadataList.map {
            Log.i(
                TAG, "Final: ${it.itemCount}:" +
                        "NAme: ${it.title}"
            )
        }
        return albumMediaMetadataList
    }

    private fun MutableListIterator<MediaMetadataCompat>.containsString(s: String): Boolean {
        val list = this.asSequence().toMutableList()
        return list.containsAlbumName(s)
    }

    /**
     * check if a string is contained in a list of strings and returns the contained string if true
     */
    private fun MutableList<String>.getAStringFromStringList(str: String): String {
        var newString = ""
        this.map {
            if (it == str) {
                newString = it
            }
        }
        return newString
    }


    /**
     * Loop through the list of maps of albums and return the [MediaMetadataCompat]s that have matching
     * keys with [albumName]
     */
    private fun MutableList<Map<String, MediaMetadataCompat>>.loopThroughListOfMapAndReturnMatchingItems(
        albumName: String
    ): MutableList<MediaMetadataCompat> {
        val list = mutableListOf<MediaMetadataCompat>()
        this.map {
            if (it.containsKey(albumName)) {
                list.add(it.getValue(albumName))
            }

        }
        Log.i(TAG, "loopThroughListOfMapAndReturnMatchingItems: List: ${list[0].album}")
        return list
    }

    /**
     * Check if a string is contained in a list of metadata.
     */
    private fun MutableList<MediaMetadataCompat>.containsAlbumName(string: String?): Boolean {
        val names = this.map {
            it.album
        }
        return names.contains(string)

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

