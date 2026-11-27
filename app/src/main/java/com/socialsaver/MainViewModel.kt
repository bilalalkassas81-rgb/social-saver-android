package com.socialsaver

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.socialsaver.core.ErrorText
import com.socialsaver.core.HistoryItem
import com.socialsaver.core.LinkCheck
import com.socialsaver.core.LinkValidator
import com.socialsaver.core.MediaInfo
import com.socialsaver.core.Platform
import com.socialsaver.core.UpdateInfo
import com.socialsaver.core.UserFacingException
import com.socialsaver.data.Account
import com.socialsaver.data.Accounts
import com.socialsaver.data.Engine
import com.socialsaver.data.HistoryStore
import com.socialsaver.data.MediaSaver
import com.socialsaver.data.SavedFile
import com.socialsaver.data.Updater
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Phase { IDLE, FETCHING, READY, DOWNLOADING, DONE }

data class HomeState(
    val input: String = "",
    val phase: Phase = Phase.IDLE,
    val info: MediaInfo? = null,
    val selected: Int = 0,
    val progress: Float? = null,
    val error: String? = null,
    val loginFor: Platform? = null,
    val details: String? = null,
    val message: String? = null,
    val saved: List<SavedFile> = emptyList(),
) {
    val busy: Boolean get() = phase == Phase.FETCHING || phase == Phase.DOWNLOADING
}

data class SettingsState(
    val engineVersion: String? = null,
    val updating: Boolean = false,
    val updateMessage: String? = null,
)

data class UpdateState(
    val info: UpdateInfo? = null,
    val showDialog: Boolean = false,
    val checking: Boolean = false,
    val checkMessage: String? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx get() = getApplication<App>()
    val history: HistoryStore = getApplication<App>().history

    private val _home = MutableStateFlow(HomeState())
    val home: StateFlow<HomeState> = _home.asStateFlow()

    private val _settings = MutableStateFlow(SettingsState())
    val settings: StateFlow<SettingsState> = _settings.asStateFlow()

    private val _accounts = MutableStateFlow<Set<Account>>(emptySet())
    val accounts: StateFlow<Set<Account>> = _accounts.asStateFlow()

    private val _update = MutableStateFlow(UpdateState())
    val update: StateFlow<UpdateState> = _update.asStateFlow()

    private var job: Job? = null

    init {
        viewModelScope.launch { history.load() }
        checkForUpdate(manual = false)
        viewModelScope.launch { _accounts.value = Accounts.loggedIn() }
        // Warm up the engine (first launch unpacks Python + yt-dlp, takes a few seconds).
        viewModelScope.launch {
            _settings.update { it.copy(engineVersion = Engine.version(ctx)) }
            Engine.autoUpdateIfDue(ctx)
            _settings.update { it.copy(engineVersion = Engine.version(ctx)) }
        }
    }

    fun onInput(text: String) {
        if (_home.value.busy) return
        _home.value = HomeState(input = text)
    }

    fun fetch() {
        val state = _home.value
        if (state.busy) return
        when (val check = LinkValidator.check(state.input)) {
            is LinkCheck.Invalid -> _home.update { it.copy(error = check.reason) }
            is LinkCheck.Valid -> {
                _home.value = HomeState(input = check.url, phase = Phase.FETCHING)
                job = viewModelScope.launch {
                    try {
                        val info = Engine.fetch(ctx, check.url, check.platform)
                        _home.update { it.copy(phase = Phase.READY, info = info, selected = 0) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        _home.update {
                            val ufe = e as? UserFacingException
                            it.copy(phase = Phase.IDLE, error = e.userMessage(), loginFor = ufe?.loginFor, details = ufe?.details)
                        }
                    }
                }
            }
        }
    }

    fun select(index: Int) {
        if (!_home.value.busy) _home.update { it.copy(selected = index) }
    }

    fun download() {
        val state = _home.value
        val info = state.info ?: return
        if (state.busy) return
        val option = info.options.getOrNull(state.selected) ?: return
        _home.update { it.copy(phase = Phase.DOWNLOADING, progress = 0f, error = null, message = null) }
        job = viewModelScope.launch {
            try {
                val saved = Engine.download(ctx, info, option) { p -> _home.update { s -> s.copy(progress = p) } }
                val now = System.currentTimeMillis()
                history.add(
                    HistoryItem(
                        id = now,
                        title = info.title,
                        platform = info.platform.label,
                        sourceUrl = info.url,
                        thumbnail = info.thumbnail,
                        quality = option.label,
                        uris = saved.map { it.uri },
                        mimes = saved.map { it.mime },
                        timestamp = now,
                    )
                )
                val where = "Downloads/${MediaSaver.FOLDER}"
                _home.update {
                    it.copy(
                        phase = Phase.DONE,
                        progress = 1f,
                        saved = saved,
                        message = if (saved.size == 1) "Saved to $where" else "Saved ${saved.size} files to $where",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _home.update { it.copy(phase = Phase.READY, progress = null, error = e.userMessage()) }
            }
        }
    }

    fun cancel() {
        if (_home.value.phase != Phase.DOWNLOADING) return
        Engine.cancel()
        job?.cancel()
        _home.update { it.copy(phase = Phase.READY, progress = null, error = "Download cancelled.") }
    }

    fun newLink() {
        if (!_home.value.busy) _home.value = HomeState()
    }

    /** Called when the login screen closes. Retries the link if it failed because of a missing login. */
    fun onLoginFinished() {
        viewModelScope.launch {
            _accounts.value = Accounts.loggedIn()
            val waitingFor = _home.value.loginFor?.let { Account.forPlatform(it) }
            if (waitingFor != null && waitingFor in _accounts.value) fetch()
        }
    }

    fun logout(account: Account) {
        viewModelScope.launch {
            Accounts.logout(account)
            _accounts.value = Accounts.loggedIn()
        }
    }

    /** Runs on every app start; [manual] is the "Check for updates" button in Settings. */
    fun checkForUpdate(manual: Boolean) {
        if (_update.value.checking) return
        _update.update { it.copy(checking = manual, checkMessage = null) }
        viewModelScope.launch {
            when (val r = Updater.check()) {
                is Updater.Check.Available -> _update.update {
                    it.copy(info = r.info, showDialog = true, checking = false)
                }
                Updater.Check.UpToDate -> _update.update {
                    it.copy(checking = false, checkMessage = if (manual) "You have the latest version." else null)
                }
                is Updater.Check.Failed -> _update.update {
                    it.copy(checking = false, checkMessage = if (manual) "Couldn't check for updates. Try again later." else null)
                }
            }
        }
    }

    fun dismissUpdate() = _update.update { it.copy(showDialog = false) }

    fun removeHistory(id: Long) {
        viewModelScope.launch { history.remove(id) }
    }

    fun clearHistory() {
        viewModelScope.launch { history.clear() }
    }

    fun updateEngine() {
        if (_settings.value.updating) return
        _settings.update { it.copy(updating = true, updateMessage = null) }
        viewModelScope.launch {
            val message = try {
                Engine.update(ctx)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                e.userMessage()
            }
            val version = Engine.version(ctx)
            _settings.update { it.copy(updating = false, updateMessage = message, engineVersion = version) }
        }
    }

    private fun Exception.userMessage(): String =
        if (this is UserFacingException) message.orEmpty() else ErrorText.friendly(message)
}
