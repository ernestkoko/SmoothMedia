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
    val genre: String ?=null
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