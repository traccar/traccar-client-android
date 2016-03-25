/*
 * Copyright 2013 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

public abstract class PositionProvider {

    protected static final String TAG = PositionProvider.class.getSimpleName();

    public interface PositionListener {
        void onPositionUpdate(Position position);
    }

    private final PositionListener listener;

    private final Context context;
    private final SharedPreferences preferences;
    protected final LocationManager locationManager;

    private String deviceId;
    protected String type;
    protected boolean stateCarMode = false;
    protected final long period;

    protected Location lastLocation = null;
    protected final long periodFast;
    protected final long minDistance;
    protected final boolean carMode;

    private long lastUpdateTime;

    public PositionProvider(Context context, PositionListener listener) {
        this.context = context;
        this.listener = listener;

        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        deviceId = preferences.getString(MainActivity.KEY_DEVICE, null);
        carMode = preferences.getBoolean(MainActivity.KEY_CARMODE, false);
        period = Integer.parseInt(preferences.getString(MainActivity.KEY_INTERVAL, null)) * 1000;
        periodFast = Integer.parseInt(preferences.getString(MainActivity.KEY_INTERVAL_FAST, null)) * 1000;
        minDistance = Integer.parseInt(preferences.getString(MainActivity.KEY_DISTANCE_START, null));

        type = preferences.getString(MainActivity.KEY_PROVIDER, null);
    }

    public abstract void startUpdates();

    public abstract void startUpdatesFast();

    public abstract void stopUpdates();

    protected void updateLocation(Location location) {
        if(lastLocation == null) {
            Log.i(TAG, "init lastLocation");
            lastLocation = location;
        }

        if(carMode) {
            if(stateCarMode == false) {
                if (lastLocation.distanceTo(location) > minDistance) {
                    Log.i(TAG, "update switch : sleepMode => CarMode");
                    stateCarMode = true;
                    stopUpdates();
                    startUpdatesFast();
                }
            } else {

                if(lastLocation.distanceTo(location) > minDistance) { // moving car
                    lastLocation = location;
                }

                if(location.getTime() - lastLocation.getTime() > period) { // not moving while normal period time
                    Log.i(TAG, "update switch : carMode => sleepMode");
                    stateCarMode = false;
                    stopUpdates();
                    startUpdates();
                } else if(lastLocation.distanceTo(location) == 0) { // No timeout but no moving, so we don't need data to send
                    return;
                }

            }
        }

        if (location != null && location.getTime() != lastUpdateTime) {
            Log.i(TAG, "location new");
            lastUpdateTime = location.getTime();
            listener.onPositionUpdate(new Position(deviceId, location, getBatteryLevel()));
        } else {
            Log.i(TAG, location != null ? "location old" : "location nil");
        }
    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    private double getBatteryLevel() {
        if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR) {
            Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
            return (level * 100.0) / scale;
        } else {
            return 0;
        }
    }

}
