package kb.dev.trackme.repositories

interface SessionRepository {
    fun startNewSession()
    fun stopSession(sessionId: String)
    fun pauseSession(sessionId: String)
    fun getSessions(offset: Int, limit: Int)

}