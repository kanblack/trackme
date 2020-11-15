package kb.dev.trackme

import android.location.Location
import com.google.android.gms.maps.model.LatLng

enum class SessionState {
    ACTIVE,
    PAUSE,
    COMPLETE
}

const val EXTRA_REQUEST_PAUSE_SESSION = "EXTRA_REQUEST_PAUSE_SESSION"
const val EXTRA_REQUEST_RESUME_SESSION = "EXTRA_REQUEST_RESUME_SESSION"
const val EXTRA_REQUEST_START_SESSION = "EXTRA_REQUEST_START_SESSION"
const val EXTRA_REQUEST_COMPLETE_SESSION = "EXTRA_REQUEST_COMPLETE_SESSION"

const val PREF_KEY_SESSION_STATE = "PREF_KEY_SESSION_STATE"
const val PREF_KEY_PERMISSION_GRANT_STATUS= "PREF_KEY_PERMISSION_GRANT_STATUS"
const val PREF_KEY_PERMISSION_SESSION_BACKUP= "PREF_KEY_PERMISSION_SESSION_BACKUP"

data class SessionEvent(
    val sessionState: SessionState,
    val distance: Double = 0.0,
    val route: List<LatLng>,
    val startLatLng: LatLng?,
    val lastKnowLocation: Location?,
    val avgSpeed: Double,
    val duration: Double,
    val currentSpeed: Double
)


data class BackupSession(
    val currentSpeed: Double = 0.0,
    val avgSpeed: Double = 0.0,
    val route: List<Array<Double>>,
    val distance: Double = 0.0,
    val startAt: Long = 0,
    val velocityCount: Int = 0,
    val totalVelocity: Double = 0.0,
    val duration: Double = 0.0,
    val startLatLng: Array<Double>?,
    val lastKnownLocation: Array<Double>?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BackupSession

        if (currentSpeed != other.currentSpeed) return false
        if (avgSpeed != other.avgSpeed) return false
        if (route != other.route) return false
        if (distance != other.distance) return false
        if (startAt != other.startAt) return false
        if (velocityCount != other.velocityCount) return false
        if (totalVelocity != other.totalVelocity) return false
        if (duration != other.duration) return false
        if (startLatLng != null) {
            if (other.startLatLng == null) return false
            if (!startLatLng.contentEquals(other.startLatLng)) return false
        } else if (other.startLatLng != null) return false
        if (lastKnownLocation != null) {
            if (other.lastKnownLocation == null) return false
            if (!lastKnownLocation.contentEquals(other.lastKnownLocation)) return false
        } else if (other.lastKnownLocation != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = currentSpeed.hashCode()
        result = 31 * result + avgSpeed.hashCode()
        result = 31 * result + route.hashCode()
        result = 31 * result + distance.hashCode()
        result = 31 * result + startAt.hashCode()
        result = 31 * result + velocityCount
        result = 31 * result + totalVelocity.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + (startLatLng?.contentHashCode() ?: 0)
        result = 31 * result + (lastKnownLocation?.contentHashCode() ?: 0)
        return result
    }
}