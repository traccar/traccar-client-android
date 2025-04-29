package org.traccar.client

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class TrackingSchedulerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("TrackingScheduler", ">>> Received intent with action: ${intent.action}")
        when (intent.action) {
            "START_TRACKING" -> {
                ContextCompat.startForegroundService(context, Intent(context, TrackingService::class.java))
            }
            "STOP_TRACKING" -> {
                context.stopService(Intent(context, TrackingService::class.java))
            }
        }
    }
}
