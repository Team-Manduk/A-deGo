package com.teammanduk.adego.core.notifications

import android.app.Notification

interface Notifier {
    fun createNotification(): Notification
}
