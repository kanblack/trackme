package kb.dev.trackme

import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import kb.dev.trackme.utils.convertMeterToKilometer
import java.text.NumberFormat
import java.util.concurrent.TimeUnit

@BindingAdapter("sessionState")
fun setSessionButtonState(imv: ImageView, state: SessionState?) {
    when (state) {
        SessionState.ACTIVE -> {
            imv.setImageResource(R.drawable.ic_pause_circle)
        }
        SessionState.PAUSE -> {
            imv.setImageResource(R.drawable.ic_stop_circle)
        }
        else -> imv.setImageDrawable(null)
    }
}

@BindingAdapter("distance")
fun setDistance(tv: TextView, distanceInMeter: Double) {
    val distanceInKilometers = convertMeterToKilometer(distanceInMeter)
    tv.text = if (distanceInKilometers < 1) {
        "${NumberFormat.getInstance().format(distanceInMeter)} m"
    } else {
        "${NumberFormat.getInstance().format(distanceInKilometers)} km"
    }
}

@BindingAdapter("duration")
fun setDuration(tv: TextView, durationInMills: Double) {
    val hours = TimeUnit.MILLISECONDS.toHours(durationInMills.toLong())
    val minutes =
        TimeUnit.MILLISECONDS.toMinutes(durationInMills.toLong() - (TimeUnit.HOURS.toMillis(hours)))
    val seconds = TimeUnit.MILLISECONDS.toSeconds(
        durationInMills.toLong()
                - (TimeUnit.HOURS.toMillis(hours))
                - (TimeUnit.MINUTES.toMillis(minutes))
    )
    tv.text = tv.context.getString(
        R.string.tv_duration,
        hours.toString().padStart(2, '0'),
        minutes.toString().padStart(2, '0'),
        seconds.toString().padStart(2, '0')
    )
}

@BindingAdapter("velocity")
fun setVelocity(tv: TextView, velocity: Double) {
    val context = tv.context
    tv.text = context.getString(R.string.tv_avg_speed, NumberFormat.getInstance().format(velocity))
}