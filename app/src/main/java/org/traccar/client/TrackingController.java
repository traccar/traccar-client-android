/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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
import android.preference.PreferenceManager;

public class TrackingController implements PositionProvider.PositionListener {

    private Context context;
    private SharedPreferences preferences;

    private PositionProvider positionProvider;

    public TrackingController(Context context) {
        this.context = context;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        positionProvider = new PositionProvider(context, this,
                preferences.getString(MainActivity.KEY_ID, null),
                preferences.getString(MainActivity.KEY_PROVIDER, null),
                Integer.parseInt(preferences.getString(MainActivity.KEY_INTERVAL, null)) * 1000);
    }

    public void start() {
        positionProvider.startUpdates();
    }

    public void stop() {
        positionProvider.stopUpdates();
    }

    @Override
    public void onPositionUpdate(Position position) {
        String request = ProtocolFormatter.formatRequest(
                preferences.getString(MainActivity.KEY_ADDRESS, null),
                Integer.parseInt(preferences.getString(MainActivity.KEY_INTERVAL, null)),
                position.getId(), position.location, position.getBattery());

        RequestManager.sendRequest(request, new RequestManager.RequestHandler() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onFailure() {
                StatusActivity.addMessage(context.getString(R.string.status_send_fail));
            }
        });
    }

}
