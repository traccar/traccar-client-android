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
    protected final long period;

    private long lastUpdateTime;

    public PositionProvider(Context context, PositionListener listener) {
        this.context = context;
        this.listener = listener;

        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        deviceId = preferences.getString(MainActivity.KEY_DEVICE, null);
        period = Long.parseLong(preferences.getString(MainActivity.KEY_INTERVAL, null)) * 1000;

        type = preferences.getString(MainActivity.KEY_PROVIDER, "gps");
    }

    public abstract void startUpdates();

    public abstract void stopUpdates();

    protected void updateLocation(Location location) {
        if (location != null && location.getTime() - lastUpdateTime >= period) {
            Log.i(TAG, "location new");
            lastUpdateTime = location.getTime();
            listener.onPositionUpdate(new Position(deviceId, location, getBatteryLevel(context)));
        } else {
            Log.i(TAG, location != null ? "location old" : "location nil");
        }
    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    public static double getBatteryLevel(Context context) {
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
