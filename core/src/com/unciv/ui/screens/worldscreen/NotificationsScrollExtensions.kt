package com.unciv.ui.screens.worldscreen

import com.unciv.GUI
import com.unciv.logic.civilization.Notification

/** Hide a single notification from the current display until the category is restored */
internal fun NotificationsScroll.hideNotification(notification: Notification) {
    hiddenNotifications.add(notification)
    GUI.setUpdateWorldOnNextRender()
}

/** Restore all hidden notifications in a given category; returns true if any were restored */
internal fun NotificationsScroll.restoreHiddenInCategory(category: Notification.NotificationCategory): Boolean {
    val restored = hiddenNotifications.removeIf { it.category == category }
    if (restored) GUI.setUpdateWorldOnNextRender()
    return restored
}
