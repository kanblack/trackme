package kb.dev.trackme

import android.location.Location
import com.google.android.gms.maps.model.LatLng

enum class SessionState {
    ACTIVE,
    PAUSE,
    COMPLETE
}

enum class AppEvent{
    PERMISSION_GRANTED
}

const val EXTRA_REQUEST_PAUSE_SESSION = "EXTRA_REQUEST_PAUSE_SESSION"
const val EXTRA_REQUEST_RESUME_SESSION = "EXTRA_REQUEST_RESUME_SESSION"
const val EXTRA_REQUEST_START_SESSION = "EXTRA_REQUEST_START_SESSION"
const val EXTRA_REQUEST_COMPLETE_SESSION = "EXTRA_REQUEST_COMPLETE_SESSION"

const val PREF_KEY_SESSION_STATE = "PREF_KEY_SESSION_STATE"
const val PREF_KEY_PERMISSION_GRANT_STATUS= "PREF_KEY_PERMISSION_GRANT_STATUS"

data class SessionEvent(
    val sessionState: SessionState,
    val distance: Double = 0.0,
    val route: List<LatLng>,
    val startLatLng: LatLng?,
    val lastKnowLocation: Location?,
    val avgSpeed: Double,
    val duration: Double
)

data class MessageEvent(val event: AppEvent)
