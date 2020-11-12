package kb.dev.trackme

import kb.dev.trackme.map.MapManager
import kb.dev.trackme.map.MapManagerImpl
import kb.dev.trackme.mvvm.viewmodels.SessionViewModel
import kb.dev.trackme.mvvm.viewmodels.SessionsHistoryViewModel
import kb.dev.trackme.repositories.SessionRepositoryImpl
import kb.dev.trackme.repositories.SessionRepository
import kotlinx.coroutines.FlowPreview
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

@FlowPreview
val appModule = module {
    viewModel { SessionViewModel(get(), get()) }
    viewModel { SessionsHistoryViewModel(get()) }

    factory<MapManager> { MapManagerImpl() }

    single<SessionRepository> { SessionRepositoryImpl() }
}