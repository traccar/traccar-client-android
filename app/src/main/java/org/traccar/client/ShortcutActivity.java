/*
 * Copyright 2016 - 2017 Anton Tananaev (anton.tananaev@gmail.com)
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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class ShortcutActivity extends Activity {

    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_ACTION_START = "start";
    public static final String EXTRA_ACTION_STOP = "stop";
    public static final String EXTRA_ACTION_SOS = "sos";

    private static final String ALARM_SOS = "sos";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkShortcutAction(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkShortcutAction(intent);
    }

    @SuppressWarnings("MissingPermission")
    private void sendAlarm() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (location == null) {
            location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        }

        if (location != null) {

            Position position = new Position(
                    preferences.getString(MainActivity.KEY_DEVICE, null),
                    location, PositionProvider.getBatteryLevel(this));

            String request = ProtocolFormatter.formatRequest(
                    preferences.getString(MainActivity.KEY_ADDRESS, null),
                    Integer.parseInt(preferences.getString(MainActivity.KEY_PORT, null)),
                    preferences.getBoolean(MainActivity.KEY_SECURE, false),
                    position, ALARM_SOS);

            RequestManager.sendRequestAsync(request, new RequestManager.RequestHandler() {
                @Override
                public void onComplete(boolean success) {
                    if (success) {
                        Toast.makeText(ShortcutActivity.this, R.string.status_send_success, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(ShortcutActivity.this, R.string.status_send_fail, Toast.LENGTH_SHORT).show();
                    }
                }
            });

        } else {
            Toast.makeText(this, R.string.status_send_fail, Toast.LENGTH_SHORT).show();
        }
    }

    private void checkShortcutAction(Intent intent) {
        String action;
        if (intent.hasExtra("shortcutAction")) {
            action = intent.getBooleanExtra("shortcutAction", false)
                    ? EXTRA_ACTION_START : EXTRA_ACTION_STOP;
        } else {
            action = intent.getStringExtra(EXTRA_ACTION);
        }
        switch (action) {
            case EXTRA_ACTION_START:
                PreferenceManager.getDefaultSharedPreferences(this)
                        .edit().putBoolean(MainActivity.KEY_STATUS, true).commit();
                startService(new Intent(this, TrackingService.class));
                Toast.makeText(this, R.string.status_service_create, Toast.LENGTH_SHORT).show();
                break;
            case EXTRA_ACTION_STOP:
                PreferenceManager.getDefaultSharedPreferences(this)
                        .edit().putBoolean(MainActivity.KEY_STATUS, false).commit();
                stopService(new Intent(this, TrackingService.class));
                Toast.makeText(this, R.string.status_service_destroy, Toast.LENGTH_SHORT).show();
                break;
            case EXTRA_ACTION_SOS:
                sendAlarm();
                break;
        }
        finish();
    }

}
