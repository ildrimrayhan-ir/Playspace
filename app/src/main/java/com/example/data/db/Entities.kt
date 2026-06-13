package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val id: String,
    val name: String,
    val developerName: String,
    val packageName: String,
    val category: String,
    val iconName: String,
    val rating: Float,
    val ratingCount: Int,
    val isPaid: Boolean,
    val price: Double,
    val description: String,
    val sizeMb: Double,
    val isApproved: Boolean = true,
    val uploadTimestamp: Long = System.currentTimeMillis(),
    val localDownloadStatus: String = "NOT_DOWNLOADED", // "NOT_DOWNLOADED", "DOWNLOADING", "DOWNLOADED"
    val downloadProgress: Int = 0,
    val downloadCount: Int = 0,
    val userRating: Float = 0f
)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appId: String,
    val userName: String,
    val rating: Float,
    val comment: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "crash_logs")
data class CrashLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appName: String,
    val packageName: String,
    val exceptionMessage: String,
    val stackTrace: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "ACTIVE" // "ACTIVE", "RESOLVED"
)

@Entity(tableName = "user_profiles")
data class UserEntity(
    @PrimaryKey val email: String,
    val displayName: String,
    val walletBalance: Double = 100.0, // Default demo balance
    val isBiometricEnabled: Boolean = false,
    val appUsageStats: String = "", // JSON or simple comma separated stats
    val sessionDownloadedCount: Int = 0
)
