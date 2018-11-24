/*
 * Copyright 2013 - 2017 Anton Tananaev (anton.tananaev@gmail.com)
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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import static android.content.Context.SENSOR_SERVICE;
import static android.hardware.Sensor.REPORTING_MODE_ON_CHANGE;

public class PositionProvider implements LocationListener, SensorEventListener {

    private static final String TAG = PositionProvider.class.getSimpleName();

    private static final int MINIMUM_INTERVAL = 1000;

    public interface PositionListener {
        void onPositionUpdate(Position position);
    }

    private final PositionListener listener;

    private final Context context;
    private SharedPreferences preferences;
    private LocationManager locationManager;

    private String deviceId;
    private long interval;
    private double distance;
    private double angle;

    private Location lastLocation;

    private SensorManager sensorManager;
    private Sensor temperatureSensor;
    private Float temperature;

    public PositionProvider(Context context, PositionListener listener) {
        this.context = context;
        this.listener = listener;

        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        preferences = PreferenceManager.getDefaultSharedPreferences(context);

        deviceId = preferences.getString(MainFragment.KEY_DEVICE, "undefined");
        interval = Long.parseLong(preferences.getString(MainFragment.KEY_INTERVAL, "600")) * 1000;
        distance = Integer.parseInt(preferences.getString(MainFragment.KEY_DISTANCE, "0"));
        angle = Integer.parseInt(preferences.getString(MainFragment.KEY_ANGLE, "0"));
        if (preferences.contains(MainFragment.KEY_TEMPERATURE)) {
            temperature = preferences.getFloat(MainFragment.KEY_TEMPERATURE, 0);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
            temperatureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @SuppressLint("MissingPermission")
    public void startUpdates() {
        try {
            locationManager.requestLocationUpdates(
                    getProvider(preferences.getString(MainFragment.KEY_ACCURACY, "medium")),
                    distance > 0 || angle > 0 ? MINIMUM_INTERVAL : interval, 0, this);
        } catch (RuntimeException e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }
        if (temperatureSensor != null) {
            sensorManager.registerListener(this, temperatureSensor, REPORTING_MODE_ON_CHANGE);
        }
    }

    public static String getProvider(String accuracy) {
        switch (accuracy) {
            case "high":
                return LocationManager.GPS_PROVIDER;
            case "low":
                return LocationManager.PASSIVE_PROVIDER;
            default:
                return LocationManager.NETWORK_PROVIDER;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null && (lastLocation == null
                || location.getTime() - lastLocation.getTime() >= interval
                || distance > 0 && location.distanceTo(lastLocation) >= distance
                || angle > 0 && Math.abs(location.getBearing() - lastLocation.getBearing()) >= angle)) {
            Log.i(TAG, "location new");
            lastLocation = location;
            listener.onPositionUpdate(new Position(deviceId, location, getBatteryLevel(context), temperature));
        } else {
            Log.i(TAG, location != null ? "location ignored" : "location nil");
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    public void stopUpdates() {
        locationManager.removeUpdates(this);
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    public static double getBatteryLevel(Context context) {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent != null) {
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
            return (level * 100.0) / scale;
        }
        return 0;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            temperature = event.values[0];
            preferences.edit().putFloat(MainFragment.KEY_TEMPERATURE, temperature).apply();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

}
