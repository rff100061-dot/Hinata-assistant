package com.example.model

data class WebPreset(
    val title: String,
    val url: String,
    val subtitle: String
)

data class WebUiState(
    val currentUrl: String = "",
    val inputUrl: String = "",
    val pageTitle: String = "Web App",
    val isLoading: Boolean = false,
    val progress: Float = 0f,
    val isDesktopMode: Boolean = false,
    val isOffline: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isAppMode: Boolean = false,
    val autoLaunchEnabled: Boolean = false,
    val isTopBarVisible: Boolean = true
)
