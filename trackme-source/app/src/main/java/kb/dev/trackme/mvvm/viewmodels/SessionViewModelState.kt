package kb.dev.trackme.mvvm.viewmodels

import android.app.Activity
import com.google.android.gms.maps.GoogleMap

interface SessionViewModelState {
    fun onAttachMap(activity: Activity, googleMap: GoogleMap)

    fun onActionButtonClicked()

    fun onResumeButtonClicked()

    fun onAttachMapToSave(activity: Activity, googleMap: GoogleMap)

    fun onPermissionResult(status: Boolean)
}