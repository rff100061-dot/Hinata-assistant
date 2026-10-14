package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.model.WebPreset
import com.example.model.WebUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class WebConverterViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("web_to_app_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(WebUiState())
    val uiState: StateFlow<WebUiState> = _uiState.asStateFlow()

    val presets = listOf(
        WebPreset(
            title = "AI Studio Preview",
            url = "https://ais-pre-bklelusu3sykszunzjwtnq-653456169553.asia-southeast1.run.app",
            subtitle = "Live Cloud Preview App"
        ),
        WebPreset(
            title = "Wikipedia",
            url = "https://m.wikipedia.org",
            subtitle = "Free Online Encyclopedia"
        ),
        WebPreset(
            title = "GitHub",
            url = "https://github.com",
            subtitle = "Developer Platform & Code"
        ),
        WebPreset(
            title = "Google News",
            url = "https://news.google.com",
            subtitle = "Latest Headlines & Articles"
        )
    )

    init {
        val savedUrl = prefs.getString(KEY_SAVED_URL, "") ?: ""
        val autoLaunch = prefs.getBoolean(KEY_AUTO_LAUNCH, false)
        val defaultInput = if (savedUrl.isNotBlank()) savedUrl else presets.first().url

        if (savedUrl.isNotBlank() && autoLaunch) {
            _uiState.update {
                it.copy(
                    currentUrl = savedUrl,
                    inputUrl = savedUrl,
                    isAppMode = true,
                    autoLaunchEnabled = true
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    inputUrl = defaultInput,
                    autoLaunchEnabled = autoLaunch
                )
            }
        }
    }

    fun onInputUrlChanged(newUrl: String) {
        _uiState.update { it.copy(inputUrl = newUrl) }
    }

    fun onAutoLaunchToggled(enabled: Boolean) {
        _uiState.update { it.copy(autoLaunchEnabled = enabled) }
        prefs.edit().putBoolean(KEY_AUTO_LAUNCH, enabled).apply()
    }

    fun launchWebsite(urlToLaunch: String? = null) {
        var rawUrl = (urlToLaunch ?: _uiState.value.inputUrl).trim()
        if (rawUrl.isBlank()) return

        if (!rawUrl.startsWith("http://") && !rawUrl.startsWith("https://")) {
            rawUrl = "https://$rawUrl"
        }

        prefs.edit().putString(KEY_SAVED_URL, rawUrl).apply()

        _uiState.update {
            it.copy(
                currentUrl = rawUrl,
                inputUrl = rawUrl,
                isAppMode = true,
                isOffline = false,
                isLoading = true,
                progress = 0.1f
            )
        }
    }

    fun closeAppMode() {
        _uiState.update { it.copy(isAppMode = false) }
    }

    fun toggleDesktopMode(): Boolean {
        val nextMode = !_uiState.value.isDesktopMode
        _uiState.update { it.copy(isDesktopMode = nextMode) }
        return nextMode
    }

    fun toggleTopBar() {
        _uiState.update { it.copy(isTopBarVisible = !it.isTopBarVisible) }
    }

    fun updateProgress(progressInt: Int) {
        val progFloat = (progressInt / 100f).coerceIn(0f, 1f)
        _uiState.update {
            it.copy(
                progress = progFloat,
                isLoading = progressInt < 100
            )
        }
    }

    fun updatePageState(title: String?, canBack: Boolean, canForward: Boolean) {
        _uiState.update {
            it.copy(
                pageTitle = if (!title.isNullOrBlank()) title else it.pageTitle,
                canGoBack = canBack,
                canGoForward = canForward
            )
        }
    }

    fun setOffline(offline: Boolean) {
        _uiState.update { it.copy(isOffline = offline) }
    }

    companion object {
        private const val KEY_SAVED_URL = "saved_website_url"
        private const val KEY_AUTO_LAUNCH = "auto_launch_enabled"
    }
}
