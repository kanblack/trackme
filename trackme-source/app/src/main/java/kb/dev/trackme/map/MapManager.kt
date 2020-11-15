package kb.dev.trackme.map

import android.app.Activity
import com.google.android.gms.maps.GoogleMap

interface MapManager {
    fun attachMap(activity: Activity, googleMap: GoogleMap)
    fun attachMapToSave(activity: Activity, googleMap: GoogleMap)
    fun release()
    suspend fun getRouteImage(): String
}