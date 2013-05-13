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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.location.Location;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Background service
 */
public class TraccarService extends Service {

    public static final String LOG_TAG = "Traccar.TraccarService";

    private String id;
    private String address;
    private int port;
    private int interval;
    private String provider;

    private SharedPreferences sharedPreferences;
    private ClientController clientController;
    private PositionProvider positionProvider;

    @Override
    public void onCreate() {
        StatusActivity.addMessage(getString(R.string.status_service_create));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        StatusActivity.addMessage(getString(R.string.status_service_start));

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            id = sharedPreferences.getString(TraccarActivity.KEY_ID, null);
            address = sharedPreferences.getString(TraccarActivity.KEY_ADDRESS, null);
            provider = sharedPreferences.getString(TraccarActivity.KEY_PROVIDER, null);
            port = Integer.valueOf(sharedPreferences.getString(TraccarActivity.KEY_PORT, null));
            interval = Integer.valueOf(sharedPreferences.getString(TraccarActivity.KEY_INTERVAL, null));
        } catch (Exception error) {
            Log.w(LOG_TAG, error);
        }

        clientController = new ClientController(this, address, port, Protocol.createLoginMessage(id));
        clientController.start();

        positionProvider = new PositionProvider(this, provider, interval * 1000, positionListener);
        positionProvider.startUpdates();

        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        StatusActivity.addMessage(getString(R.string.status_service_destroy));

        if (sharedPreferences != null) {
        	sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        }

        if (positionProvider != null) {
        	positionProvider.stopUpdates();
        }

        if (clientController != null) {
        	clientController.stop();
        }
    }

    private PositionProvider.PositionListener positionListener = new PositionProvider.PositionListener() {

        @Override
        public void onPositionUpdate(Location location) {
            if (location != null) {
                StatusActivity.addMessage(getString(R.string.status_location_update));
                clientController.setNewLocation(Protocol.createLocationMessage(location));
            }
        }

    };

    OnSharedPreferenceChangeListener preferenceChangeListener = new OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            StatusActivity.addMessage(getString(R.string.status_preference_update));
            try {
                if (key.equals(TraccarActivity.KEY_ADDRESS)) {

                    address = sharedPreferences.getString(TraccarActivity.KEY_ADDRESS, null);
                    clientController.setNewServer(address, port);

                } else if (key.equals(TraccarActivity.KEY_PORT)) {

                    port = Integer.valueOf(sharedPreferences.getString(TraccarActivity.KEY_PORT, null));
                    clientController.setNewServer(address, port);

                } else if (key.equals(TraccarActivity.KEY_INTERVAL)) {

                    interval = Integer.valueOf(sharedPreferences.getString(TraccarActivity.KEY_INTERVAL, null));
                    positionProvider.stopUpdates();
                    positionProvider = new PositionProvider(TraccarService.this, provider, interval * 1000, positionListener);
                    positionProvider.startUpdates();

                } else if (key.equals(TraccarActivity.KEY_ID)) {

                    id = sharedPreferences.getString(TraccarActivity.KEY_ID, null);
                    clientController.setNewLogin(Protocol.createLoginMessage(id));

                } else if (key.equals(TraccarActivity.KEY_PROVIDER)) {

                    provider = sharedPreferences.getString(TraccarActivity.KEY_PROVIDER, null);
                    positionProvider.stopUpdates();
                    positionProvider = new PositionProvider(TraccarService.this, provider, interval * 1000, positionListener);
                    positionProvider.startUpdates();

                }
            } catch (Exception error) {
                Log.w(LOG_TAG, error);
            }
        }

    };

}
