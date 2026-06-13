package com.example.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps ORDER BY rating DESC, downloadCount DESC")
    fun getAllApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE id = :id LIMIT 1")
    fun getAppById(id: String): Flow<AppEntity?>

    @Query("SELECT * FROM apps WHERE id = :id LIMIT 1")
    suspend fun getAppByIdSuspend(id: String): AppEntity?

    @Query("SELECT * FROM apps WHERE category = :category AND isApproved = 1")
    fun getAppsByCategory(category: String): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchApps(query: String): Flow<List<AppEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppEntity>)

    @Update
    suspend fun updateApp(app: AppEntity)

    @Query("UPDATE apps SET localDownloadStatus = :status, downloadProgress = :progress WHERE id = :id")
    suspend fun updateDownloadProgress(id: String, status: String, progress: Int)

    @Query("UPDATE apps SET downloadCount = downloadCount + 1, localDownloadStatus = 'DOWNLOADED', downloadProgress = 100 WHERE id = :id")
    suspend fun markAsDownloaded(id: String)

    @Delete
    suspend fun deleteApp(app: AppEntity)
}

@Dao
interface ReviewDao {
    @Query("SELECT * FROM reviews WHERE appId = :appId ORDER BY timestamp DESC")
    fun getReviewsForApp(appId: String): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}

@Dao
interface CrashLogDao {
    @Query("SELECT * FROM crash_logs ORDER BY timestamp DESC")
    fun getAllCrashLogs(): Flow<List<CrashLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrashLog(log: CrashLogEntity)

    @Query("UPDATE crash_logs SET status = 'RESOLVED' WHERE id = :id")
    suspend fun resolveCrash(id: Int)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM user_profiles LIMIT 1")
    fun getUserProfile(): Flow<UserEntity?>

    @Query("SELECT * FROM user_profiles LIMIT 1")
    suspend fun getUserProfileSuspend(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(user: UserEntity)

    @Query("UPDATE user_profiles SET walletBalance = :balance WHERE email = :email")
    suspend fun updateWalletBalance(email: String, balance: Double)
}
