/*
 * Copyright 2020 Anton Tananaev (anton@traccar.org)
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
package org.traccar.client;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class ServiceReceiver extends BroadcastReceiver {

    public static final String KEY_DURATION = "serviceTime";

    private static long startTime = 0;
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (TrackingService.ACTION_STARTED.equals(intent.getAction())) {
            startTime = System.currentTimeMillis();
        } else {
            if (startTime > 0) {
                updateTime(context, System.currentTimeMillis() - startTime);
                startTime = 0;
            }
        }
    }

    private void updateTime(Context context, long duration) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        long totalDuration = preferences.getLong(KEY_DURATION, 0);
        preferences.edit().putLong(KEY_DURATION, totalDuration + duration).apply();
    }

}
