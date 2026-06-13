package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.*
import com.example.ui.viewmodel.PlaySpaceViewModel
import com.example.ui.viewmodel.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Helper resolver for Material Icons dynamically mapped from string keys
@Composable
fun resolveIcon(name: String): ImageVector {
    return when (name) {
        "sports_esports" -> Icons.Default.SportsEsports
        "grid_view" -> Icons.Default.GridView
        "edit_calendar" -> Icons.Default.EditCalendar
        "history_edu" -> Icons.Default.HistoryEdu
        "forum" -> Icons.Default.Forum
        "bolt" -> Icons.Default.Bolt
        "vpn_key" -> Icons.Default.VpnKey
        "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
        "search" -> Icons.Default.Search
        "settings" -> Icons.Default.Settings
        "verified_user" -> Icons.Default.VerifiedUser
        "analytics" -> Icons.Default.Analytics
        else -> Icons.Default.Android
    }
}

@Composable
fun PlaySpaceAppContent(viewModel: PlaySpaceViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val activePaymentApp by viewModel.activePaymentApp.collectAsState()
    
    // Window configuration size check for responsiveness
    val configuration = LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 600

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isTablet) {
                // Adaptive Tablet Layout: Side Navigation Rail + Left-Right Split View
                Row(modifier = Modifier.fillMaxSize()) {
                    PlaySpaceNavigationRail(
                        currentScreen = currentScreen,
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxHeight()
                    )
                    
                    VerticalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))

                    // Center-Right content: If Screen is Home or AppDetails on tablet, show Split view
                    Box(modifier = Modifier.weight(1f)) {
                        when (currentScreen) {
                            Screen.Home, is Screen.AppDetails -> {
                                TabletSplitView(viewModel = viewModel)
                            }
                            Screen.DeveloperDashboard -> DeveloperDashboardView(viewModel = viewModel)
                            Screen.AdminModeration -> AdminModerationView(viewModel = viewModel)
                            Screen.UserProfile -> UserProfileView(viewModel = viewModel)
                            Screen.NotificationCenter -> NotificationCenterView(viewModel = viewModel)
                        }
                    }
                }
            } else {
                // Mobile Portrait Layout: Top bar content + central page + bottom navigation bar
                Scaffold(
                    topBar = { PlaySpaceTopBar(viewModel = viewModel) },
                    bottomBar = { PlaySpaceBottomNavigation(currentScreen = currentScreen, viewModel = viewModel) },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "screen_transition"
                        ) { target ->
                            when (target) {
                                Screen.Home -> HomeView(viewModel = viewModel)
                                is Screen.AppDetails -> AppDetailsView(viewModel = viewModel, appId = target.appId)
                                Screen.DeveloperDashboard -> DeveloperDashboardView(viewModel = viewModel)
                                Screen.AdminModeration -> AdminModerationView(viewModel = viewModel)
                                Screen.UserProfile -> UserProfileView(viewModel = viewModel)
                                Screen.NotificationCenter -> NotificationCenterView(viewModel = viewModel)
                            }
                        }
                    }
                }
            }

            // Secure Payment sheet dialog (Dynamic visual feedback)
            if (activePaymentApp != null) {
                SecurePaymentDialog(
                    app = activePaymentApp!!,
                    onDismiss = { viewModel.cancelPayment() },
                    onConfirm = { method -> viewModel.completePayment(method) },
                    processing = viewModel.paymentProcessing.collectAsState().value,
                    balance = viewModel.userProfile.collectAsState().value?.walletBalance ?: 0.0
                )
            }
        }
    }
}

// Tablet side navigation panel
@Composable
fun PlaySpaceNavigationRail(
    currentScreen: Screen,
    viewModel: PlaySpaceViewModel,
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.notifications.collectAsState()
    val unreadNotifications = notifications.size

    NavigationRail(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        header = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "PlaySpace Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = "PlaySpace",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        val showHomeActive = currentScreen == Screen.Home || currentScreen is Screen.AppDetails

        NavigationRailItem(
            selected = showHomeActive,
            onClick = { viewModel.navigateTo(Screen.Home) },
            icon = { Icon(Icons.Default.Storefront, contentDescription = "Store") },
            label = { Text("Store") },
            modifier = Modifier.testTag("nav_store_tablet")
        )

        NavigationRailItem(
            selected = currentScreen == Screen.DeveloperDashboard,
            onClick = { viewModel.navigateTo(Screen.DeveloperDashboard) },
            icon = { Icon(Icons.Default.DeveloperMode, contentDescription = "Developers") },
            label = { Text("Developer") },
            modifier = Modifier.testTag("nav_dev_tablet")
        )

        NavigationRailItem(
            selected = currentScreen == Screen.AdminModeration,
            onClick = { viewModel.navigateTo(Screen.AdminModeration) },
            icon = { Icon(Icons.Default.Security, contentDescription = "Moderation") },
            label = { Text("Admin") },
            modifier = Modifier.testTag("nav_admin_tablet")
        )

        NavigationRailItem(
            selected = currentScreen == Screen.UserProfile,
            onClick = { viewModel.navigateTo(Screen.UserProfile) },
            icon = { Icon(Icons.Default.AccountCircle, contentDescription = "User Hub") },
            label = { Text("Profile") },
            modifier = Modifier.testTag("nav_profile_tablet")
        )

        Spacer(modifier = Modifier.weight(1f))

        NavigationRailItem(
            selected = currentScreen == Screen.NotificationCenter,
            onClick = { viewModel.navigateTo(Screen.NotificationCenter) },
            icon = {
                BadgedBox(badge = {
                    if (unreadNotifications > 0) {
                        Badge { Text(unreadNotifications.toString()) }
                    }
                }) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                }
            },
            label = { Text("Alerts") },
            modifier = Modifier.testTag("nav_notif_tablet")
        )

        IconButton(onClick = { viewModel.toggleDarkMode() }, modifier = Modifier.padding(bottom = 16.dp)) {
            val isDark by viewModel.isDarkMode.collectAsState()
            Icon(
                imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Theme Switcher"
            )
        }
    }
}

// Tablet split layout list + details
@Composable
fun TabletSplitView(viewModel: PlaySpaceViewModel) {
    val selectedApp by viewModel.selectedApp.collectAsState()

    Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.width(380.dp).fillMaxHeight()) {
            Column(modifier = Modifier.fillMaxSize()) {
                PlaySpaceTopBar(viewModel = viewModel, trailingBellOnly = true)
                HomeView(viewModel = viewModel, isListOnly = true)
            }
        }

        VerticalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
        ) {
            if (selectedApp != null) {
                AppDetailsView(viewModel = viewModel, appId = selectedApp!!.id, displayBackArrow = false)
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "Select App Symbol",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Choose an app to inspect",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Select any title from the left play store listing",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// Mobile top heading actions
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaySpaceTopBar(viewModel: PlaySpaceViewModel, trailingBellOnly: Boolean = false) {
    val notifications by viewModel.notifications.collectAsState()
    val unread = notifications.count { !it.isRead }
    val currentScreen by viewModel.currentScreen.collectAsState()

    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Logo",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "PlaySpace",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    letterSpacing = 0.5.sp
                )
            }
        },
        navigationIcon = {
            if (!trailingBellOnly && currentScreen != Screen.Home) {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Return")
                }
            }
        },
        actions = {
            IconButton(onClick = { viewModel.navigateTo(Screen.NotificationCenter) }, modifier = Modifier.testTag("top_bell_button")) {
                BadgedBox(badge = {
                    if (unread > 0) {
                        Badge { Text(unread.toString()) }
                    }
                }) {
                    Icon(imageVector = Icons.Default.Notifications, contentDescription = "Notifications Log")
                }
            }
        }
    )
}

// Mobile bottom navigation panel
@Composable
fun PlaySpaceBottomNavigation(currentScreen: Screen, viewModel: PlaySpaceViewModel) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)) {
        val showHomeActive = currentScreen == Screen.Home || currentScreen is Screen.AppDetails

        NavigationBarItem(
            selected = showHomeActive,
            onClick = { viewModel.navigateTo(Screen.Home) },
            icon = { Icon(Icons.Default.Storefront, contentDescription = "Catalog Overview") },
            label = { Text("Store") },
            modifier = Modifier.testTag("nav_btn_store")
        )
        NavigationBarItem(
            selected = currentScreen == Screen.DeveloperDashboard,
            onClick = { viewModel.navigateTo(Screen.DeveloperDashboard) },
            icon = { Icon(Icons.Default.DeveloperMode, contentDescription = "Developer Suite") },
            label = { Text("Developer") },
            modifier = Modifier.testTag("nav_btn_developer")
        )
        NavigationBarItem(
            selected = currentScreen == Screen.AdminModeration,
            onClick = { viewModel.navigateTo(Screen.AdminModeration) },
            icon = { Icon(Icons.Default.Security, contentDescription = "Moderations Settings") },
            label = { Text("Admin") },
            modifier = Modifier.testTag("nav_btn_admin")
        )
        NavigationBarItem(
            selected = currentScreen == Screen.UserProfile,
            onClick = { viewModel.navigateTo(Screen.UserProfile) },
            icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Profile Details") },
            label = { Text("Profile") },
            modifier = Modifier.testTag("nav_btn_profile")
        )
    }
}

// --------------------------- STORE FRONT VIEW ---------------------------

@Composable
fun HomeView(
    viewModel: PlaySpaceViewModel,
    isListOnly: Boolean = false
) {
    val apps by viewModel.appsList.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    val categories = listOf("All", "Games", "Productivity", "Social", "Tools", "Finance")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // High-Fidelity Search Card Interface
        SearchTextField(
            query = searchQuery,
            onQueryChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        // Horizontal Category Chips Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                FilterChip(
                    selected = (selectedCategory == category),
                    onClick = { viewModel.setCategory(category) },
                    label = { Text(category, fontSize = 13.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("category_chip_$category")
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Large aesthetic welcome banner carousel if not searching/listOnly
        if (!isListOnly && searchQuery.isEmpty() && selectedCategory == "All") {
            PlayStorePromoBanner(
                appName = "Galaxy Quest 3D",
                promoText = "Explore deep galaxies of StarForge - Free install today!",
                onClick = { viewModel.navigateTo(Screen.AppDetails("galaxy_quest")) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Title listing segment
        Text(
            text = if (searchQuery.isNotEmpty()) "Search Results" else "Featured Applications",
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        if (apps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = "None found logo",
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Apps Found",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Try another keyword or developer query",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .testTag("apps_list"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(apps) { app ->
                    AppStoreRowItem(
                        app = app,
                        onClick = { viewModel.navigateTo(Screen.AppDetails(app.id)) }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchTextField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.testTag("app_search_bar"),
        placeholder = { Text("Search apps & play games...", fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Magnifier") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        maxLines = 1,
        shape = RoundedCornerShape(28.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
            focusedBorderColor = MaterialTheme.colorScheme.primary
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() })
    )
}

@Composable
fun PlayStorePromoBanner(
    appName: String,
    promoText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable(onClick = onClick)
            .shadow(4.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "EDITOR'S CHOICE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = appName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = promoText,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
            }
            // Abstract floating elements simulating graphics
            Icon(
                imageVector = Icons.Default.SportsEsports,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.15f),
                modifier = Modifier
                    .size(96.dp)
                    .align(Alignment.CenterEnd)
            )
        }
    }
}

@Composable
fun AppStoreRowItem(
    app: AppEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("app_row_${app.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App visual avatar container
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = resolveIcon(app.iconName),
                    contentDescription = "App Icon",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Body text information
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Text(
                        text = "${app.rating} ★",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${app.sizeMb} MB",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = app.category,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Downloader Indicator Status Icon
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (app.localDownloadStatus) {
                    "NOT_DOWNLOADED" -> {
                        Text(
                            text = if (app.isPaid) "$${app.price}" else "FREE",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    "DOWNLOADING" -> {
                        CircularProgressIndicator(
                            progress = app.downloadProgress / 100f,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                    "DOWNLOADED" -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Installed",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

// --------------------------- APP DETAIL VIEW ---------------------------

@Composable
fun AppDetailsView(
    viewModel: PlaySpaceViewModel,
    appId: String,
    displayBackArrow: Boolean = true
) {
    val appState = viewModel.selectedApp.collectAsState()
    val reviews by viewModel.selectedAppReviews.collectAsState()

    val app = appState.value ?: return

    var userComment by remember { mutableStateOf("") }
    var userRating by remember { mutableStateOf(5f) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("app_details_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App header containing metadata
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = resolveIcon(app.iconName),
                        contentDescription = "Detail Icon",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = app.name,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = app.packageName,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = app.developerName,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Action CTA installing/download triggers
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (app.localDownloadStatus) {
                    "NOT_DOWNLOADED" -> {
                        Button(
                            onClick = {
                                if (app.isPaid) {
                                    viewModel.initiatePayment(app)
                                } else {
                                    viewModel.startAppDownload(app.id)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("install_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                text = if (app.isPaid) "Buy for $${app.price}" else "Install",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    "DOWNLOADING" -> {
                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Downloading (${app.downloadProgress}%)",
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    "DOWNLOADED" -> {
                        Button(
                            onClick = { },
                            modifier = Modifier
                                .weight(1.5f)
                                .height(48.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32)
                            )
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open App", fontWeight = FontWeight.Bold)
                        }

                        // Debug simulator trigger for Developer/Crash analytics
                        Button(
                            onClick = { viewModel.simulateCrashEvent(app.name, app.packageName) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("simulate_crash_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.BugReport, contentDescription = "Crash")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Mock Crash", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Ratings metadata metrics grid
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${app.rating} ★",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${app.ratingCount} reviews",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Divider(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${app.sizeMb} MB",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(text = "App Size", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Divider(
                        modifier = Modifier
                            .height(30.dp)
                            .width(1.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${app.downloadCount}+",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            fontSize = 16.sp
                        )
                        Text(text = "Downloads", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Offline storage & Sync banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2E7D32).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF2E7D32).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "Offline indicator",
                        tint = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Offline Access Available",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = "Downloaded applications remain sandbox active with data synced.",
                            fontSize = 11.sp,
                            color = Color(0xFF1B5E20).copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Detailed Description Card
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "About this App",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = app.description,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }

        // User submission Feedback reviews portal
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Submit PlaySpace Rating",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Simulated sliding star indicators interactiveness
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..5) {
                            val active = i <= userRatingInt(userRating)
                            Icon(
                                imageVector = if (active) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Rating $i",
                                tint = if (active) Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clickable { userRating = i.toFloat() }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = userComment,
                        onValueChange = { userComment = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .testTag("review_input"),
                        placeholder = { Text("Write your reviews in Bengali or English...", fontSize = 13.sp) },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (userComment.isNotEmpty()) {
                                viewModel.submitAppReview(app.id, userRating, userComment)
                                userComment = ""
                            }
                        },
                        enabled = userComment.isNotEmpty(),
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("submit_review_btn")
                    ) {
                        Text("Post Review")
                    }
                }
            }
        }

        // Existing reviews stream listing
        item {
            Text(
                text = "Developer reviews & Feedbacks",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (reviews.isEmpty()) {
            item {
                Text(
                    text = "No reviews available yet. Be the first to express opinion!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        } else {
            items(reviews) { review ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = review.userName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${review.rating.toInt()} ★",
                                color = Color(0xFFFFB300),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = review.comment,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

private fun userRatingInt(rating: Float): Int = rating.roundToInt()

// --------------------------- DEVELOPER DASHBOARD VIEW ---------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperDashboardView(viewModel: PlaySpaceViewModel) {
    val apps by viewModel.appsList.collectAsState()
    val crashLogs by viewModel.crashLogs.collectAsState()

    var activeDeveloperTab by remember { mutableStateOf(0) } // 0: Metrics & Apps, 1: Submission Center, 2: Crash Reports

    // Upload form states
    var newAppName by remember { mutableStateOf("") }
    var newAppPkg by remember { mutableStateOf("") }
    var newAppCat by remember { mutableStateOf("Games") }
    var newAppDesc by remember { mutableStateOf("") }
    var newAppPaid by remember { mutableStateOf(false) }
    var newAppPrice by remember { mutableStateOf("1.99") }
    var newAppSize by remember { mutableStateOf("15.5") }
    var newAppIcon by remember { mutableStateOf("sports_esports") }

    val categories = listOf("Games", "Productivity", "Social", "Tools", "Finance")
    val iconsList = listOf("sports_esports", "grid_view", "edit_calendar", "history_edu", "forum", "bolt", "vpn_key")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("developer_dashboard_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Developer Studio Console",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Monitor metrics, sandbox uploads, & inspect automated crash reporting logs",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Custom M3 Tab Panel Selector
        item {
            SecondaryTabRow(selectedTabIndex = activeDeveloperTab) {
                Tab(
                    selected = (activeDeveloperTab == 0),
                    onClick = { activeDeveloperTab = 0 },
                    text = { Text("App Metrics") }
                )
                Tab(
                    selected = (activeDeveloperTab == 1),
                    onClick = { activeDeveloperTab = 1 },
                    text = { Text("Upload Form") },
                    modifier = Modifier.testTag("dev_upload_tab_btn")
                )
                Tab(
                    selected = (activeDeveloperTab == 2),
                    onClick = { activeDeveloperTab = 2 },
                    text = {
                        BadgedBox(badge = {
                            if (crashLogs.isNotEmpty()) {
                                Badge { Text(crashLogs.size.toString()) }
                            }
                        }) {
                            Text("Telemetry Crashes")
                        }
                    },
                    modifier = Modifier.testTag("dev_telemetry_tab_btn")
                )
            }
        }

        when (activeDeveloperTab) {
            0 -> {
                // Interactive analytical metrics summary (Canvas charts with simulated indicators)
                item {
                    Text("Total Downloads metrics (Last 6 Months)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    AppDownloadsVectorChart()
                }

                item {
                    Text("Category distribution share (Compose Canvas Circle Chart)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    CategoryAnalyticsPieChart(viewModel = viewModel)
                }

                item {
                    Text("My Sandbox uploads catalog", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                val devApps = apps.filter { it.developerName == "Your Dev Studio" }
                if (devApps.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, size = 48.dp, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No developer uploads detected", fontWeight = FontWeight.SemiBold)
                                Text("Use the Upload Form tab to submit custom creations.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(devApps) { app ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(resolveIcon(app.iconName), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(app.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(app.packageName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (app.isApproved) Color(0xFF2E7D32).copy(alpha = 0.15f) else Color(0xFFC62828).copy(alpha = 0.15f),
                                            RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (app.isApproved) "APPROVED & LIVE" else "PENDING REVIEW",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (app.isApproved) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            1 -> {
                // Form submissions parameters
                item {
                    Text("Register metadata properties and trigger admin pipeline validation approvals.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                item {
                    OutlinedTextField(
                        value = newAppName,
                        onValueChange = { newAppName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_app_name"),
                        label = { Text("Application Name") },
                        placeholder = { Text("e.g. Speed Chess 3D") },
                        singleLine = true
                    )
                }

                item {
                    OutlinedTextField(
                        value = newAppPkg,
                        onValueChange = { newAppPkg = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_app_package"),
                        label = { Text("Unique Package ID (Namespace)") },
                        placeholder = { Text("e.g. com.mychess.game") },
                        singleLine = true
                    )
                }

                item {
                    Text("Select Store Category Chip:", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { category ->
                            val active = newAppCat == category
                            FilterChip(
                                selected = active,
                                onClick = { newAppCat = category },
                                label = { Text(category) },
                                modifier = Modifier.testTag("form_category_$category")
                            )
                        }
                    }
                }

                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = newAppPaid,
                            onCheckedChange = { newAppPaid = it },
                            modifier = Modifier.testTag("form_paid_checkbox")
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("This is a Paid Application", fontSize = 14.sp)
                    }
                }

                if (newAppPaid) {
                    item {
                        OutlinedTextField(
                            value = newAppPrice,
                            onValueChange = { newAppPrice = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("form_app_price"),
                            label = { Text("Price ($)") },
                            singleLine = true
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = newAppSize,
                        onValueChange = { newAppSize = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("form_app_size"),
                        label = { Text("Package APK Size (MB)") },
                        singleLine = true
                    )
                }

                item {
                    Text("Select Launcher Vector Icon Type:", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(iconsList) { icon ->
                            val active = newAppIcon == icon
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .border(2.dp, if (active) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
                                    .clickable { newAppIcon = icon },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(resolveIcon(icon), contentDescription = null, tint = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = newAppDesc,
                        onValueChange = { newAppDesc = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("form_app_desc"),
                        label = { Text("Detailed Application Descriptions") }
                    )
                }

                item {
                    Button(
                        onClick = {
                            if (newAppName.isNotEmpty() && newAppPkg.isNotEmpty()) {
                                viewModel.submitDeveloperApp(
                                    name = newAppName,
                                    packageName = newAppPkg,
                                    category = newAppCat,
                                    description = newAppDesc,
                                    isPaid = newAppPaid,
                                    price = newAppPrice.toDoubleOrNull() ?: 0.0,
                                    sizeMb = newAppSize.toDoubleOrNull() ?: 10.0,
                                    iconName = newAppIcon
                                )
                                // Clear form
                                newAppName = ""
                                newAppPkg = ""
                                newAppDesc = ""
                                activeDeveloperTab = 0
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_dev_app_btn"),
                        enabled = newAppName.isNotEmpty() && newAppPkg.isNotEmpty()
                    ) {
                        Text("Submit App to Moderation", fontWeight = FontWeight.Bold)
                    }
                }
            }

            2 -> {
                // Telemetry real-time error logger tracker
                item {
                    Text("Crash reports feed back loop logs", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                if (crashLogs.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF2E7D32), size = 48.dp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No Active Crashes Reported", fontWeight = FontWeight.Bold)
                                Text("Apps within play store are currently performing extremely healthy.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(crashLogs) { crash ->
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("crash_item_${crash.id}"),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column {
                                        Text(crash.appName, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                                        Text(crash.packageName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f))
                                    }
                                    IconButton(
                                        onClick = { viewModel.resolveCrash(crash.id) },
                                        modifier = Modifier.testTag("resolve_crash_${crash.id}")
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Resolve", tint = Color(0xFF1B5E20))
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = crash.exceptionMessage,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = crash.stackTrace,
                                            fontSize = 10.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Custom-drawn downloads analytics graph in Compose pure Canvas (Offline-friendly)
@Composable
fun AppDownloadsVectorChart() {
    val chartColor = MaterialTheme.colorScheme.primary
    val supportColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
    val fontLabelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        val width = size.width
        val height = size.height

        // Horizontal guides
        drawLine(
            color = supportColor,
            start = Offset(0f, height * 0.25f),
            end = Offset(width, height * 0.25f),
            strokeWidth = 1f
        )
        drawLine(
            color = supportColor,
            start = Offset(0f, height * 0.5f),
            end = Offset(width, height * 0.5f),
            strokeWidth = 1f
        )
        drawLine(
            color = supportColor,
            start = Offset(0f, height * 0.75f),
            end = Offset(width, height * 0.75f),
            strokeWidth = 1f
        )

        // Plot points: Jan(10k), Feb(25k), Mar(15k), Apr(45k), May(35k), Jun(60k)
        val dataPoints = listOf(0.12f, 0.32f, 0.2f, 0.7f, 0.55f, 0.95f)
        val stepX = width / (dataPoints.size - 1)

        val pathsPoints = dataPoints.mapIndexed { idx, value ->
            Offset(idx * stepX, height - (value * height))
        }

        // Draw graph nodes connecting line
        for (i in 0 until pathsPoints.size - 1) {
            drawLine(
                color = chartColor,
                start = pathsPoints[i],
                end = pathsPoints[i + 1],
                strokeWidth = 5f
            )
        }

        // Draw node circles
        pathsPoints.forEach { pt ->
            drawCircle(
                color = chartColor,
                radius = 7f,
                center = pt
            )
            drawCircle(
                color = Color.White,
                radius = 3f,
                center = pt
            )
        }
    }
}

// Category Distribution Pie Chart with custom Canvas calculations (Offline-friendly)
@Composable
fun CategoryAnalyticsPieChart(viewModel: PlaySpaceViewModel) {
    val scoreMap by viewModel.uniqueCategoryInteraction.collectAsState()
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        Color(0xFFFFB300),
        Color(0xFF2E7D32),
        MaterialTheme.colorScheme.tertiary
    )

    val total = scoreMap.values.sum().toFloat()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(100.dp)) {
                var startAngle = 0f
                scoreMap.values.forEachIndexed { index, value ->
                    val sweepAngle = if (total > 0) (value / total) * 360f else 0f
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true
                    )
                    startAngle += sweepAngle
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                scoreMap.keys.toList().forEachIndexed { index, category ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(colors[index % colors.size], RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$category (${scoreMap[category] ?: 0})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

// Helper to provide extension size mapping to avoid compiler complaint
@Composable
fun Icon(imageVector: ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    Icon(imageVector = imageVector, contentDescription = contentDescription, modifier = Modifier.size(size), tint = tint)
}

// --------------------------- ADMIN CONTROLS VIEW ---------------------------

@Composable
fun AdminModerationView(viewModel: PlaySpaceViewModel) {
    val pendingApps by viewModel.moderationAppsList.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("admin_moderation_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Moderator Admin Control Center",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Approve newly created application uploads submitted by developer sandbox pipelines.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (pendingApps.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory,
                            contentDescription = "Inbox empty mark",
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("All Submissions Cleared", fontWeight = FontWeight.Bold)
                        Text("No pending developer uploads are waiting for checks.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(pendingApps) { app ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("pending_app_card_${app.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(resolveIcon(app.iconName), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(app.name, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                Text(app.packageName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Descriptions:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = app.description,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Size: ${app.sizeMb}MB", fontSize = 11.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Price: ${if (app.isPaid) "$${app.price}" else "Free"}", fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { viewModel.approveApp(app.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                modifier = Modifier.testTag("admin_approve_btn_${app.id}")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Approve Live", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { viewModel.rejectApp(app.id) },
                                modifier = Modifier.testTag("admin_reject_btn_${app.id}"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.DeleteForever, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Reject")
                            }
                        }
                    }
                }
            }
        }
    }
}

// --------------------------- USER PROFILE PORTAL ---------------------------

@Composable
fun UserProfileView(viewModel: PlaySpaceViewModel) {
    val userProfileState = viewModel.userProfile.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isSyncing by viewModel.isCloudSyncing.collectAsState()

    val profile = userProfileState.value ?: return

    var securityPromptActive by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("user_profile_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User primary avatar badge layout
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = profile.displayName.firstOrNull()?.toString() ?: "U",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = profile.displayName,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = profile.email,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // Wallet balances & transaction top-up actions
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Secure Wallet Credits [DEMO]", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "$${"%.2f".format(profile.walletBalance)}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Button(
                            onClick = { viewModel.addWalletBalance(50.0) },
                            modifier = Modifier.testTag("topup_wallet_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Top up $50")
                        }
                    }
                }
            }
        }

        // Synchronize and themes parameters
        item {
            Text("Settings & Accessibilities", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column {
                    // Dark theme Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Accessibility Dark mode theme", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Renders an beautiful warm, eye-friendly layout", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isDarkMode,
                            onCheckedChange = { viewModel.toggleDarkMode() },
                            modifier = Modifier.testTag("dark_mode_switch")
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    // Biometric lock Toggle with PIN security simulator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Biometric Face ID / Fingerprint Lock", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Require authentication passcode to download paid apps", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = profile.isBiometricEnabled,
                            onCheckedChange = { _ -> securityPromptActive = true },
                            modifier = Modifier.testTag("biometric_switch")
                        )
                    }

                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    // Cloud state synchronized
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Multi-Device Cloud Synchronization", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Seamlessly backup downloads list history", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            IconButton(onClick = { viewModel.triggerCloudSync() }, modifier = Modifier.testTag("cloud_sync_btn")) {
                                Icon(Icons.Default.Sync, contentDescription = "Sync Catalog")
                            }
                        }
                    }
                }
            }
        }

        // Comprehensive User Analytics Tracker
        item {
            Text("Comprehensive Activity Tracking Panel", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Digital Engagement Analytics", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text("Apps Installed", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = "${profile.sessionDownloadedCount} units", fontSize = 13.sp)
                        }
                        Column {
                            Text("PlaySpace Status", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(text = "Healthy Connected", fontSize = 11.sp, color = Color(0xFF2E7D32))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Device App Usage Track (Estimated):", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                    val stats = profile.appUsageStats.split(",")
                    Column(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        stats.forEach { stat ->
                            if (stat.contains(":")) {
                                val split = stat.split(":")
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(text = split[0], fontSize = 12.sp)
                                    Text(text = split[1], fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Biometric prompt pin entry mock modal
    if (securityPromptActive) {
        BiometricSimulatingDialog(
            active = profile.isBiometricEnabled,
            onDismiss = { securityPromptActive = false },
            onEnrollChange = { enabled ->
                viewModel.setBiometricEnrollment(enabled)
                securityPromptActive = false
            }
        )
    }
}

// Micro dialog pin authentication entry
@Composable
fun BiometricSimulatingDialog(
    active: Boolean,
    onDismiss: () -> Unit,
    onEnrollChange: (Boolean) -> Unit
) {
    var code1 by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, size = 48.dp, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (active) "Deactivate Fingerprint ID" else "Authorize Secure Biometrics",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Confirm 4-digit security PIN index to modify system security preferences.",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = code1,
                    onValueChange = { if (it.length <= 4) code1 = it },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier
                        .width(120.dp)
                        .testTag("pin_entry_field"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { onEnrollChange(!active) },
                        enabled = code1.length == 4,
                        modifier = Modifier.testTag("pin_confirm_button")
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

// --------------------------- NOTIFICATION CENTER VIEW ---------------------------

@Composable
fun NotificationCenterView(viewModel: PlaySpaceViewModel) {
    val alerts by viewModel.notifications.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("notification_center_view"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "System Notifications Hub",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Realtime visual warnings, approvals, and transaction approvals alerts",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (alerts.isNotEmpty()) {
                    TextButton(
                        onClick = { viewModel.clearNotifications() },
                        modifier = Modifier.testTag("clear_alerts_btn")
                    ) {
                        Text("Clear All")
                    }
                }
            }
        }

        if (alerts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.NotificationsNone,
                            contentDescription = "Alerts flat mark",
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Notification Center Is Clear",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "New alerts detailing your downloads and updates will populate dynamically.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        } else {
            items(alerts) { alert ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("alert_item_${alert.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, size = 18.dp)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = alert.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = alert.message,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --------------------------- DEEP PAYMENT SHEETS DIALOG ---------------------------

@Composable
fun SecurePaymentDialog(
    app: AppEntity,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    processing: Boolean,
    balance: Double
) {
    var selectedCard by remember { mutableStateOf("Visa ending 4205") }
    var currentStep by remember { mutableStateOf(0) } // 0: Select card method, 1: Loading / simulated validation

    val creditDetails = listOf("Visa ending 4205", "Mastercard ending 9812", "Bkash Digital Wallet", "SSLCommerz Pay")

    Dialog(onDismissRequest = if (processing) ({}) else onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentStep == 0) {
                    Icon(Icons.Default.Security, contentDescription = null, size = 48.dp, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Secure Checkout Payment",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "Complete transaction with PlaySpace authenticated tokens.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(app.name, fontWeight = FontWeight.Bold)
                            Text("$${app.price}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Select Payment Source Options:",
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    // Card options
                    Column(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        creditDetails.forEach { details ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCard = details }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (selectedCard == details),
                                    onClick = { selectedCard = details },
                                    modifier = Modifier.testTag("radio_pay_$details")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(details, fontSize = 14.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Balance status check
                    if (balance < app.price) {
                        Text(
                            text = "Warning: Insufficient credits! Balance: $${"%.2f".format(balance)}. Top up wallet in Profile.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Remaining Balance status: $${"%.2f".format(balance - app.price)}",
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                            Text("Dismiss")
                        }
                        Button(
                            onClick = {
                                currentStep = 1
                                onConfirm(selectedCard)
                            },
                            enabled = balance >= app.price,
                            modifier = Modifier
                                .weight(1.5f)
                                .testTag("payment_confirm_btn")
                        ) {
                            Text("Pay $${app.price}", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Step 1: Simulated Payment validation circular states
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(64.dp)
                            .padding(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Processing secure checkout...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Authenticating tokens and decrypting ledger, please wait...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }
}
