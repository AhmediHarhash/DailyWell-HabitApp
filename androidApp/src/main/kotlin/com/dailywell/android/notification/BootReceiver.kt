package com.dailywell.android.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Re-schedule notifications after device reboot
            // This would use the NotificationScheduler to reschedule
            // based on saved preferences
        }
    }
}
