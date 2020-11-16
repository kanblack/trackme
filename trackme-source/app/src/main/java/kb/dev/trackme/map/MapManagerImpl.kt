package kb.dev.trackme.map

import android.app.Activity
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import kb.dev.trackme.common.ImageStorage
import kb.dev.trackme.R
import kb.dev.trackme.common.SharePreferenceUtils
import kb.dev.trackme.common.toLatLgn
import kb.dev.trackme.mvvm.SessionEvent
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.lang.Exception
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.ln

class MapManagerImpl(
    private val imageStorage: ImageStorage,
    private val sharePreferenceUtils: SharePreferenceUtils
) : MapManager {
    private var mMap: GoogleMap? = null
    private var mMapToSave: GoogleMap? = null
    private var startLatLng: LatLng? = null
    private var session = MutableLiveData<SessionEvent>()
    private var currentPolyline: Polyline? = null

    init {
        EventBus.getDefault().register(this)
    }

    override fun attachMap(activity: Activity, googleMap: GoogleMap) {
        mMap = googleMap
        mMap?.uiSettings?.apply {
            setAllGesturesEnabled(false)
        }
        updateLocationUI()
        startLatLng?.let {
            moveCamera(it)
            markStartLocation(it)
        }

    }

    override fun attachMapToSave(activity: Activity, googleMap: GoogleMap) {
        mMapToSave = googleMap
    }

    private fun requestUpdateRoute(route: List<LatLng>) {
        currentPolyline?.remove()
        currentPolyline = mMap?.addPolyline(PolylineOptions().apply {
            add(*route.toTypedArray())
            width(POLYLINE_WIDTH)
            color(POLYLINE_COLOR)
            jointType(JointType.ROUND)
        })
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: SessionEvent) {
        if (startLatLng == null) {
            startLatLng = event.startLatLng
            startLatLng?.let { updateUIStartLocation(it) }
        }
        session.postValue(event)
        event.lastKnowLocation?.let { lastLocation ->
            val lastLocationInLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)
            moveCamera(lastLocationInLatLng)
        }
        updateLocationUI()
        requestUpdateRoute(event.route)
    }

    override fun release() {
        EventBus.getDefault().unregister(this)
    }

    override suspend fun getRouteImage(): String = withContext(Dispatchers.Main) {
        return@withContext suspendCoroutine { ct ->
            val session = session.value
            val route = session?.route ?: listOf()
            if (route.isEmpty() || route.size == 1) {
                ct.resumeWithException(Exception("No route"))
                return@suspendCoroutine
            }

            session?.lastKnowLocation?.let { lastKnownLocation ->
                val bounds = getRouteBoundLatLgn(route)
                val zoomLevel = getOverallZoomLevel(bounds)

                mMapToSave?.apply {
                    moveCamera(CameraUpdateFactory.newLatLngZoom(bounds.center, zoomLevel))
                    addPolyline(PolylineOptions().apply {
                        add(*route.toTypedArray())
                        width(POLYLINE_WIDTH)
                        color(POLYLINE_COLOR)
                        jointType(JointType.ROUND)
                    })
                    addMarker(
                        MarkerOptions().apply {
                            startLatLng?.let { position(it) }
                            icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_start_marker))
                        }
                    )
                    addMarker(
                        MarkerOptions().apply {
                            position(lastKnownLocation.toLatLgn())
                            icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_target_flag))
                        }
                    )
                }

                GlobalScope.launch(Dispatchers.IO) {
                    delay(1000)
                    ct.resume(getMapSnapshot())
                }
            }
        }
    }

    private suspend fun getMapSnapshot(): String {
        return suspendCoroutine { ct ->
            mMapToSave?.snapshot { snapshotBitmap ->
                GlobalScope.launch(Dispatchers.IO) {
                    ct.resume(
                        imageStorage.storeImage(
                            snapshotBitmap,
                            System.currentTimeMillis().toString()
                        )
                    )
                }
            }
        }
    }

    private fun getOverallZoomLevel(bounds: LatLngBounds): Float {
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
        return (16 - ln(scale) / ln(2.0)).toFloat()
    }

    private fun getRouteBoundLatLgn(route: List<LatLng>): LatLngBounds {
        val builder = LatLngBounds.Builder()
        route.forEach {
            builder.include(it)
        }
        return builder.build()
    }

    private fun updateLocationUI() {
        if (mMap == null) {
            return
        }
        try {
            if (sharePreferenceUtils.getGrantPermissionStatus()) {
                mMap?.isMyLocationEnabled = true
                mMap?.uiSettings?.isMyLocationButtonEnabled = false
            } else {
                mMap?.isMyLocationEnabled = false
                mMap?.uiSettings?.isMyLocationButtonEnabled = false
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun updateUIStartLocation(startLatLng: LatLng) {
        GlobalScope.launch(Dispatchers.Main) {
            try {
                updateLocationUI()
                moveCamera(startLatLng)
                markStartLocation(startLatLng)
            } catch (e: SecurityException) {
                Log.e("Exception: %s", e.message, e)
            }
        }
    }

    private fun markStartLocation(startLatLng: LatLng) {
        mMap?.addMarker(
            MarkerOptions().position(startLatLng).apply {
                icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_start_marker))
            }
        )
    }

    private fun moveCamera(location: LatLng) {
        GlobalScope.launch(Dispatchers.Main) {
            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, DEFAULT_ZOOM))
        }
    }

    companion object {
        private const val POLYLINE_WIDTH = 14f
        private val POLYLINE_COLOR = Color.parseColor("#0747A6")
        private const val DEFAULT_ZOOM = 17f
    }
}