package com.koko.smoothmedia.mediasession.library.loader

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import com.koko.smoothmedia.dataclass.Song
import com.koko.smoothmedia.mediasession.extension.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException


private val TAG = "SongLoader"

/**
 * A class that fetches the Audio media files from the local database of the device
 * It does the fetching the IO Thread
 */

class SongLoader {


    companion object {
        @Throws(IOException::class)
        suspend fun getAllSongs(context: Context): List<Song> {
            val songs = mutableListOf<Song>()
            withContext(Dispatchers.IO) {
                /* Declare the collection*/
                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                /* Declare the projection*/
                val projection =
                    arrayOf(
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.TRACK,
                        MediaStore.Audio.Media.YEAR,
                        MediaStore.Audio.Media.DURATION,
                        MediaStore.Audio.Media.DATE_MODIFIED,
                        MediaStore.Audio.Media.ALBUM_ID,
                        MediaStore.Audio.Media.ALBUM,
                        MediaStore.Audio.Media.ARTIST_ID,
                        MediaStore.Audio.Media.ARTIST
                    )

                context.contentResolver.query(
                    collection,
                    projection,
                    null,
                    null, null
                )?.use { cursor ->
                    // Cache column indices.

                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val trackNumberCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                    val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    //al playlistsCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.

                    val dateModifiedCol =
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                    val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val albumNameCol =
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)

                    val artistIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                    val artistNameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    while (cursor.moveToNext()) {
                        //val playListId = cursor.getLong(playListCol)

                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn)
                        val trackNumber = cursor.getLong(trackNumberCol)
                        val year = cursor.getLong(yearCol)
                        val duration = cursor.getLong(durationCol)
                        val dateModified = cursor.getLong(dateModifiedCol)
                        val albumId = cursor.getLong(albumIdCol)
                        val albumName = cursor.getString(albumNameCol)
                        val artistId = cursor.getLong(artistIdCol)
                        val artistName = cursor.getString(artistNameCol)
                        //get the content uri
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id
                        )

                        Log.i(TAG, "Content Uri: $contentUri")
                        // Log.i(TAG, "Playlist Uri: $playListId")

                        val song = Song(
                            id.toString(),
                            contentUri,
                            title,
                            trackNumber,
                            year,
                            duration,
                            "Data",
                            dateModified,
                            albumId,
                            albumName,
                            artistId,
                            artistName
                        )
                        songs.add(song)

                    }


                }

            }
            return songs
        }


    }


}

fun MediaMetadataCompat.Builder.from(song: Song): MediaMetadataCompat.Builder {
    id = song.id
    mediaUri = song.uri.toString()
    title = song.title
    trackNumber = song.trackNumber
    duration = song.duration
    artist = song.artistName
    downloadStatus = MediaDescriptionCompat.STATUS_NOT_DOWNLOADED
    return this

}