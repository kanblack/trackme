package kb.dev.trackme.mvvm.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import kb.dev.trackme.repositories.SessionRepository

class SessionsHistoryViewModel(repository: SessionRepository) : ViewModel() {
    val flow = Pager(PagingConfig(pageSize = 2)) {
        repository.getSessions()
    }.flow.cachedIn(viewModelScope)
}
