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

    public static String formatRequest(String address, int port, boolean secure, Position position) {
        return formatRequest(address, port, secure, position, null);
    }

    public static String formatRequest(String address, int port, boolean secure, Position position, String alarm) {

        Uri.Builder builder = new Uri.Builder();
        builder.scheme(secure ? "https" : "http").encodedAuthority(address + ':' + port)
                .appendQueryParameter("id", position.getDeviceId())
                .appendQueryParameter("timestamp", String.valueOf(position.getTime().getTime() / 1000))
                .appendQueryParameter("lat", String.valueOf(position.getLatitude()))
                .appendQueryParameter("lon", String.valueOf(position.getLongitude()))
                .appendQueryParameter("speed", String.valueOf(position.getSpeed()))
                .appendQueryParameter("bearing", String.valueOf(position.getCourse()))
                .appendQueryParameter("altitude", String.valueOf(position.getAltitude()))
                .appendQueryParameter("batt", String.valueOf(position.getBattery()));

        if (alarm != null) {
            builder.appendQueryParameter("alarm", alarm);
        }

        return builder.build().toString();
    }

}
