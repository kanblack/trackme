package kb.dev.trackme.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Session")
class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long,
    val duration: Long = 0,
    val avgSpeed: Double = 0.0,
    val distance: Long = 0,
    val route: String?
) {
    override fun equals(other: Any?): Boolean {
        if (other is Session) {
            return id == other.id
                    && createdAt == other.id
                    && avgSpeed == other.avgSpeed
                    && distance == other.distance
                    && duration == other.duration
                    && route == other.route
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + avgSpeed.hashCode()
        result = 31 * result + distance.hashCode()
        result = 31 * result + (route?.hashCode() ?: 0)
        return result
    }
}