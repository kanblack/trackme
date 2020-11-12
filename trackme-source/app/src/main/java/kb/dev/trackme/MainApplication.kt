package kb.dev.trackme

import android.app.Application
import kb.dev.trackme.database.TrackMeRoomDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext.startKoin
import org.koin.core.logger.Level

class MainApplication : Application() {
    @ExperimentalCoroutinesApi
    @FlowPreview
    override fun onCreate() {
        super.onCreate()
        // Start Koin

        startKoin{
            printLogger(Level.DEBUG)
            androidContext(this@MainApplication)
            modules(appModule)
        }
    }
}