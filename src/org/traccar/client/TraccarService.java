/*
 * Copyright 2012 Anton Tananaev (anton.tananaev@gmail.com)
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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;

/**
 * Background service
 */
public class TraccarService extends Service {

    public static final long RECONNECT_DELAY = 10 * 1000;

    private String id;
    private String address;
    private int port;
    private int interval;

    private Handler handler;
    private Connection connection;
    LocationManager locationManager;

    private void statusMessage(int message) {
        StatusActivity.addMessage(getString(message));
    }

    @Override
    public void onCreate() {
        statusMessage(R.string.status_service_create);

        handler = new Handler();
        connection = new Connection(connectionHandler);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        statusMessage(R.string.status_service_start);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        updateServerPreferences(sharedPreferences);
        updateIntervalPreferences(sharedPreferences);
        updateOtherPreferences(sharedPreferences);

        connection.close();
        connection.connect(address, port);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval * 1000, 0, locationListener);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        statusMessage(R.string.status_service_destroy);

        locationManager.removeUpdates(locationListener);

        handler.removeCallbacksAndMessages(null);
        connection.close();
        connection = null;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

    private boolean updateServerPreferences(SharedPreferences sharedPreferences) {
        boolean changed = false;

        String address = sharedPreferences.getString(TraccarActivity.KEY_ADDRESS, null);
        if (!address.equals(this.address)) {
            this.address = address;
            changed = true;
        }

        int port = Integer.valueOf(sharedPreferences.getString(TraccarActivity.KEY_PORT, null));
        if (port != this.port) {
            this.port = port;
            changed = true;
        }

        return changed;
    }

    private boolean updateIntervalPreferences(SharedPreferences sharedPreferences) {
        boolean changed = false;

        int interval = Integer.valueOf(sharedPreferences.getString(TraccarActivity.KEY_INTERVAL, null));
        if (interval != this.interval) {
            this.interval = interval;
            changed = true;
        }

        return changed;
    }

    private boolean updateOtherPreferences(SharedPreferences sharedPreferences) {
        id = sharedPreferences.getString(TraccarActivity.KEY_ID, null);
        return false;
    }

    private void reconnect() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                connection.close();
                connection.connect(address, port);
            }
        }, RECONNECT_DELAY);
    }

    private Connection.ConnectionHandler connectionHandler = new Connection.ConnectionHandler() {

        @Override
        public void onConnected(boolean result) {
            if (result) {
                statusMessage(R.string.status_connection_success);
                connection.send(Protocol.createLoginMessage(id));
            } else {
                statusMessage(R.string.status_connection_fail);
                reconnect();
            }
        }

        @Override
        public void onSent(boolean result) {
            if (!result) {
                statusMessage(R.string.status_send_fail);
                reconnect();
            }
        }

    };

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            statusMessage(R.string.status_location_update);
            connection.send(Protocol.createLocationMessage(location));
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

    };

    OnSharedPreferenceChangeListener preferenceChangeListener = new OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            statusMessage(R.string.status_preference_update);

            if (updateServerPreferences(sharedPreferences)) {
                connection.close();
                connection.connect(address, port);
            }

            if (updateIntervalPreferences(sharedPreferences)) {
                locationManager.removeUpdates(locationListener);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval * 1000, 0, locationListener);
            }

            updateOtherPreferences(sharedPreferences);
        }

    };

}
