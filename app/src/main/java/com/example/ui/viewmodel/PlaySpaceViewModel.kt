package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.*
import com.example.data.repository.AppRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.random.Random

// Sealed representation of screens for custom in-app stateful backstack navigation, robust across tablets and phones
sealed class Screen {
    object Home : Screen()
    data class AppDetails(val appId: String) : Screen()
    object DeveloperDashboard : Screen()
    object AdminModeration : Screen()
    object UserProfile : Screen()
    object NotificationCenter : Screen()
}

class PlaySpaceViewModel(private val repository: AppRepository) : ViewModel() {

    // Navigation and Backstack state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val backStack = mutableListOf<Screen>()

    // Filter and search selectors
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Dark Mode state Toggle
    private val _isDarkMode = MutableStateFlow(true) // Start with premium dark theme
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Active apps filtered by category + query
    val appsList: StateFlow<List<AppEntity>> = combine(
        repository.allApps,
        _searchQuery,
        _selectedCategory
    ) { apps, query, category ->
        apps.filter { app ->
            val matchesCategory = (category == "All" || app.category == category)
            val matchesQuery = (query.isEmpty() || app.name.contains(query, ignoreCase = true) || app.description.contains(query, ignoreCase = true))
            matchesCategory && matchesQuery
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Pending Moderator Apps
    val moderationAppsList: StateFlow<List<AppEntity>> = repository.allApps.map { apps ->
        apps.filter { !it.isApproved }
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    // All active notifications list
    val notifications: StateFlow<List<NotificationEntity>> = repository.allNotifications
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    // Active crash logs
    val crashLogs: StateFlow<List<CrashLogEntity>> = repository.allCrashLogs
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    // Logged in user profile details
    val userProfile: StateFlow<UserEntity?> = repository.userProfile
        .stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null)

    // Current app detail configuration
    private val _selectedAppId = MutableStateFlow<String?>(null)
    val selectedAppId: StateFlow<String?> = _selectedAppId.asStateFlow()

    val selectedApp: StateFlow<AppEntity?> = _selectedAppId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getAppById(id)
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null)

    val selectedAppReviews: StateFlow<List<ReviewEntity>> = _selectedAppId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getReviewsForApp(id)
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = emptyList())

    // Active payment flow processing state
    private val _activePaymentApp = MutableStateFlow<AppEntity?>(null)
    val activePaymentApp: StateFlow<AppEntity?> = _activePaymentApp.asStateFlow()

    private val _paymentProcessing = MutableStateFlow(false)
    val paymentProcessing: StateFlow<Boolean> = _paymentProcessing.asStateFlow()

    // Cloud synchronized status visual indicator
    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    // Download/Install active percentage states
    private val _downloadProgressMap = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgressMap: StateFlow<Map<String, Int>> = _downloadProgressMap.asStateFlow()

    // Statistics counts
    val uniqueCategoryInteraction = MutableStateFlow<Map<String, Int>>(mapOf(
        "Games" to 12, "Productivity" to 8, "Social" to 14, "Tools" to 5, "Finance" to 3
    ))

    // Navigation triggers
    fun navigateTo(screen: Screen) {
        if (_currentScreen.value != screen) {
            backStack.add(_currentScreen.value)
            _currentScreen.value = screen
            if (screen is Screen.AppDetails) {
                _selectedAppId.value = screen.appId
            }
        }
    }

    fun navigateBack(): Boolean {
        if (backStack.isNotEmpty()) {
            val prev = backStack.removeAt(backStack.size - 1)
            _currentScreen.value = prev
            if (prev is Screen.AppDetails) {
                _selectedAppId.value = prev.appId
            } else if (prev == Screen.Home) {
                _selectedAppId.value = null
            }
            return true
        }
        return false
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
        // Track visual interaction metric for analytics
        val updatedMap = uniqueCategoryInteraction.value.toMutableMap()
        updatedMap[category] = (updatedMap[category] ?: 0) + 1
        uniqueCategoryInteraction.value = updatedMap
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Download flow
    fun startAppDownload(appId: String) {
        viewModelScope.launch {
            repository.simulateAppDownload(appId) { progress ->
                val currentMap = _downloadProgressMap.value.toMutableMap()
                currentMap[appId] = progress
                _downloadProgressMap.value = currentMap
            }
        }
    }

    // Rating / review submitter
    fun submitAppReview(appId: String, rating: Float, comment: String) {
        val user = userProfile.value?.displayName ?: "Anonymous User"
        viewModelScope.launch {
            repository.submitReview(appId, user, rating, comment)
        }
    }

    // Payment initiator
    fun initiatePayment(app: AppEntity) {
        _activePaymentApp.value = app
    }

    fun cancelPayment() {
        _activePaymentApp.value = null
    }

    fun completePayment(paymentMethod: String) {
        val app = _activePaymentApp.value ?: return
        viewModelScope.launch {
            _paymentProcessing.value = true
            val success = repository.processSecurePayment(app.id, paymentMethod, app.price)
            _paymentProcessing.value = false
            if (success) {
                _activePaymentApp.value = null
                // Complete mock purchase and allow instantaneous simulated installation triggers
                startAppDownload(app.id)
            }
        }
    }

    // Developer Submission Upload Forms
    fun submitDeveloperApp(
        name: String,
        packageName: String,
        category: String,
        description: String,
        isPaid: Boolean,
        price: Double,
        sizeMb: Double,
        iconName: String
    ) {
        viewModelScope.launch {
            val appUniqueId = name.replace(" ", "_").lowercase() + "_" + Random.nextInt(100, 999)
            repository.developerUploadApp(
                id = appUniqueId,
                name = name,
                packageName = packageName,
                category = category,
                description = description,
                isPaid = isPaid,
                price = price,
                sizeMb = sizeMb,
                iconName = iconName
            )
        }
    }

    // Moderation controls
    fun approveApp(appId: String) {
        viewModelScope.launch {
            repository.moderateApp(appId, true)
        }
    }

    fun rejectApp(appId: String) {
        viewModelScope.launch {
            repository.moderateApp(appId, false)
        }
    }

    // Simulate crash trigger
    fun simulateCrashEvent(appName: String, packageName: String) {
        viewModelScope.launch {
            val messages = listOf(
                "java.lang.NullPointerException: Screen layout container has finished rendering without binding",
                "android.os.NetworkOnMainThreadException: Networking packet sync operation on background UI dispatcher",
                "java.lang.OutOfMemoryError: Bitmaps memory allocation exceeded buffer size limitations (24.5MB)",
                "java.lang.IllegalStateException: PlaySpace API security credential verification mismatch"
            )
            repository.reportCrash(
                appName = appName,
                packageName = packageName,
                exceptionMessage = messages.random(),
                stackTrace = "at com.playspace.simulation.CrashSimulationEngine.throwDynamicMock(${appName}.kt:420)\n" +
                        "at android.widget.ComposeView.onAttachedToWindow(ComposeView.kt:135)\n" +
                        "at android.view.View.dispatchAttachedToWindow(View.java:20421)"
            )
        }
    }

    // Resolve crash log
    fun resolveCrash(crashId: Int) {
        viewModelScope.launch {
            repository.resolveCrash(crashId)
        }
    }

    // Wallet balance top up
    fun addWalletBalance(amount: Double) {
        val email = userProfile.value?.email ?: return
        viewModelScope.launch {
            repository.topUpWallet(email, amount)
        }
    }

    // Biometric Toggle Enrollment Configuration
    fun setBiometricEnrollment(enabled: Boolean) {
        val email = userProfile.value?.email ?: return
        viewModelScope.launch {
            repository.updateBiometricEnrollment(email, enabled)
        }
    }

    // Cloud synchronized mock execution
    fun triggerCloudSync() {
        viewModelScope.launch {
            _isCloudSyncing.value = true
            delay(1500)
            _isCloudSyncing.value = false
            repository.clearAllNotifications() // Clean mock logs and reload a synced notification state
            repository.prepopulateDatabaseIfNeeded()
        }
    }

    fun clearNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }
}

class PlaySpaceViewModelFactory(private val repository: AppRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlaySpaceViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlaySpaceViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class Exception configuration")
    }
}
