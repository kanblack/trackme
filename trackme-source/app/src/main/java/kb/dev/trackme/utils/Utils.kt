package kb.dev.trackme.utils

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
    return distanceInKilometer / max(durationByHour, 1.0)
}