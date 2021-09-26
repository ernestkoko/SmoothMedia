package com.koko.smoothmedia.dataclass.room_db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * The Room database
 */
@Database(
    entities =
    [PlayListEntity::class, SongEntity::class, PlaylistSongCrossRef::class],
    version = 1,
    exportSchema = false
)
abstract class PlaylistDatabase : RoomDatabase() {
    //Dao for accessing the database
    abstract val playListDao: PlayListDao

    /*A companion object for accessing the db without instantiating the DB*/
    companion object {
        /*@Volatile makes changes to the db reflect on all threads instantly*/
        @Volatile
        private var INSTANCE: PlaylistDatabase? = null

        /*Method for getting the instance of the db*/
        fun getInstance(context: Context): PlaylistDatabase {
            //allow on thread of execution at a time
            synchronized(this) {
                var instance = INSTANCE
                //check if the instance is null and build a db if null else return already existing
                //instance
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        PlaylistDatabase::class.java,
                        "smooth_media_database"
                    ).fallbackToDestructiveMigration()
                        .build()
                }
                return instance

            }
        }

    }
}