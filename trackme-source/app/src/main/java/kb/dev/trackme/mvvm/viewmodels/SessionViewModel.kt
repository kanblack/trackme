package kb.dev.trackme.mvvm.viewmodels

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.GoogleMap
import kb.dev.trackme.SessionEvent
import kb.dev.trackme.SessionState
import kb.dev.trackme.SingleLiveEvent
import kb.dev.trackme.database.Session
import kb.dev.trackme.map.MapManager
import kb.dev.trackme.repositories.SessionRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

@ExperimentalCoroutinesApi
@FlowPreview
class SessionViewModel(
    private val repository: SessionRepository,
    private val mapManager: MapManager
) :
    ViewModel(), SessionViewModelState {
     val isPermissionGranted = MutableLiveData(true)
    val sessionState = MutableLiveData(SessionState.ACTIVE)
    val distance = MutableLiveData(0.0)
    val duration = MutableLiveData(0.0)//millisecond
    val currentSpeed = MutableLiveData(0.0)
    private val avgSpeed = MutableLiveData(0.0)

    private val saveSessionCompleteEvent = SingleLiveEvent<Boolean>()
    private val isSavingSession = MutableLiveData(false)
    fun getSaveSessionCompleteEvent(): SingleLiveEvent<Boolean> {
        return saveSessionCompleteEvent
    }

    init {
        EventBus.getDefault().register(this)
        viewModelScope.launch(Dispatchers.Default) {
            sessionState.asFlow().collect { state: SessionState? ->
                when (state) {
                    SessionState.COMPLETE -> {
                        isSavingSession.postValue(true)
                        saveSession()
                        releaseCurrentSession()
                        isSavingSession.postValue(false)
                    }
                    SessionState.ACTIVE -> {
                        requestLocationUpdate()
                    }
                    SessionState.PAUSE -> {
                        requestStopLocationUpdate()
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        EventBus.getDefault().unregister(this)
        mapManager.release()
    }

    private fun requestStopLocationUpdate() {
    }

    private fun requestLocationUpdate() {
    }


    private fun releaseCurrentSession() {
        saveSessionCompleteEvent.postValue(true)
    }

    private suspend fun saveSession() {
        val distance = distance.value ?: 0.0
        val duration = duration.value ?: 0.0
        val avgVelocity = avgSpeed.value ?: 0.0
        repository.saveNewSession(
            Session(
                avgSpeed = avgVelocity,
                createdAt = System.currentTimeMillis(),
                distance = distance.toLong(),
                duration = duration.toLong(),
                route = mapManager.getRouteImage()
            )
        )
    }

    override fun onAttachMap(activity: Activity, googleMap: GoogleMap) {
        mapManager.attachMap(activity, googleMap)
    }

    override fun onActionButtonClicked() {
        val updateState = when (sessionState.value) {
            SessionState.ACTIVE -> SessionState.PAUSE
            SessionState.PAUSE -> SessionState.COMPLETE
            else -> SessionState.PAUSE
        }
        updateSessionState(updateState)
    }

    override fun onResumeButtonClicked() {
        if (sessionState.value != SessionState.ACTIVE) {
            updateSessionState(SessionState.ACTIVE)
        }
    }

    private fun updateSessionState(currentState: SessionState?) {
        sessionState.postValue(
            currentState
        )
    }

    override fun onAttachMapToSave(activity: Activity, googleMap: GoogleMap) {
        mapManager.attachMapToSave(activity, googleMap)
    }

    override fun onPermissionResult(status: Boolean) {
        isPermissionGranted.postValue(status)
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)
    fun onMessageEvent(event: SessionEvent) {
        distance.postValue(event.distance)
        currentSpeed.postValue(event.currentSpeed)
        avgSpeed.postValue(event.avgSpeed)
        duration.postValue(event.duration)
        sessionState.postValue(event.sessionState)
    }
}