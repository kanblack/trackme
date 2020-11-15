package kb.dev.trackme.common

import android.content.Context
import kb.dev.trackme.R
import java.util.concurrent.TimeUnit

fun convertMillisToHour(durationInMills: Long): Double {
    return durationInMills / TimeUnit.HOURS.toMillis(1).toDouble()
}

fun convertMeterToKilometer(meter: Double): Double {
    return meter / 1000
}

fun getVelocity(durationInMills: Double, distanceInMeter: Double): Double {
    val durationByHour = convertMillisToHour(durationInMills.toLong())
    val distanceInKilometer = convertMeterToKilometer(distanceInMeter)
    return distanceInKilometer / (if (durationByHour == 0.0) 1.0 else durationByHour)
}

fun getDurationFormatted(context: Context, durationInMills: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(durationInMills)
    val minutes =
        TimeUnit.MILLISECONDS.toMinutes(durationInMills - (TimeUnit.HOURS.toMillis(hours)))
    val seconds = TimeUnit.MILLISECONDS.toSeconds(
        durationInMills
                - (TimeUnit.HOURS.toMillis(hours))
                - (TimeUnit.MINUTES.toMillis(minutes))
    )
    return context.getString(
        R.string.tv_duration,
        hours.toString().padStart(2, '0'),
        minutes.toString().padStart(2, '0'),
        seconds.toString().padStart(2, '0')
    )
}