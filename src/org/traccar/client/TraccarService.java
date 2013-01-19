/*
 * Copyright 2012 - 2013 Anton Tananaev (anton.tananaev@gmail.com)
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
import android.os.IBinder;
import android.preference.PreferenceManager;

/**
 * Background service
 */
public class TraccarService extends Service {

    private String id;
    private String address;
    private int port;
    private int interval;
    private String provider;

    private SharedPreferences sharedPreferences;
    private ClientController clientController;
    private LocationManager locationManager;

    @Override
    public void onCreate() {
        StatusActivity.addMessage(getString((R.string.status_service_create)));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        StatusActivity.addMessage(getString((R.string.status_service_start)));

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        id = sharedPreferences.getString(TraccarActivity.KEY_ID, null);
        address = sharedPreferences.getString(TraccarActivity.KEY_ADDRESS, null);
        port = Integer.valueOf(sharedPreferences.getString(TraccarActivity.KEY_PORT, null));
        interval = Integer.valueOf(sharedPreferences.getString(TraccarActivity.KEY_INTERVAL, null));
        provider = sharedPreferences.getString(TraccarActivity.KEY_PROVIDER, null);

        clientController = new ClientController(this, address, port, Protocol.createLoginMessage(id));
        clientController.start();

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(provider, interval * 1000, 0, locationListener);

        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        StatusActivity.addMessage(getString((R.string.status_service_destroy)));

        sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);

        locationManager.removeUpdates(locationListener);

        clientController.stop();
    }

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            StatusActivity.addMessage(getString((R.string.status_location_update)));
            clientController.setNewLocation(Protocol.createLocationMessage(location));
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
            StatusActivity.addMessage(getString((R.string.status_preference_update)));
            if (key.equals(TraccarActivity.KEY_ADDRESS)) {
                address = sharedPreferences.getString(TraccarActivity.KEY_ADDRESS, null);
                clientController.setNewServer(address, port);
            } else if (key.equals(TraccarActivity.KEY_PORT)) {
                port = Integer.valueOf(sharedPreferences.getString(TraccarActivity.KEY_PORT, null));
                clientController.setNewServer(address, port);
            } else if (key.equals(TraccarActivity.KEY_INTERVAL)) {
                interval = Integer.valueOf(sharedPreferences.getString(TraccarActivity.KEY_INTERVAL, null));
                locationManager.removeUpdates(locationListener);
                locationManager.requestLocationUpdates(provider, interval * 1000, 0, locationListener);
            } else if (key.equals(TraccarActivity.KEY_ID)) {
                id = sharedPreferences.getString(TraccarActivity.KEY_ID, null);
                clientController.setNewLogin(Protocol.createLoginMessage(id));
            } else if (key.equals(TraccarActivity.KEY_PROVIDER)) {
                provider = sharedPreferences.getString(TraccarActivity.KEY_PROVIDER, null);
                locationManager.removeUpdates(locationListener);
                locationManager.requestLocationUpdates(provider, interval * 1000, 0, locationListener);
            }
        }

    };

}
