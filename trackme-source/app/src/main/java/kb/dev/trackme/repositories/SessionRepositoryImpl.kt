package kb.dev.trackme.repositories

import android.util.Log
import androidx.annotation.WorkerThread
import androidx.paging.PagingSource
import kb.dev.trackme.database.Session
import kb.dev.trackme.database.SessionDao

class SessionRepositoryImpl(private val sessionDao: SessionDao) : SessionRepository{
    override fun stopSession(sessionId: String) {
        TODO("Not yet implemented")
    }

    override fun pauseSession(sessionId: String) {
        TODO("Not yet implemented")
    }

    override fun getSessions(): PagingSource<Int, Session> {
        return sessionDao.getSession()
    }

    @WorkerThread
    override suspend fun saveNewSession(session: Session ) {
        val id =  sessionDao.insert(session)

        Log.e("Save session complete","$id")
    }
}