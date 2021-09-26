package com.koko.smoothmedia.dataclass.room_db

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * The database access object for the Play list entity
 */
@Dao
interface PlayListDao {
    /*get playlists with songs*/
    /**
     * Get all the playlists and all the songs under them
     */
    @Query("SELECT * FROM play_list_table")
    fun getPlaylistsAndSongs(): LiveData<List<PlaylistWithSongs>>

    /**
     * Get a playlist with all the songs under it.
     */
    @Query("SELECT * FROM play_list_table WHERE playlistId = :key")
    fun getAPlaylistAndSongs(key: Long): PlaylistWithSongs

    /*Get all the playlists in the table*/
    @Query("SELECT * FROM play_list_table ORDER BY playlistId ASC")
    fun getAllPlayLists(): LiveData<List<PlayListEntity>>

    /*get a single playlist from the table that has the id matching with the supplied key or return null*/
    @Query("SELECT * FROM play_list_table WHERE playlistId = :key")
    fun getPlaylist(key: Long): PlayListEntity

    /*Save a playlist to the db*/
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun savePlaylist(playlist: PlayListEntity)

    /*Update a particular playlist in the db*/
    @Update
    fun updatePlaylist(playlist: PlayListEntity)

    /*save a number 0f playlists in the dp*/
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun savePlaylists(vararg playlists: PlayListEntity)

    /*Delete one or more playlists from the table*/
    @Query("DELETE FROM play_list_table WHERE playlistId = :key ")
    fun deleteAPlaylist(key: PlayListEntity): Int


}