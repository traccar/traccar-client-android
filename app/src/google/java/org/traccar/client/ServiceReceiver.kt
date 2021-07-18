/*
 * Copyright 2020 - 2021 Anton Tananaev (anton@traccar.org)
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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager

class ServiceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (TrackingService.ACTION_STARTED == intent.action) {
            startTime = System.currentTimeMillis()
        } else if (startTime > 0) {
            updateTime(context, System.currentTimeMillis() - startTime)
            startTime = 0
        }
    }

    private fun updateTime(context: Context, duration: Long) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val totalDuration = preferences.getLong(KEY_DURATION, 0)
        preferences.edit().putLong(KEY_DURATION, totalDuration + duration).apply()
    }

    companion object {
        const val KEY_DURATION = "serviceTime"
        private var startTime: Long = 0
    }
}
