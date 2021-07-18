/*
 * Copyright 2019 Anton Tananaev (anton@traccar.org)
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
import android.content.Context;
import android.location.Location;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class GooglePositionProvider extends PositionProvider {

    private FusedLocationProviderClient fusedLocationClient;

    public GooglePositionProvider(Context context, PositionListener listener) {
        super(context, listener);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    @SuppressLint("MissingPermission")
    public void startUpdates() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setPriority(getPriority(preferences.getString(MainFragment.KEY_ACCURACY, "medium")));
        locationRequest.setInterval(distance > 0 || angle > 0 ? MINIMUM_INTERVAL : interval);
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    public void stopUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @SuppressLint("MissingPermission")
    public void requestSingleLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    listener.onPositionUpdate(new Position(deviceId, location, getBatteryLevel(context)));
                }
            }
        });
    }

    private static int getPriority(String accuracy) {
        switch (accuracy) {
            case "high":
                return LocationRequest.PRIORITY_HIGH_ACCURACY;
            case "low":
                return LocationRequest.PRIORITY_LOW_POWER;
            default:
                return LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
        }
    }

    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (locationResult != null) {
                for (Location location : locationResult.getLocations()) {
                    processLocation(location);
                }
            }
        }
    };

}
