package kb.dev.trackme.map

import com.google.android.gms.maps.SupportMapFragment

interface MapManager {
    fun attachMap(mapFragment: SupportMapFragment)
}