/*
 * Copyright 2017 - 2021 Anton Tananaev (anton@traccar.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.client

import android.util.Log
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.app.AlarmManager
import android.app.PendingIntent
import java.util.Calendar
import android.content.Context
import android.content.SharedPreferences


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
        setContentView(R.layout.main)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        val uri = intent.data ?: return
        if (uri.scheme == "traccar" && uri.host == "config") {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            prefs.edit().apply {
                uri.getQueryParameter("deviceId")?.let { putString("id", it) }
                uri.getQueryParameter("serverUrl")?.let { putString("url", it) }
                uri.getQueryParameter("accuracy")?.let { putString("accuracy", it) }
                uri.getQueryParameter("interval")?.let { putString("interval", it) }
                uri.getQueryParameter("distance")?.let { putString("distance", it) }
                uri.getQueryParameter("angle")?.let { putString("angle", it) }
                uri.getQueryParameter("startTime")?.let { putString("startTime", it) }
                uri.getQueryParameter("stopTime")?.let { putString("stopTime", it) }
                apply()
            }
            scheduleTrackingFromPreferences()
            val serviceOn = uri.getQueryParameter("service") == "true"
            if (serviceOn) {
                ContextCompat.startForegroundService(this, Intent(this, TrackingService::class.java))
            } else {
                stopService(Intent(this, TrackingService::class.java))
            }

            prefs.edit().putBoolean("status", serviceOn).apply()
            val fragment = supportFragmentManager.findFragmentById(R.id.fragment)
            if (fragment is MainFragment) {
                fragment.updateStatusSwitch(serviceOn)
            }
            Toast.makeText(this, "Traccar konfigurert via QR", Toast.LENGTH_SHORT).show()
        }
    }
    private fun scheduleTrackingFromPreferences() {
    val prefs = PreferenceManager.getDefaultSharedPreferences(this)
    val startTimeStr = prefs.getString("startTime", null)
    val stopTimeStr = prefs.getString("stopTime", null)

    val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager

    fun schedule(timeStr: String?, action: String) {
        if (timeStr == null) return
        val parts = timeStr.split(":")
        if (parts.size != 2) return
        val hour = parts[0].toIntOrNull() ?: return
        val minute = parts[1].toIntOrNull() ?: return

        val time = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (before(java.util.Calendar.getInstance())) add(java.util.Calendar.DATE, 1)
        }

        val intent = Intent(this, TrackingSchedulerReceiver::class.java).apply {
            this.action = action
        }

        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(this, action.hashCode(), intent, flags)

        alarmManager.set(
            android.app.AlarmManager.RTC_WAKEUP,
            time.timeInMillis,
            /*android.app.AlarmManager.INTERVAL_DAY,*/
            pendingIntent
        )
        Log.i("TrackingScheduler", "Scheduled $action at $hour:$minute (${time.time})")
    }

    schedule(startTimeStr, "START_TRACKING")
    schedule(stopTimeStr, "STOP_TRACKING")
}
}