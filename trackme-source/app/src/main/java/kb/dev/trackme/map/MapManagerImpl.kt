package kb.dev.trackme.map

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

class MapManagerImpl : MapManager {
    private var mMap: GoogleMap? = null
    private var cameraPosition: CameraPosition? = null
    private val defaultLocation = LatLng(21.027763, 105.834160)
    private var locationPermissionGranted = false
    private var lastKnownLocation: Location? = null
    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null


    override fun attachMap(activity: Activity, googleMap: GoogleMap) {
        mMap = googleMap
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)

        getLocationPermission(activity)

        updateLocationUI(activity)

        getDeviceLocation(activity)
    }

    override fun updateDatePermissionResult(
        activity: Activity,
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true
                }
            }
        }
        updateLocationUI(activity)
    }

    override fun saveMapState(outState: Bundle) {
        mMap?.let { map ->
            outState.putParcelable(KEY_CAMERA_POSITION, map.cameraPosition)
            outState.putParcelable(KEY_LOCATION, lastKnownLocation)
        }
    }

    override fun restoreMapState(savedInstanceState: Bundle) {
        lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
        cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
    }

    private fun updateLocationUI(activity: Activity) {
        if (mMap == null) {
            return
        }
        try {
            if (locationPermissionGranted) {
                mMap?.isMyLocationEnabled = true
                mMap?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                mMap?.isMyLocationEnabled = false
                mMap?.uiSettings?.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                getLocationPermission(activity)
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun getLocationPermission(activity: Activity) {
        if (ContextCompat.checkSelfPermission(
                activity.applicationContext, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(
                activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private fun getDeviceLocation(activity: Activity) {
        try {
            if (locationPermissionGranted) {
                val locationResult = mFusedLocationProviderClient?.lastLocation
                locationResult?.addOnCompleteListener(activity) { task ->
                    if (task.isSuccessful) {
                        lastKnownLocation = task.result
                        lastKnownLocation?.let {
                            moveCamera(LatLng(it.latitude, it.longitude))
                        }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.")
                        Log.e(TAG, "Exception: %s", task.exception)
                        moveCamera(defaultLocation)
                        mMap?.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun moveCamera(location: LatLng) {
        mMap?.moveCamera(
            CameraUpdateFactory
                .newLatLngZoom(location, DEFAULT_ZOOM.toFloat())
        )
    }

    companion object {
        private val TAG = MapManager::class.java.simpleName
        private const val DEFAULT_ZOOM = 15
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1

        // Keys for storing activity state.
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"
    }
}