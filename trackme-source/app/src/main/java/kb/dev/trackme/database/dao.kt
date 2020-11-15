package kb.dev.trackme.database

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.IGNORE
import androidx.room.Query

@Dao
interface SessionDao {
    @Query("SELECT * FROM session ORDER BY createdAt DESC")
    fun getSession(): PagingSource<Int, Session>

    @Insert(onConflict = IGNORE)
    suspend fun insert(session: Session): Long
}
