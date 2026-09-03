package com.sainpas.nkc.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sainpas.nkc.BuildConfig
import com.sainpas.nkc.content.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class NkcUiState(val appVersion: Int = BuildConfig.VERSION_CODE, val content: ContentSnapshot, val status: String = "Ready")
class NkcViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FileContentRepository(application)
    private val store = AtomicContentStore(File(application.filesDir, "content"))
    private val updater = ContentUpdater(BuildConfig.CONTENT_MANIFEST_URL, BuildConfig.VERSION_CODE, store, cacheDirectory = application.cacheDir)
    private val _state = MutableStateFlow(NkcUiState(content = repository.snapshot()))
    val state = _state.asStateFlow()
    init { checkForUpdates() }
    fun checkForUpdates() = viewModelScope.launch {
        _state.value = _state.value.copy(status = "Checking for updates…")
        val result = updater.checkForUpdate()
        _state.value = _state.value.copy(content = repository.snapshot(), status = when (result) {
            UpdateResult.UpToDate -> "Content is up to date"; is UpdateResult.Updated -> "Updated to content ${result.version}"
            is UpdateResult.Deferred -> result.reason; is UpdateResult.Failed -> "Update unavailable: ${result.reason}"
        })
    }
}
