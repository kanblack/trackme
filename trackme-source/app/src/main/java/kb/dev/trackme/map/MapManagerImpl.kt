package kb.dev.trackme.map

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.*
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
    private val distance = MutableLiveData(0.0)

    override fun attachMap(activity: Activity, googleMap: GoogleMap) {
        mMap = googleMap

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(activity)

        getLocationPermission(activity)

        updateLocationUI(activity)

        getDeviceLocation(activity)

        requestLocationUpdate()
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

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)
            onNewLocation(locationResult?.lastLocation)
        }
    }


    private fun onNewLocation(lastLocation: Location?) {
        lastLocation?.let {
            distance.postValue(distance.value?.plus(it.distanceTo(lastKnownLocation).toDouble()))
            lastKnownLocation = lastLocation
        }
    }

    override fun requestLocationUpdate() {
        Log.e(TAG, "Requesting location updates")
//        Utils.setRequestingLocationUpdates(this, true)
//        startService(
//            Intent(
//                ApplicationProvider.getApplicationContext<Context>(),
//                LocationUpdatesService::class.java
//            )
//        )
        try {
            mFusedLocationProviderClient?.requestLocationUpdates(
                createLocationRequest(),
                mLocationCallback, Looper.myLooper()
            )
        } catch (unlikely: SecurityException) {
//            Utils.setRequestingLocationUpdates(this, false)
            unlikely.printStackTrace()
            Log.e(TAG, "Lost location permission. Could not request updates. $unlikely")
        }
    }

    override fun getDistance(): LiveData<Double> {
        return distance
    }

    override fun stopUpdateLocation() {
        mFusedLocationProviderClient?.removeLocationUpdates(mLocationCallback)
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

    private fun createLocationRequest(): LocationRequest {
        val locationRequest = LocationRequest()
        locationRequest.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        locationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        return locationRequest
    }

    companion object {
        private val TAG = MapManager::class.java.simpleName
        private const val DEFAULT_ZOOM = 15
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
        private const val UPDATE_INTERVAL_IN_MILLISECONDS = 10000L
        private const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2

        // Keys for storing activity state.
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"
    }
}