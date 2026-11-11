package app.echo.messages.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.echo.messages.data.ConversationRow
import app.echo.messages.data.Message
import app.echo.messages.echo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class EchoViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = app.echo.db.messages()
    val prefs = app.echo.prefs
    val billing = app.echo.billing

    /** null until the first database read, so screens can tell "loading" from "empty". */
    val conversations: StateFlow<List<ConversationRow>?> = dao.conversations().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val deleted: StateFlow<List<Message>?> = dao.deleted().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun thread(pkg: String, conversation: String): Flow<List<Message>> = dao.thread(pkg, conversation)

    val query = MutableStateFlow("")
    val results: StateFlow<List<Message>> = query
        .debounce(120)
        .flatMapLatest { q -> if (q.isBlank()) flowOf(emptyList()) else dao.search(q.trim()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteConversation(pkg: String, conversation: String) =
        viewModelScope.launch(Dispatchers.IO) { dao.deleteConversation(pkg, conversation) }

    fun clearAll() = viewModelScope.launch(Dispatchers.IO) { dao.clear() }

    fun buyRemoveAds(activity: Activity) = billing.buy(activity)
}
