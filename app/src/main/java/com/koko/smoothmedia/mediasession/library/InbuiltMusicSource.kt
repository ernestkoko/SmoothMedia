package com.koko.smoothmedia.mediasession.library

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import com.koko.smoothmedia.mediasession.library.loader.SongLoader
import com.koko.smoothmedia.mediasession.library.loader.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class InbuiltMusicSource : AbstractMusicSource() {
    private var catalog: List<MediaMetadataCompat> = emptyList()

    init {
        state = STATE_INITIALIZING
    }

    override fun iterator(): Iterator<MediaMetadataCompat> = catalog.iterator()

    override suspend fun load(context: Context) {
        updateCatalog(context)?.let {
            catalog= it
            state = STATE_INITIALIZED

        } ?: run {
            catalog = emptyList()
            state = STATE_ERROR
        }
    }

    suspend fun updateCatalog(context: Context): List<MediaMetadataCompat>? {
        return withContext(Dispatchers.IO) {

            val musciCat = try {
                SongLoader.getAllSongs(context)
            } catch (e: IOException) {
                return@withContext null
            }

            val mediaMetadataCompacts = musciCat.map {
                MediaMetadataCompat.Builder().from(it).build()

            }.toList()
            mediaMetadataCompacts.forEach {
                it.description.extras?.putAll(it.bundle)
            }
            mediaMetadataCompacts


        }
    }

}