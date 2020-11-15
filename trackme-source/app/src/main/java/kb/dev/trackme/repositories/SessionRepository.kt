package kb.dev.trackme.repositories

import androidx.paging.PagingSource
import kb.dev.trackme.database.Session

interface SessionRepository {
    fun getSessions(): PagingSource<Int, Session>
    suspend fun saveNewSession(session: Session)
}