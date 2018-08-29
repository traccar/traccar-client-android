/*
 * Copyright 2012 - 2016 Anton Tananaev (anton.tananaev@gmail.com)
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

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;

public class ProtocolFormatter {

    public static final String KEY_INTERVAL = "interval";

    public static String formatRequest(Context context, String url, Position position) {
        return formatRequest(context, url, position, null);
    }

    public static String formatRequest(Context context, String url, Position position, String alarm) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);

        int interval = Integer.parseInt(preferences.getString(KEY_INTERVAL, "600")) + 5;

        Uri serverUrl = Uri.parse(url);
        Uri.Builder builder = serverUrl.buildUpon()
                .appendQueryParameter("id", position.getDeviceId())
                .appendQueryParameter("timestamp", String.valueOf(position.getTime().getTime() / 1000))
                .appendQueryParameter("lat", String.valueOf(position.getLatitude()))
                .appendQueryParameter("lon", String.valueOf(position.getLongitude()))
                .appendQueryParameter("speed", String.valueOf(position.getSpeed()))
                .appendQueryParameter("bearing", String.valueOf(position.getCourse()))
                .appendQueryParameter("altitude", String.valueOf(position.getAltitude()))
                .appendQueryParameter("accuracy", String.valueOf(position.getAccuracy()))
                .appendQueryParameter("batt", String.valueOf(position.getBattery()))
                .appendQueryParameter("reportInterval", String.valueOf(interval * 1000));

        if (position.getMock()) {
            builder.appendQueryParameter("mock", String.valueOf(position.getMock()));
        }

        if (alarm != null) {
            builder.appendQueryParameter("alarm", alarm);
        }

        return builder.build().toString();
    }
}
