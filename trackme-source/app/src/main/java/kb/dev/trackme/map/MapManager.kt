package kb.dev.trackme.map

import android.app.Activity
import android.os.Bundle
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
}