package kb.dev.trackme.repositories

import androidx.annotation.WorkerThread
import androidx.paging.PagingSource
import kb.dev.trackme.database.Session
import kb.dev.trackme.database.SessionDao

class SessionRepositoryImpl(private val sessionDao: SessionDao) : SessionRepository{
    override fun getSessions(): PagingSource<Int, Session> {
        return sessionDao.getSession()
    }

    @WorkerThread
    override suspend fun saveNewSession(session: Session ) {
        sessionDao.insert(session)
    }
}