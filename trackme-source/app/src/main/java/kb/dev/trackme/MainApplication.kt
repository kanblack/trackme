package kb.dev.trackme

import android.app.Application
import org.koin.core.context.GlobalContext.startKoin

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Start Koin
        startKoin{
            modules(appModule)
        }
    }
}