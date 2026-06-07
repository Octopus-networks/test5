package com.mithaq.app.domain.model

/**
 * Phase 13C — per-user notification preferences.
 *
 * Stored at users/{uid}/notificationSettings/preferences (owner-only). All toggles
 * default to true: a missing settings document, a missing field, or a read failure
 * all mean "notifications enabled" so users never silently stop receiving alerts.
 *
 * [notificationsEnabled] is the master switch; when false every push is skipped
 * regardless of the per-category toggles.
 */
data class NotificationSettings(
    val notificationsEnabled: Boolean = true,
    val interestRequestNotifications: Boolean = true,
    val photoRequestNotifications: Boolean = true,
    val chatRequestNotifications: Boolean = true,
    val messageNotifications: Boolean = true,
    val photoModerationNotifications: Boolean = true
)
