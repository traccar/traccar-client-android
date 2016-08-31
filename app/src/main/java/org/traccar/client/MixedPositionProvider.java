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
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

@SuppressWarnings("MissingPermission")
public class MixedPositionProvider extends PositionProvider implements LocationListener, GpsStatus.Listener {

    private static int FIX_TIMEOUT = 30 * 1000;

    private LocationListener backupListener;
    private long lastFixTime;

    public MixedPositionProvider(Context context, PositionListener listener) {
        super(context, listener);
    }

    public void startUpdates() {
        lastFixTime = System.currentTimeMillis();
        locationManager.addGpsStatusListener(this);
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, period, 0, this);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, e);
        }
    }

    public void stopUpdates() {
        locationManager.removeUpdates(this);
        locationManager.removeGpsStatusListener(this);
        stopBackupProvider();
    }

    private void startBackupProvider() {
        Log.i(TAG, "backup provider start");
        if (backupListener == null) {

            backupListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    Log.i(TAG, "backup provider location");
                    updateLocation(location);
                }

                @Override
                public void onStatusChanged(String s, int i, Bundle bundle) {
                }

                @Override
                public void onProviderEnabled(String s) {
                }

                @Override
                public void onProviderDisabled(String s) {
                }
            };

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, period, 0, backupListener);
        }
    }

    private void stopBackupProvider() {
        Log.i(TAG, "backup provider stop");
        if (backupListener != null) {
            locationManager.removeUpdates(backupListener);
            backupListener = null;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(TAG, "provider location");
        stopBackupProvider();
        lastFixTime = System.currentTimeMillis();
        updateLocation(location);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.i(TAG, "provider enabled");
        stopBackupProvider();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.i(TAG, "provider disabled");
        startBackupProvider();
    }

    @Override
    public void onGpsStatusChanged(int event) {
        if (backupListener == null && System.currentTimeMillis() - (lastFixTime + period) > FIX_TIMEOUT) {
            startBackupProvider();
        }
    }

}
