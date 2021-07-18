/*
 * Copyright 2015 - 2021 Anton Tananaev (anton@traccar.org)
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

class DialLaunchReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER)
        if (phoneNumber == LAUNCHER_NUMBER) {
            resultData = null
            val appIntent = Intent(context, MainActivity::class.java)
            appIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(appIntent)
        }
    }

    companion object {
        private const val LAUNCHER_NUMBER = "8722227" // TRACCAR
    }

}
