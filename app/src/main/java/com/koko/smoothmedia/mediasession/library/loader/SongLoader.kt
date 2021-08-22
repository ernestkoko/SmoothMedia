package com.koko.smoothmedia.mediasession.library.loader

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Audio.AudioColumns
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import androidx.annotation.NonNull
import com.koko.smoothmedia.dataclass.Song
import com.koko.smoothmedia.mediasession.extension.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

val BASE_PROJECTION = arrayOf(
    BaseColumns._ID,// 0
    AudioColumns.TITLE,// 1
    AudioColumns.TRACK,// 2
    AudioColumns.YEAR,// 3
    AudioColumns.DURATION,// 4
    AudioColumns.DATA,// 5
    AudioColumns.DATE_MODIFIED,// 6
    AudioColumns.ALBUM_ID,// 7
    AudioColumns.ALBUM,// 8
    AudioColumns.ARTIST_ID,// 9
    AudioColumns.ARTIST,// 10
)
private val TAG = "SongLoader"

class SongLoader {


    companion object {

        @Throws(IOException::class)
        suspend fun getAllSongs(context: Context): List<Song> {
            val songs = mutableListOf<Song>()
            withContext(Dispatchers.IO) {
//                context.contentResolver.query(
//                    MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, null,null,null,null
//                )
               // printPlayList(context)

                context.contentResolver.query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    null,
                    null,
                    null, null
                )?.use { cursor ->
                   // val playListCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.PLAYLIST_ID)

                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                    val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                    val trackNumberCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                    val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                    val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                    val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                   //al playlistsCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.

                    val dateModifiedCol =
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
                    val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                    val albumNameCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                    } else {
                        cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_KEY)
                    }
                    val artistIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                    val artistNameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                    while (cursor.moveToNext()) {
                        //val playListId = cursor.getLong(playListCol)

                        val id = cursor.getLong(idColumn)
                        val title = cursor.getString(titleColumn)
                        val trackNumber = cursor.getLong(trackNumberCol)
                        val year = cursor.getLong(yearCol)
                        val duration = cursor.getLong(durationCol)
                        val data = cursor.getString(dataColumn)
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
                            data,
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
            Log.i(TAG, "$songs")

            return songs
        }



        @NonNull
        fun getSongs(@NonNull cursor: Cursor): ArrayList<Song> {
            val songs = ArrayList<Song>()
            if (cursor.moveToFirst()) {
                do {
                    // songs.add(getSongFromCursorImpl(cursor))
                } while (cursor.moveToNext())
            }
            if (!cursor.isClosed) {
                cursor.close()
            }

            return songs

        }

//        @NonNull
//        fun getSOng(@NonNull cursor: Cursor): Song {
//            val song = if (cursor.moveToFirst()) {
//              //  getSongFromCursorImpl(cursor)
//            } else {
//                Song()
//
//            }
//            if (!cursor.isClosed) {
//                //close the cursor
//                cursor.close()
//            }
//
//            return song
//
//        }

//        @NonNull
//        private fun getSongFromCursorImpl(@NonNull cursor: Cursor): Song {
//            val id: Int = cursor.getInt(0)
//            val title: String = cursor.getString(1)
//            val trackNumber: Int = cursor.getInt(3)
//            val year: Int = cursor.getInt(4)
//            val duration: Long = cursor.getLong(5)
//            val data: String = cursor.getString(6)
//            val dateModified: Long = cursor.getLong(7)
//            val albumId: Int = cursor.getInt(8)
//            val albumName: String = cursor.getString(9)
//            val artistId: Int = cursor.getInt(10)
//            val artistName: String = cursor.getString(11)
//            return Song(
//                id,
//                title,
//                trackNumber,
//                year,
//                duration,
//                data,
//                dateModified,
//                albumId,
//                albumName,
//                artistId,
//                artistName
//            )
//
//        }

//        fun makeSongCursor(
//            @NonNull context: Context,
//            selection: String,
//            selectionValues: List<String>, sortOrder: String
//        ): Cursor? {
//            return try {
//                context.contentResolver.query(
//                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, BASE_PROJECTION,
//                    null, null, null
//                )
//
//            } catch (e: Exception) {
//
//                e.printStackTrace()
//                null
//            }
//
//
//        }
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