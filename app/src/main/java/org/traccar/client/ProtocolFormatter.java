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

import android.net.Uri;

public class ProtocolFormatter {

    public static String formatRequest(String url, Position position) {
        return formatRequest(url, position, null);
    }

    public static String formatRequest(String url, Position position, String alarm) {
        Uri serverUrl = Uri.parse(url);
        Uri.Builder builder = serverUrl.buildUpon()
                .appendQueryParameter("id", position.getDeviceId())
                .appendQueryParameter("timestamp", String.valueOf(position.getTime().getTime() / 1000))
                .appendQueryParameter("lat", String.valueOf(position.getLatitude()))
                .appendQueryParameter("lon", String.valueOf(position.getLongitude()))
                .appendQueryParameter("batt", String.valueOf(position.getBattery()));

        if (position.getSpeed() != null) {
            builder.appendQueryParameter("speed", String.valueOf(position.getSpeed()));
        }

        if (position.getCourse() != null) {
            builder.appendQueryParameter("bearing", String.valueOf(position.getCourse()));
        }

        if (position.getAltitude() != null) {
            builder.appendQueryParameter("altitude", String.valueOf(position.getAltitude()));
        }

        if (position.getAccuracy() != null) {
            builder.appendQueryParameter("accuracy", String.valueOf(position.getAccuracy()));
        }

        if (position.getMock()) {
            builder.appendQueryParameter("mock", String.valueOf(position.getMock()));
        }

        if (position.getProvider() != null) {
            builder.appendQueryParameter("provider", position.getProvider());
        }

        builder.appendQueryParameter("setting", position.getSetting())
                .appendQueryParameter("interval", String.valueOf(position.getInterval()));

        if (position.getDelta() != null) {
            builder.appendQueryParameter("delta", String.valueOf(position.getDelta()));
        }

        if (alarm != null) {
            builder.appendQueryParameter("alarm", alarm);
        }

        return builder.build().toString();
    }
}
