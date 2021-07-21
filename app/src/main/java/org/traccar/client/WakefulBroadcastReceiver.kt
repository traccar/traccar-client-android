/*
 * Copyright 2021 Anton Tananaev (anton@traccar.org)
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
import android.util.SparseArray
import android.os.PowerManager.WakeLock
import android.content.Intent
import androidx.core.content.ContextCompat
import android.os.PowerManager

abstract class WakefulBroadcastReceiver : BroadcastReceiver() {

    companion object {

        private const val EXTRA_WAKE_LOCK_ID = "android.support.content.wakelockid"
        private val activeWakeLocks = SparseArray<WakeLock>()
        private var nextId = 1

        fun startWakefulForegroundService(context: Context, intent: Intent) {
            synchronized(activeWakeLocks) {
                val id = nextId
                nextId += 1
                if (nextId <= 0) {
                    nextId = 1
                }
                intent.putExtra(EXTRA_WAKE_LOCK_ID, id)
                ContextCompat.startForegroundService(context, intent)
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                val wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    WakefulBroadcastReceiver::class.java.simpleName
                )
                wakeLock.setReferenceCounted(false)
                wakeLock.acquire((60 * 1000).toLong())
                activeWakeLocks.put(id, wakeLock)
            }
        }

        fun completeWakefulIntent(intent: Intent?): Boolean {
            val id = intent?.getIntExtra(EXTRA_WAKE_LOCK_ID, 0) ?: 0
            if (id == 0) {
                return false
            }
            synchronized(activeWakeLocks) {
                val wakeLock = activeWakeLocks[id]
                if (wakeLock != null) {
                    wakeLock.release()
                    activeWakeLocks.remove(id)
                    return true
                }
                return true
            }
        }
    }
}
