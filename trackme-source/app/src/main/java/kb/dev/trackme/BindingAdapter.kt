package kb.dev.trackme

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import kb.dev.trackme.utils.convertMeterToKilometer
import kb.dev.trackme.utils.getDurationFormatted
import java.text.NumberFormat
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

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

@BindingAdapter("hide")
fun hideView(view: View, isHide: Boolean) {
    view.visibility = if (isHide) View.GONE else View.VISIBLE
}

@BindingAdapter("distance")
fun setDistance(tv: TextView, distanceInMeter: Double) {
    val distanceInKilometers = convertMeterToKilometer(distanceInMeter)
    tv.text = if (distanceInKilometers < 1) {
        "${NumberFormat.getInstance().format(distanceInMeter.roundToInt())} m"
    } else {
        "${NumberFormat.getInstance().format(distanceInKilometers)} km"
    }
}

@BindingAdapter("duration")
fun setDuration(tv: TextView, durationInMills: Double) {
    tv.text = getDurationFormatted(tv.context, durationInMills.toLong())
}

@BindingAdapter("velocity")
fun setVelocity(tv: TextView, velocity: Double) {
    val context = tv.context
    tv.text = context.getString(R.string.tv_avg_speed, NumberFormat.getInstance().format(velocity))
}