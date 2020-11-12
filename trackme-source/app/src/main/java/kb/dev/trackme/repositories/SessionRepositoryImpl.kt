package kb.dev.trackme.repositories

import kb.dev.trackme.mvvm.Session

class SessionRepositoryImpl : SessionRepository{
    private val sessions = hashMapOf<Long, Session>()

    override fun startNewSession(): Session {
       return addNewSession()
    }

    private fun addNewSession(): Session {
        val newSession = Session()
        val sessionId = newSession.id
        sessions[sessionId] = newSession
        return newSession
    }

    override fun stopSession(sessionId: String) {
        TODO("Not yet implemented")
    }

    override fun pauseSession(sessionId: String) {
        TODO("Not yet implemented")
    }

    override fun getSessions(offset: Int, limit: Int) {
        TODO("Not yet implemented")
    }

    override fun saveNewSession(distance: Double, duration: Double, avgVelocity: Double) {
    }

}