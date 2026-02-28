package com.dailywell.android.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dailywell.app.notification.ProactiveNotificationManager

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            // Re-initialize notification workers after device reboot
            // WorkManager persists across reboots, but explicit re-init is safer
            ProactiveNotificationManager.initialize(context)
        }
    }
}
