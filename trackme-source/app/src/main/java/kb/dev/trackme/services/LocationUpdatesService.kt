package kb.dev.trackme.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import kb.dev.trackme.*
import kb.dev.trackme.R
import kb.dev.trackme.utils.SharePreferenceUtils
import kb.dev.trackme.utils.getVelocity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit


class LocationUpdatesService : Service() {
    private val sharedPreferences: SharePreferenceUtils by inject()

    private var avgSpeed: Double = 0.0
    private val route = arrayListOf<LatLng>()
    private var distance = 0.0
    private var startLatLng: LatLng? = null
    private var lastKnownLocation: Location? = null
    private var startAt: Long = 0
    private var velocityCount = 0
    private var totalVelocity = 0.0
    private var sessionState: SessionState? = null
    private var duration = 0.0
    private lateinit var mLocationRequest: LocationRequest

    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)
            onNewLocation(locationResult?.lastLocation)
        }
    }

    init {
        mLocationRequest = createLocationRequest()
    }

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "onCreate")
        getNotification()?.let {
            startForeground(1, it)
        }
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun getNotification(): Notification? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    createNotificationChannel()
                } else {
                    // If earlier version channel ID is not used
                    // https://developer.android.com/reference/android/support/v4/app/NotificationCompat.Builder.html#NotificationCompat.Builder(android.content.Context)
                    ""
                }
            val pendingIntent: PendingIntent =
                Intent(this, LocationUpdatesService::class.java).let { notificationIntent ->
                    PendingIntent.getActivity(this, 0, notificationIntent, 0)
                }

            return Notification.Builder(this, channelId)
                .setContentTitle("Request update location")
                .setContentText("Workout recording ...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pendingIntent)
                .setTicker("ticker")
                .build()
        }

        return null
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(
        channelId: String = "trackme",
        channelName: String = "location tracking"
    ): String {
        val chan = NotificationChannel(
            channelId,
            channelName, NotificationManager.IMPORTANCE_NONE
        )
        chan.lightColor = Color.BLUE
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        val type = intent.getStringExtra("type")
        onStartSession(type)
        return START_STICKY
    }

    private fun onStartSession(type: String?) {
        when (type) {
            EXTRA_REQUEST_START_SESSION -> {
                onRequestStartSession()
            }
            EXTRA_REQUEST_PAUSE_SESSION -> {
                requestStopLocationUpdates()
                sessionState = SessionState.PAUSE
                cancelJob()
            }
            EXTRA_REQUEST_RESUME_SESSION -> {
                requestLocationUpdates()
                sessionState = SessionState.ACTIVE
                startJob()
            }
            EXTRA_REQUEST_COMPLETE_SESSION -> {
                sessionState = SessionState.COMPLETE
                cancelJob()
                requestStopLocationUpdates()
                stopForeground(true)
                stopSelf()
            }
            else -> {
                if (sessionState == null) {
                    restart()
                }
                notifySessionToApp()
            }
        }
        sessionState?.let { sharedPreferences.saveActiveSession(it) }
    }

    private fun onRequestStartSession() {
        startSession()
        sessionState = SessionState.ACTIVE
        startJob()
    }

    private fun restart() {
        val type = when (sharedPreferences.getLastSessionSate()) {
            SessionState.ACTIVE.toString() -> EXTRA_REQUEST_RESUME_SESSION
            SessionState.PAUSE.toString() -> EXTRA_REQUEST_RESUME_SESSION
            else -> EXTRA_REQUEST_START_SESSION
        }
        onStartSession(type)
    }

    private fun startJob() {
        if (updateVelocityJob == null || updateVelocityJob!!.isCancelled) {
            updateVelocityJob = updateVelocity()
        }
        if (timerJob == null || timerJob!!.isCancelled) {
            timerJob = startTimer()
        }
    }

    private fun cancelJob() {
        timerJob?.cancel()
        updateVelocityJob?.cancel()
    }

    private fun startSession() {
        startAt = System.currentTimeMillis()
        getLastLocation()
        requestLocationUpdates()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun getLastLocation() {
        try {
            mFusedLocationProviderClient?.lastLocation?.addOnCompleteListener { task ->
                if (task.isSuccessful && task.result != null) {
                    lastKnownLocation = task.result
                    startLatLng = LatLng(task.result.latitude, task.result.longitude)
                    route.add(LatLng(task.result.latitude, task.result.longitude))
                    Log.e(TAG, "getLastLocation success")
                } else {
                    Log.e(TAG, "Failed to get location.")
                }
            }
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission.$unlikely")
        }
    }

    private fun createLocationRequest(): LocationRequest {
        val locationRequest = LocationRequest()
        locationRequest.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        locationRequest.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        return locationRequest
    }


    private fun requestLocationUpdates() {
        Log.e(TAG, "Requesting location updates")
        try {
            mFusedLocationProviderClient?.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback, Looper.myLooper()
            )
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission. Could not request updates. $unlikely")
        }
    }

    private fun requestStopLocationUpdates() {
        Log.i(TAG, "Removing location updates")
        try {
            mFusedLocationProviderClient?.removeLocationUpdates(mLocationCallback)
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission. Could not remove updates. $unlikely")
        }
    }

    var updateVelocityJob: Job? = null
    var timerJob: Job? = null

    private fun updateVelocity() = GlobalScope.launch {
        while (sessionState != SessionState.COMPLETE) {
            if (sessionState == SessionState.ACTIVE) {
                totalVelocity += getVelocity(duration, distance)
                if (totalVelocity > 0) {
                    velocityCount++
                }
                val currentAvgSpeed = totalVelocity / if (velocityCount == 0) 1 else velocityCount
                avgSpeed = currentAvgSpeed
                delay(TimeUnit.SECONDS.toMillis(10))
            }
        }
    }

    private fun startTimer() = GlobalScope.launch {
        val timerIntervalInMills = 500L
        while (sessionState != SessionState.COMPLETE) {
            if (sessionState == SessionState.ACTIVE) {
                duration += timerIntervalInMills
                notifySessionToApp()
                delay(timerIntervalInMills)
            }
        }
    }

    private fun onNewLocation(location: Location?) {
        location?.let {
            if (lastKnownLocation == null) {
                lastKnownLocation = location
            }
            val distanceToLastLocation = it.distanceTo(lastKnownLocation).toDouble()
            if (distanceToLastLocation > MINIMUM_DISTANCE) {
                lastKnownLocation = location
                distance += distanceToLastLocation
                val lastLocationInLatLng = LatLng(location.latitude, location.longitude)
                route.add(lastLocationInLatLng)
                notifySessionToApp()
                Log.e(TAG, "${route.size}")
            }
        }
    }

    private fun notifySessionToApp() {
        EventBus.getDefault()
            .post(
                sessionState?.let {
                    SessionEvent(
                        it,
                        distance,
                        route,
                        startLatLng,
                        lastKnownLocation,
                        avgSpeed,
                        duration
                    )
                }
            )
    }


    companion object {
        private const val MINIMUM_DISTANCE = 5
        private const val UPDATE_INTERVAL_IN_MILLISECONDS = 1000L
        private const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2
        private val TAG = LocationUpdatesService::class.java.simpleName
    }
}