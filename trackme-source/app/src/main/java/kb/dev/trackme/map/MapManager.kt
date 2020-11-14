package kb.dev.trackme.map

import android.app.Activity
import android.os.Bundle
import androidx.lifecycle.LiveData
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng

interface MapManager {
    fun attachMap(activity: Activity, googleMap: GoogleMap)
    fun attachMapToSave(activity: Activity, googleMap: GoogleMap)
    fun release()
    suspend fun getRouteImage(): String
}