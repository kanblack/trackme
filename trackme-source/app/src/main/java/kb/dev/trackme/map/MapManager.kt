package kb.dev.trackme.map

import android.app.Activity
import android.os.Bundle
import androidx.lifecycle.LiveData
import com.google.android.gms.maps.GoogleMap

interface MapManager {
    fun attachMap(activity: Activity, googleMap: GoogleMap)

    fun updateDatePermissionResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    )

    fun saveMapState(outState: Bundle)

    fun restoreMapState(savedInstanceState: Bundle)

    fun requestLocationUpdate()

    fun getDistance(): LiveData<Double>
    fun stopUpdateLocation()
}