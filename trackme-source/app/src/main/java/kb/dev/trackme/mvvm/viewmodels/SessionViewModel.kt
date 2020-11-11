package kb.dev.trackme.mvvm.viewmodels

import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kb.dev.trackme.map.MapManager
import kb.dev.trackme.repositories.SessionRepository

class SessionViewModel(repository: SessionRepository, private val mapManager: MapManager): ViewModel() {

    fun attachMap(mapFragment: SupportMapFragment) {
        mapManager.attachMap(mapFragment)

    }
}