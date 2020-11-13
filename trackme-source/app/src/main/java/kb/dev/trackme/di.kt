package kb.dev.trackme

import kb.dev.trackme.database.TrackMeRoomDatabase
import kb.dev.trackme.map.MapManager
import kb.dev.trackme.map.MapManagerImpl
import kb.dev.trackme.mvvm.viewmodels.SessionViewModel
import kb.dev.trackme.mvvm.viewmodels.SessionsHistoryViewModel
import kb.dev.trackme.repositories.SessionRepository
import kb.dev.trackme.repositories.SessionRepositoryImpl
import kb.dev.trackme.utils.SharePreferenceUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

@ExperimentalCoroutinesApi
@FlowPreview
val appModule = module {
    viewModel { SessionViewModel(get(), get()) }
    viewModel { SessionsHistoryViewModel(get()) }

    factory<MapManager> { MapManagerImpl(get(), get()) }

    single { SharePreferenceUtils(androidContext()) }
    single { TrackMeRoomDatabase.getDatabase(get()).sessionDao() }
    single<SessionRepository> { SessionRepositoryImpl(get()) }
    single { ImageStorage(get()) }
}