package kb.dev.trackme.map

import android.app.Activity
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import kb.dev.trackme.ImageStorage
import kb.dev.trackme.R
import kb.dev.trackme.SessionEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.ln


class MapManagerImpl(private val imageStorage: ImageStorage) : MapManager {
    private var locationPermissionGranted: Boolean = false
    private var mMap: GoogleMap? = null
    private var mMapToSave: GoogleMap? = null
    private var startLatLng: LatLng? = null
    private var cameraPosition: CameraPosition? = null
    private var session = MutableLiveData<SessionEvent>()
    private var currentPolyline: Polyline? = null

    init {
        EventBus.getDefault().register(this)
    }

    override fun attachMap(activity: Activity, googleMap: GoogleMap) {
        mMap = googleMap
        updateLocationUI()
    }

    override fun attachMapToSave(activity: Activity, googleMap: GoogleMap) {
        mMapToSave = googleMap
    }

    private fun requestUpdateRoute(route: List<LatLng>) {
        currentPolyline?.remove()
        currentPolyline = mMap?.addPolyline(PolylineOptions().add(*route.toTypedArray()))
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: SessionEvent) {
        Log.e("@@@","event")
        if (session.value == null) {
            event.startLatLng?.let { updateUIStartLocation(it) }
        }
        session.postValue(event)
        event.lastKnowLocation?.let { lastLocation ->
            val lastLocationInLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
            moveCamera(lastLocationInLatLng)
        }
        requestUpdateRoute(event.route)
    }


    override fun release() {
        EventBus.getDefault().unregister(this)
    }

    override suspend fun getRoute(): String {
        return suspendCoroutine { ct ->
            val session = session.value
            val route = session?.route ?: listOf()
            val lastKnownLocation = session?.lastKnowLocation
            val startLatLng = session?.startLatLng

            val imageName = System.currentTimeMillis().toString()
            val builder = LatLngBounds.Builder()
            if (route.isEmpty()) {
                ct.resume("")
                return@suspendCoroutine
            }
            route.forEach {
                builder.include(it)
            }

            val bounds = builder.build()

            lastKnownLocation?.let {
                GlobalScope.launch(Dispatchers.Main) {
                    startLatLng?.let { startLatLng ->


                        mMapToSave?.addMarker(
                            MarkerOptions().position(
                                startLatLng
                            )
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_start_marker))
                        )
                        lastKnownLocation?.let { lastKnownLocation ->
                            mMapToSave?.addMarker(
                                MarkerOptions().position(
                                    LatLng(
                                        lastKnownLocation.latitude,
                                        lastKnownLocation.longitude
                                    )
                                )
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_target_flag))
                            )
                        }
                    }
                    mMapToSave?.setLatLngBoundsForCameraTarget(bounds)

                    val loc1 = Location(LocationManager.GPS_PROVIDER).apply {
                        latitude = bounds.northeast.latitude
                        longitude = bounds.northeast.longitude
                    }
                    val loc2 = Location(LocationManager.GPS_PROVIDER).apply {
                        latitude = bounds.southwest.latitude
                        longitude = bounds.southwest.longitude
                    }

                    val radius = loc1.distanceTo(loc2)
                    val scale = radius / 340
                    val zoomLevel = (16 - ln(scale) / ln(2.0)).toFloat()

                    Log.e("zoom level", "zoomLevel: $zoomLevel - radius: $radius")

                    mMapToSave?.moveCamera(
                        CameraUpdateFactory
                            .newLatLngZoom(bounds.center, zoomLevel)
                    )
                    mMapToSave?.addPolyline(PolylineOptions().add(*route.toTypedArray()))
                    delay(1000)
                    mMapToSave?.snapshot { snapshotBitmap ->
                        ct.resume(imageStorage.storeImage(snapshotBitmap, imageName))
                    }
                }
            }
        }

    }

    private fun updateLocationUI() {
        if (mMap == null) {
            return
        }
        try {
//            mMap?.isMyLocationEnabled = true
//            mMap?.uiSettings?.isMyLocationButtonEnabled = true
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun updateUIStartLocation(startLatLng: LatLng) {
        try {
            updateLocationUI()
            moveCamera(startLatLng)
            markStartLocation(startLatLng)
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun markStartLocation(lastKnownLocation: LatLng) {
        mMap?.addMarker(
            MarkerOptions().position(
                lastKnownLocation
            ).icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_start_marker))
        )
    }

    private fun moveCamera(location: LatLng) {
        GlobalScope.launch(Dispatchers.Main) {
            mMap?.moveCamera(
                CameraUpdateFactory
                    .newLatLngZoom(location, DEFAULT_ZOOM.toFloat())
            )
        }
    }

    companion object {
        private const val DEFAULT_ZOOM = 15

        // Keys for storing activity state.
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"
    }
}