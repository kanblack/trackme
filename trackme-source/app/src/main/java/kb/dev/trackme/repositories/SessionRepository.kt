package kb.dev.trackme.repositories

import kb.dev.trackme.mvvm.Session

interface SessionRepository {
    fun startNewSession(): Session
    fun stopSession(sessionId: String)
    fun pauseSession(sessionId: String)
    fun getSessions(offset: Int, limit: Int)
    fun saveNewSession(distance: Double, duration: Double, avgVelocity: Double)
}