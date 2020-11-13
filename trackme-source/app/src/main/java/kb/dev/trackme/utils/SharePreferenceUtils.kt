package kb.dev.trackme.utils

import android.content.Context
import android.content.SharedPreferences
import kb.dev.trackme.PREF_KEY_PERMISSION_GRANT_STATUS
import kb.dev.trackme.PREF_KEY_SESSION_STATE
import kb.dev.trackme.SessionState

class SharePreferenceUtils(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(context.packageName, Context.MODE_PRIVATE)

    fun saveActiveSession(state: SessionState) {
        sharedPreferences.edit()
            .putString(PREF_KEY_SESSION_STATE, state.toString()).apply()
    }

    fun getLastSessionSate(): String? {
        return sharedPreferences
            .getString(PREF_KEY_SESSION_STATE, null)
    }

    fun saveGrantPermissionStatus(isGranted: Boolean) {
        sharedPreferences.edit()
            .putBoolean(PREF_KEY_PERMISSION_GRANT_STATUS, isGranted).apply()
    }

    fun getGrantPermissionStatus(): Boolean {
        return sharedPreferences.getBoolean(PREF_KEY_PERMISSION_GRANT_STATUS, false)
    }
}