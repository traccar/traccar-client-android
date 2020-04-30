/*
 * Copyright 2015 - 2019 Anton Tananaev (anton@traccar.org)
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

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class RequestData {

    public static String get(Position position) {
        return get(position, null);
    }

    public static String get(Position position, String alarm) {
        JSONObject jsonParam = new JSONObject();

        try {
            jsonParam.put("id", position.getDeviceId());
            jsonParam.put("timestamp", String.valueOf(position.getTime().getTime() / 1000));
            jsonParam.put("lat", String.valueOf(position.getLatitude()));
            jsonParam.put("lon", String.valueOf(position.getLongitude()));
            jsonParam.put("speed", String.valueOf(position.getSpeed()));
            jsonParam.put("bearing", String.valueOf(position.getCourse()));
            jsonParam.put("altitude", String.valueOf(position.getAltitude()));
            jsonParam.put("accuracy", String.valueOf(position.getAccuracy()));
            jsonParam.put("batt", String.valueOf(position.getBattery()));

            if (position.getMock()) {
                jsonParam.put("mock", String.valueOf(position.getMock()));
            }

            if (alarm != null) {
                jsonParam.put("alarm", alarm);
            }

        } catch (JSONException e) {
            Log.w(RequestData.class.getSimpleName(), e);
            return "";
        }

        return jsonParam.toString();
    }

}