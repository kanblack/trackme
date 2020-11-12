package kb.dev.trackme.utils

import android.util.Log
import java.util.concurrent.TimeUnit
import kotlin.math.max

fun convertMillisToHour(durationInMills: Long): Double {
    return durationInMills / TimeUnit.HOURS.toMillis(1).toDouble()
}

fun convertMeterToKilometer(meter: Double): Double {
    return meter / 1000
}

fun getVelocity(durationInMills: Double, distanceInMeter: Double): Double {
    val durationByHour = convertMillisToHour(durationInMills.toLong())

    val distanceInKilometer = convertMeterToKilometer(distanceInMeter)
    val velocity = distanceInKilometer /( if (durationByHour == 0.0) 1.0 else durationByHour)
    return velocity
}