package com.koko.smoothmedia.dataclass

import android.net.Uri
import androidx.recyclerview.widget.DiffUtil
import androidx.versionedparcelable.VersionedParcelize
import java.util.*

@VersionedParcelize
data class SongData(
    val id: Long,
    val contentUri: Uri,
    val title: String?,
    val dateAdded: Date?,
    val displayName: String?,
    val duration: Long?,
    val genre: String? = null
) {
    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<SongData>() {
            override fun areItemsTheSame(oldItem: SongData, newItem: SongData): Boolean {
                return oldItem === newItem
            }

            override fun areContentsTheSame(oldItem: SongData, newItem: SongData): Boolean {
                return oldItem.id == newItem.id

            }

        }
    }
}
/**
 * data class for metadata that is browsable
 */
//data class MediaItemData(
//    val mediaId: String,
//    val title: String,
//    val subtitle: String,
//    val albumArtUri: Uri,
//    val browsable: Boolean,
//    var playbackRes: Int
//)

/**
 * if [isPlaying] is 0 it means it is not playing but when it is 1 it is playing
 */

data class Song(
    val id: String = "",
    val uri: Uri?=null,
    val title: String = "",
    val trackNumber: Long = -1,
    val year: Long = -1,
    val duration: Long = -1,
    val data: String = "",
    val dateModified: Long = -1,
    val albumId: Long = -1,
    val isPlaying:Boolean? =false,
    val albumArtUri: Uri? =null,
    val albumName: String = "",
    val itemCount: Long=0,
    val artistId: Long = -1,
    val artistName: String = ""
)


