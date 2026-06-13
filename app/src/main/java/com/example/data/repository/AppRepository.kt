package com.example.data.repository

import com.example.data.db.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlin.random.Random

class AppRepository(
    private val appDao: AppDao,
    private val reviewDao: ReviewDao,
    private val notificationDao: NotificationDao,
    private val crashLogDao: CrashLogDao,
    private val userDao: UserDao
) {
    // Flows mapping to UI
    val allApps: Flow<List<AppEntity>> = appDao.getAllApps()
    val allNotifications: Flow<List<NotificationEntity>> = notificationDao.getAllNotifications()
    val allCrashLogs: Flow<List<CrashLogEntity>> = crashLogDao.getAllCrashLogs()
    val userProfile: Flow<UserEntity?> = userDao.getUserProfile()

    fun getAppById(id: String): Flow<AppEntity?> = appDao.getAppById(id)
    fun getReviewsForApp(appId: String): Flow<List<ReviewEntity>> = reviewDao.getReviewsForApp(appId)

    // Prepopulate DB if empty
    suspend fun prepopulateDatabaseIfNeeded() = withContext(Dispatchers.IO) {
        val currentApps = appDao.getAllApps().first()
        if (currentApps.isEmpty()) {
            val initialApps = listOf(
                AppEntity(
                    id = "galaxy_quest",
                    name = "Galaxy Quest 3D",
                    developerName = "StarForge Studios",
                    packageName = "com.starforge.galaxyquest",
                    category = "Games",
                    iconName = "sports_esports",
                    rating = 4.8f,
                    ratingCount = 14205,
                    isPaid = false,
                    price = 0.0,
                    description = "Embark on an epic space adventure! Explore procedurally generated star systems, assemble a cosmic fleet, engage in real-time tactical space combat, and defend the frontier against incoming alien invasions. Experience stellar graphics, customizable spaceships, and a gorgeous cinematic story soundtrack.",
                    sizeMb = 134.2,
                    downloadCount = 8920,
                    isApproved = true
                ),
                AppEntity(
                    id = "chess_royale",
                    name = "Chess Royale Pro",
                    developerName = "MindGames Inc.",
                    packageName = "com.mindgames.chessroyale",
                    category = "Games",
                    iconName = "grid_view",
                    rating = 4.6f,
                    ratingCount = 824,
                    isPaid = true,
                    price = 2.99,
                    description = "A premium chess simulator designed for both casual beginners and grandmasters. Includes high-fidelity tactile feedback, dynamic difficulty presets powered by standard rule-based puzzle bots, customized aesthetic board skins, local and multiplayer visual match rooms, and complete offline interactive tournament modes.",
                    sizeMb = 42.5,
                    downloadCount = 120,
                    isApproved = true
                ),
                AppEntity(
                    id = "task_flow",
                    name = "TaskFlow Calendar",
                    developerName = "Aura Productivity",
                    packageName = "com.auraprod.taskflow",
                    category = "Productivity",
                    iconName = "edit_calendar",
                    rating = 4.3f,
                    ratingCount = 5231,
                    isPaid = false,
                    price = 0.0,
                    description = "Achieve peak clarity and conquer daily workflows with TaskFlow. Combines a elegant Kanban board interface, smart notification alerts, task priority tags, dark theme color palettes, local file backups, and clean visual widgets to organize your checklist directly on your device screen.",
                    sizeMb = 14.8,
                    downloadCount = 3105,
                    isApproved = true
                ),
                AppEntity(
                    id = "markdown_pro",
                    name = "Writer's Pad Pro",
                    developerName = "ScriptScribe Ltd",
                    packageName = "com.scriptscribe.writerspad",
                    category = "Productivity",
                    iconName = "history_edu",
                    rating = 4.7f,
                    ratingCount = 412,
                    isPaid = true,
                    price = 4.99,
                    description = "A professional, minimalist Markdown notepad interface designed for novelists, academic students, and programmers. Supports syntax highlighting, offline backup compilation, PDF/HTML document rendering, cloud synchronize exports, and complete document management under nested folders.",
                    sizeMb = 12.1,
                    downloadCount = 160,
                    isApproved = true
                ),
                AppEntity(
                    id = "chat_wave",
                    name = "ChatWave Secure",
                    developerName = "WaveLink Labs",
                    packageName = "com.wavelink.chatwave",
                    category = "Social",
                    iconName = "forum",
                    rating = 4.5f,
                    ratingCount = 9812,
                    isPaid = false,
                    price = 0.0,
                    description = "Interact securely with friends using ChatWave. Focuses on local file caching, biometric passcode authentication locks, customizable group chats, beautiful voice memo filters, and integrated picture compression for smooth communications.",
                    sizeMb = 28.6,
                    downloadCount = 15302,
                    isApproved = true
                ),
                AppEntity(
                    id = "clean_speed",
                    name = "Swift Booster & Cleaner",
                    developerName = "Apex Tools",
                    packageName = "com.apextools.swiftbooster",
                    category = "Tools",
                    iconName = "bolt",
                    rating = 3.8f,
                    ratingCount = 18940,
                    isPaid = false,
                    price = 0.0,
                    description = "Inspect and delete application junk files safely! Offers visual system storage analysis lists, mock background RAM optimization widgets, deep temperature logs, and cache clean-up buttons.",
                    sizeMb = 6.4,
                    downloadCount = 51090,
                    isApproved = true
                ),
                AppEntity(
                    id = "vpn_shield",
                    name = "FileShield VPN Secure",
                    developerName = "CyberShield Security",
                    packageName = "com.cybershield.vpntraffic",
                    category = "Tools",
                    iconName = "vpn_key",
                    rating = 4.4f,
                    ratingCount = 2031,
                    isPaid = true,
                    price = 5.99,
                    description = "Protect your online footprint with zero logging. Includes customized visual server listings across multiple mocked geolocations, one-tap connection switches, rapid speed checks, and reliable protective configurations.",
                    sizeMb = 18.2,
                    downloadCount = 4220,
                    isApproved = true
                ),
                AppEntity(
                    id = "wallet_wise",
                    name = "WalletWise Expense Tracker",
                    developerName = "Centum Ledger Group",
                    packageName = "com.centum.walletwise",
                    category = "Finance",
                    iconName = "account_balance_wallet",
                    rating = 4.5f,
                    ratingCount = 1320,
                    isPaid = false,
                    price = 0.0,
                    description = "Track expenses, budget, and visualize savings effortlessly. Perfect for families, freelancers, and small business owners. Includes beautiful interactive visual charts of financial performance offline.",
                    sizeMb = 11.5,
                    downloadCount = 3720,
                    isApproved = true
                )
            )
            appDao.insertApps(initialApps)

            // Populate some initial reviews
            for (app in initialApps) {
                reviewDao.insertReview(
                    ReviewEntity(
                        appId = app.id,
                        userName = "Sabbir Rahman",
                        rating = app.rating,
                        comment = "Ata khub sundor app! Chokh bondho kore download korte paren. Design r performance darun."
                    )
                )
                reviewDao.insertReview(
                    ReviewEntity(
                        appId = app.id,
                        userName = "Tasnim Chowdhury",
                        rating = (app.rating - 1.0f).coerceAtLeast(3.0f),
                        comment = "Bhalo, kintu arektu update dorkar. Overall functionality smoothly cholche bhaloi dakey."
                    )
                )
            }
        }

        // Prepopulate standard default User profile
        val currentUser = userDao.getUserProfileSuspend()
        if (currentUser == null) {
            userDao.insertUserProfile(
                UserEntity(
                    email = "ildrimrayhan7@gmail.com",
                    displayName = "Rayhan Ildrim",
                    walletBalance = 150.0, // Preloaded loaded balance
                    isBiometricEnabled = true,
                    appUsageStats = "Games:2.4hrs,Productivity:1.2hrs,Tools:0.5hrs",
                    sessionDownloadedCount = 2
                )
            )
        }

        // Populate a welcome push notification
        val currentNotifications = notificationDao.getAllNotifications().first()
        if (currentNotifications.isEmpty()) {
            notificationDao.insertNotification(
                NotificationEntity(
                    title = "Welcome to PlaySpace!",
                    message = "Explore top rated free and paid apps. Try our fully features offline download & developer sandbox features."
                )
            )
        }
    }

    // Simulate downloading an app reactively
    suspend fun simulateAppDownload(appId: String, onProgressChange: (Int) -> Unit) = withContext(Dispatchers.IO) {
        val app = appDao.getAppByIdSuspend(appId) ?: return@withContext
        
        // Mark as starting
        appDao.updateDownloadProgress(appId, "DOWNLOADING", 0)
        onProgressChange(0)

        // Progress mock sequence
        for (progress in 5..100 step 15) {
            delay(500) // Half second delay to feel real
            val targetProgress = if (progress > 100) 100 else progress
            appDao.updateDownloadProgress(appId, "DOWNLOADING", targetProgress)
            onProgressChange(targetProgress)
        }

        // Finish successfully
        appDao.markAsDownloaded(appId)
        onProgressChange(100)

        // Increment downloaded app count in User profile
        val user = userDao.getUserProfileSuspend()
        if (user != null) {
            userDao.insertUserProfile(
                user.copy(sessionDownloadedCount = user.sessionDownloadedCount + 1)
            )
        }

        // Put a local simulation update push notification
        notificationDao.insertNotification(
            NotificationEntity(
                title = "App Installed successfully!",
                message = "${app.name} is now available offline in your local PlaySpace library."
            )
        )
    }

    // Upload App as a Developer
    suspend fun developerUploadApp(
        id: String,
        name: String,
        packageName: String,
        category: String,
        description: String,
        isPaid: Boolean,
        price: Double,
        sizeMb: Double,
        iconName: String
    ) = withContext(Dispatchers.IO) {
        val newApp = AppEntity(
            id = id,
            name = name,
            developerName = "Your Dev Studio",
            packageName = packageName,
            category = category,
            iconName = iconName,
            rating = 0.0f,
            ratingCount = 0,
            isPaid = isPaid,
            price = price,
            description = description,
            sizeMb = sizeMb,
            isApproved = false, // Require Admin Moderation Approval!
            uploadTimestamp = System.currentTimeMillis()
        )
        appDao.insertApp(newApp)

        // Push notification that developer uploaded app
        notificationDao.insertNotification(
            NotificationEntity(
                title = "New App Submitted!",
                message = "\"$name\" has been submitted for moderation reviews. Admins can view and approve it."
            )
        )
    }

    // Moderate/Update App Approval Status
    suspend fun moderateApp(appId: String, approve: Boolean) = withContext(Dispatchers.IO) {
        val app = appDao.getAppByIdSuspend(appId) ?: return@withContext
        if (approve) {
            appDao.updateApp(app.copy(isApproved = true))
            notificationDao.insertNotification(
                NotificationEntity(
                    title = "Developer App Approved!",
                    message = "Congratulations! Custom App \"${app.name}\" has been approved and is now active for all users."
                )
            )
        } else {
            appDao.deleteApp(app)
            notificationDao.insertNotification(
                NotificationEntity(
                    title = "App Submission Declined",
                    message = "\"${app.name}\" failed visual guideline checks and was removed from moderation."
                )
            )
        }
    }

    // Submit Review with automated ratings calculations
    suspend fun submitReview(appId: String, userName: String, rating: Float, comment: String) = withContext(Dispatchers.IO) {
        val app = appDao.getAppByIdSuspend(appId) ?: return@withContext
        
        // Add Review
        reviewDao.insertReview(
            ReviewEntity(appId = appId, userName = userName, rating = rating, comment = comment)
        )

        // Adjust rating of App
        val totalCount = app.ratingCount + 1
        val newRating = ((app.rating * app.ratingCount) + rating) / totalCount
        
        appDao.updateApp(
            app.copy(
                rating = String.format("%.1f", newRating).toFloat(),
                ratingCount = totalCount,
                userRating = rating
            )
        )
    }

    // Secure simulated payment gateway processing
    suspend fun processSecurePayment(appId: String, paymentMethod: String, amount: Double): Boolean = withContext(Dispatchers.IO) {
        val user = userDao.getUserProfileSuspend() ?: return@withContext false
        if (user.walletBalance >= amount) {
            // Deduct
            userDao.updateWalletBalance(user.email, user.walletBalance - amount)
            
            // Add notification transaction confirmation
            notificationDao.insertNotification(
                NotificationEntity(
                    title = "Payment Processed securely",
                    message = "Spent $$amount via $paymentMethod for purchase. Remaining balance: $${"%.2f".format(user.walletBalance - amount)}"
                )
            )
            return@withContext true
        }
        return@withContext false
    }

    // Crash Simulation reporting
    suspend fun reportCrash(appName: String, packageName: String, exceptionMessage: String, stackTrace: String) = withContext(Dispatchers.IO) {
        val crash = CrashLogEntity(
            appName = appName,
            packageName = packageName,
            exceptionMessage = exceptionMessage,
            stackTrace = stackTrace,
            status = "ACTIVE"
        )
        crashLogDao.insertCrashLog(crash)

        notificationDao.insertNotification(
            NotificationEntity(
                title = "Automated Crash Detected!",
                message = "Crash in $appName reported dynamically. View telemetry stack trace."
            )
        )
    }

    // Resolve a reported crash
    suspend fun resolveCrash(crashId: Int) = withContext(Dispatchers.IO) {
        crashLogDao.resolveCrash(crashId)
    }

    // Update biometric enrollment configuration
    suspend fun updateBiometricEnrollment(email: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        val user = userDao.getUserProfileSuspend() ?: return@withContext
        userDao.insertUserProfile(user.copy(isBiometricEnabled = enabled))
    }

    // Top Up Wallet Balance
    suspend fun topUpWallet(email: String, amount: Double) = withContext(Dispatchers.IO) {
        val user = userDao.getUserProfileSuspend() ?: return@withContext
        userDao.updateWalletBalance(email, user.walletBalance + amount)
    }

    // Clear alert history
    suspend fun clearAllNotifications() = withContext(Dispatchers.IO) {
        notificationDao.clearAll()
    }
}
