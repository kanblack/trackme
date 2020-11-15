package kb.dev.trackme.common

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import kb.dev.trackme.*
import kb.dev.trackme.mvvm.BackupSession

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

    fun saveLastSessionBackup(backup: String?) {
        if (backup == null) {
            sharedPreferences.edit().remove(PREF_KEY_PERMISSION_SESSION_BACKUP).apply()
            return
        }
        sharedPreferences.edit()
            .putString(PREF_KEY_PERMISSION_SESSION_BACKUP, backup).apply()
    }

    fun getLastSessionBackup(): BackupSession? {
        return try {
            sharedPreferences.getString(PREF_KEY_PERMISSION_SESSION_BACKUP, null)
                ?.let { Gson().fromJson(it, BackupSession::class.java) }
        } catch (e: Exception) {
            null
        }
    }
}