package com.koko.smoothmedia.dataclass.room_db

import androidx.room.*

/**
 * Entity for the playlist table.
 * I am auto-generating the [playlistId] the moment it is created
 * [playListName] is the name of the playlist that the user will see
 */
@Entity(tableName = "play_list_table")
data class PlayListEntity(
    @PrimaryKey val playlistId: Long,
    val playListName: String,
    val playListImageUrl: String?,

    )

/**
 * Entity for the song table.
 * I am saving the song's mediaId as the primary key so i can easily fetch the song from the list of Media
 * Since the media id is unique for each song
 */
@Entity(tableName = "song_table")
data class SongEntity(
    @PrimaryKey val songMediaId: String
)

/**
 * An associative entity for the playlist and song entities
 * [primaryKeys] is [playListId]and [songMediaId] of the two children tables.
 * Also delcare the two primary keys as the fields of this table
 */
@Entity(primaryKeys = ["playlistId", "songMediaId"])
data class PlaylistSongCrossRef(
    val playListId: Long,
    val songMediaId: String
)

/**
 * [PlaylistWithSongs] is a class that defines the relationship with the two tables of interest
 * [SongEntity] and [PlayListEntity]. Since [PlayListEntity] is @Embedded, An instance of [PlayListEntity] will fetch a number of
 * [SongEntity]
 * The relationship if defined with @Relation. The parentColumn is [PlayListEntity.playlistId] and
 * entityColumn is [SongEntity.songMediaId] while they are associatedBY [PlaylistSongCrossRef] table
 */
data class PlaylistWithSongs(
    @Embedded val playList: PlayListEntity,
    @Relation(
        parentColumn = "playlistId",
        entityColumn = "songMediaId",
        associateBy = Junction(PlaylistSongCrossRef::class)
    )
    val songs: List<SongEntity>
)

