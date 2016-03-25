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
    protected final long timeoutFast;
    protected final long minDistance;
    protected final boolean carMode;

    protected final boolean add_charging;
    protected final boolean add_provider;

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
        timeoutFast = Integer.parseInt(preferences.getString(MainActivity.KEY_TIMEOUT_FAST, null)) * 1000;
        minDistance = Integer.parseInt(preferences.getString(MainActivity.KEY_DISTANCE_START, null));

        add_charging = preferences.getBoolean(MainActivity.KEY_ADD_CHARGING, false);
        add_provider = preferences.getBoolean(MainActivity.KEY_ADD_PROVIDER, false);

        type = preferences.getString(MainActivity.KEY_PROVIDER, null);
    }

    public abstract void startUpdates();

    public abstract void startUpdatesFast();

    public abstract void stopUpdates();

    protected void updateLocation(Location location) {
        Position position = new Position(deviceId, location, getBatteryLevel());

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
                    position.setAdditionalData("carMode", "start");
                }
            } else {
                position.setAdditionalData("carMode", "active");
                if(lastLocation.distanceTo(location) > minDistance) { // moving car
                    lastLocation = location;
                    position.setAdditionalData("carMode", "activeMove");
                }

                if(location.getTime() - lastLocation.getTime() > timeoutFast) { // not moving while normal period time
                    Log.i(TAG, "update switch : carMode => sleepMode");
                    stateCarMode = false;
                    stopUpdates();
                    startUpdates();
                    position.setAdditionalData("carMode", "stop");
                } else if(lastLocation.distanceTo(location) == 0) { // No timeout but no moving, so we don't need data to send
                    return;
                }

            }
        }

        if (location != null && location.getTime() != lastUpdateTime) {
            Log.i(TAG, "location new");
            lastUpdateTime = location.getTime();
            addAdditionalFields(position);
            listener.onPositionUpdate(position);
        } else {
            Log.i(TAG, location != null ? "location old" : "location nil");
        }
    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    private double getBatteryLevel() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
            Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
            return (level * 100.0) / scale;
        } else {
            return 0;
        }
    }

    private void addAdditionalFields(Position position) {
        if(add_provider) {
            position.setAdditionalData("provider", lastLocation.getProvider());
        }

        if(add_charging) {
            position.setAdditionalData("charging", String.valueOf(getCharging()));
        }
    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    private boolean getCharging() {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.ECLAIR) return false;

        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
    }

}
