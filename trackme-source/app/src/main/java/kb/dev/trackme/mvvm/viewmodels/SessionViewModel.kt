package kb.dev.trackme.mvvm.viewmodels

import android.app.Activity
import android.os.Bundle
import androidx.lifecycle.ViewModel
import com.google.android.gms.maps.GoogleMap
import kb.dev.trackme.map.MapManager
import kb.dev.trackme.repositories.SessionRepository

class SessionViewModel(repository: SessionRepository, private val mapManager: MapManager): ViewModel() {
    fun onAttachMap(activity: Activity, googleMap: GoogleMap) {
        mapManager.attachMap( activity,googleMap)
    }

    fun onRequestPermissionsResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        mapManager.updateDatePermissionResult(activity, requestCode, permissions, grantResults)
    }

    fun onSaveMapState(outState: Bundle) {
        mapManager.saveMapState(outState)

    }

    fun onRestoreMapState(savedInstanceState: Bundle) {
        mapManager.restoreMapState(savedInstanceState)
    }
}