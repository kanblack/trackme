package kb.dev.trackme.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Session::class], version = 1, exportSchema = false)
abstract class TrackMeRoomDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: TrackMeRoomDatabase? = null

        fun getDatabase(context: Context): TrackMeRoomDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TrackMeRoomDatabase::class.java,
                    "trackme_database"
                ).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}
