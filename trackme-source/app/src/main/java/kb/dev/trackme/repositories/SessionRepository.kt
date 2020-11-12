package kb.dev.trackme.repositories

import androidx.paging.PagingSource
import kb.dev.trackme.database.Session

interface SessionRepository {
    fun stopSession(sessionId: String)
    fun pauseSession(sessionId: String)
    fun getSessions(): PagingSource<Int, Session>
    suspend fun saveNewSession(session: Session)
}