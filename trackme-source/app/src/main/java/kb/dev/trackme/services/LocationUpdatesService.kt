package kb.dev.trackme.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
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


class LocationUpdatesService : Service() {
    private val sharedPreferences: SharePreferenceUtils by inject()
    private var currentSpeed: Double = 0.0
    private var avgSpeed: Double = 0.0
    private val route = arrayListOf<LatLng>()
    private var distance = 0.0
    private var startAt: Long = 0
    private var velocityCount = 0
    private var totalVelocity = 0.0
    private var duration = 0.0

    private var sessionState: SessionState? = null
    private var startLatLng: LatLng? = null
    private var lastKnownLocation: Location? = null
    private var mLocationRequest: LocationRequest

    private var backupJob: Job? = null
    private var updateVelocityJob: Job? = null
    private var timerJob: Job? = null

    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)
            Log.e(TAG, "onLocationResult")
            onNewLocation(locationResult?.lastLocation)
        }
    }

    init {
        mLocationRequest = createLocationRequest()
    }

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "create")
        try {
            getNotification()?.let {
                startForeground(1, it)
            }
            loadBackUpState()
            mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadBackUpState() {
        val backupSession = sharedPreferences.getLastSessionBackup()
        val lastState = sharedPreferences.getLastSessionSate()
        if (lastState == null || lastState == SessionState.COMPLETE.toString()) {
            Log.e(TAG, "no back up $lastState")
            return
        }
        backupSession?.let {
            duration = it.duration
            currentSpeed = it.currentSpeed
            avgSpeed = it.avgSpeed
            distance = it.distance
            startAt = it.startAt
            velocityCount = it.velocityCount
            totalVelocity = it.totalVelocity
            duration = it.duration
            startLatLng = it.startLatLng?.let { startLatLng ->
                LatLng(startLatLng[0], startLatLng[1])
            }
            lastKnownLocation = it.lastKnownLocation?.let { lastKnownLocation ->
                Location(LocationManager.GPS_PROVIDER).apply {
                    latitude = lastKnownLocation[0]
                    longitude = lastKnownLocation[1]
                }
            }
            route.addAll(it.route.map { latlgn ->
                LatLng(latlgn[0], latlgn[1])
            })
        }
        Log.e(TAG, "back up complete")
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
                .setContentTitle("Your workout session is active")
                .setSmallIcon(R.drawable.ic_speed)
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
        Log.e(TAG, "onStartCommand")
        try {
            val type = intent.getStringExtra("type")
            onStartSession(type)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return START_NOT_STICKY
    }

    private fun backUpStateJob() = GlobalScope.launch {
        while (sessionState != SessionState.COMPLETE) {
            if (sessionState == SessionState.ACTIVE) {
                backupState()
            }
            delay(INTERVAL_BACKUP_JOB)
        }
    }

    private fun backupState() {
        val routeBackup = route.map {
            arrayOf(it.latitude, it.longitude)
        }

        val startLatLngBackup = startLatLng?.let { arrayOf(it.latitude, it.longitude) }
        val lastKnownLocationBackup =
            lastKnownLocation?.let { arrayOf(it.latitude, it.longitude) }

        val backup = Gson().toJson(
            BackupSession(
                currentSpeed,
                avgSpeed,
                routeBackup,
                distance,
                startAt,
                velocityCount,
                totalVelocity,
                duration,
                startLatLngBackup,
                lastKnownLocationBackup
            )
        )
        sharedPreferences.saveLastSessionBackup(backup)
        Log.e(TAG, "backup complete")
    }

    private fun onStartSession(type: String?) {
        try {
            when (type) {
                EXTRA_REQUEST_START_SESSION -> {
                    onRequestStartSession()
                }
                EXTRA_REQUEST_PAUSE_SESSION -> {
                    backupState()
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
                    clearBackUp()
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
        } catch (e: Exception) {
            onStartSession(EXTRA_REQUEST_PAUSE_SESSION)
            e.printStackTrace()
        }

    }

    private fun clearBackUp() {
        sharedPreferences.saveLastSessionBackup(null)
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
        getLastLocation()
    }

    private fun startJob() {
        if (backupJob == null || backupJob!!.isCancelled) {
            backupJob = backUpStateJob()
        }
        if (updateVelocityJob == null || updateVelocityJob!!.isCancelled) {
            updateVelocityJob = updateVelocity()
        }
        if (timerJob == null || timerJob!!.isCancelled) {
            timerJob = startTimer()
        }
    }

    private fun cancelJob() {
        timerJob?.cancel()
        backupJob?.cancel()
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
        try {
            mFusedLocationProviderClient?.removeLocationUpdates(mLocationCallback)
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission. Could not remove updates. $unlikely")
        }
    }

    private fun updateVelocity() = GlobalScope.launch {
        var lastDistance = 0.0
        var lastDuration = 0.0
        while (sessionState != SessionState.COMPLETE) {
            if (sessionState == SessionState.ACTIVE) {
                currentSpeed = getVelocity(duration - lastDuration, distance - lastDistance)
                lastDistance = distance
                lastDuration = duration
                totalVelocity += currentSpeed
                if (totalVelocity > 0) {
                    velocityCount++
                }
                val currentAvgSpeed =
                    totalVelocity / if (velocityCount == 0) 1 else velocityCount
                avgSpeed = currentAvgSpeed
            }
            delay(INTERVAL_UPDATE_VELOCITY_JOB)
        }
    }

    private fun startTimer() = GlobalScope.launch {
        while (sessionState != SessionState.COMPLETE) {
            if (sessionState == SessionState.ACTIVE) {
                duration += INTERVAL_UPDATE_TIME_JOB
                notifySessionToApp()
            }
            delay(INTERVAL_UPDATE_TIME_JOB)
        }
    }

    private fun onNewLocation(location: Location?) {
        location?.let {
            try {
                if (lastKnownLocation == null) {
                    lastKnownLocation = location
                }
                if (startLatLng == null) {
                    startLatLng = LatLng(it.latitude, it.longitude)
                    route.add(LatLng(it.latitude, it.longitude))
                }
                val distanceToLastLocation = it.distanceTo(lastKnownLocation).toDouble()
                if (distanceToLastLocation > MINIMUM_DISTANCE_IN_METER) {
                    lastKnownLocation = location
                    distance += distanceToLastLocation
                    val lastLocationInLatLng = LatLng(location.latitude, location.longitude)
                    route.add(lastLocationInLatLng)
                    notifySessionToApp()
                }
            } catch (e: Exception) {
                e.printStackTrace()
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
                        duration,
                        currentSpeed
                    )
                }
            )
    }

    companion object {
        private const val MINIMUM_DISTANCE_IN_METER = 3
        private const val ONE_SECOND_IN_MILLS = 1000L
        private const val UPDATE_INTERVAL_IN_MILLISECONDS = 1 * ONE_SECOND_IN_MILLS
        private const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2
        private const val INTERVAL_BACKUP_JOB = 3 * ONE_SECOND_IN_MILLS
        private const val INTERVAL_UPDATE_VELOCITY_JOB = 5 * ONE_SECOND_IN_MILLS
        private const val INTERVAL_UPDATE_TIME_JOB = 500L
        private val TAG = LocationUpdatesService::class.java.simpleName
    }
}