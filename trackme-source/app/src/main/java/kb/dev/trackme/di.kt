package kb.dev.trackme

import kb.dev.trackme.mvvm.viewmodels.SessionViewModel
import kb.dev.trackme.mvvm.viewmodels.SessionsHistoryViewModel
import kb.dev.trackme.repositories.SessionRepository
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    viewModel { SessionViewModel(get()) }
    viewModel { SessionsHistoryViewModel(get()) }

    single { SessionRepository() }
}