/*
 * Copyright 2013 - 2014 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.util.Date;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

public class PositionProvider {

    public static final String PROVIDER_MIXED = "mixed";
    public static final long PERIOD_DELTA = 10 * 1000;
    public static final long RETRY_PERIOD = 60 * 1000;

    public interface PositionListener {
        public void onPositionUpdate(Location location);
    }
    
    private final Handler handler;
    private final LocationManager locationManager;
    private final long period;
    private final PositionListener listener;
    private final Context context;

    private boolean useFine;
    private boolean useCoarse;

    public PositionProvider(Context context, String type, long period, PositionListener listener) {
        handler = new Handler(context.getMainLooper());
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.period = period;
        this.listener = listener;
        this.context = context;

        // Determine providers
        if (type.equals(PROVIDER_MIXED)) {
            useFine = true;
            useCoarse = true;
        } else if (type.equals(LocationManager.GPS_PROVIDER)) {
            useFine = true;
        } else if (type.equals(LocationManager.NETWORK_PROVIDER)) {
            useCoarse = true;
        }
    }

    public void startUpdates() {
        if (useFine) {
            try {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, period, 0, fineLocationListener);
            } catch (Exception ex) {
                
            }
        }
        if (useCoarse) {
            try {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, period, 0, coarseLocationListener);
            } catch (Exception ex) {

            }
        }
        handler.postDelayed(updateTask, period);
    }

    public void stopUpdates() {
        handler.removeCallbacks(updateTask);
        locationManager.removeUpdates(fineLocationListener);
        locationManager.removeUpdates(coarseLocationListener);
    }

    private final Runnable updateTask = new Runnable() {

        private boolean tryProvider(String provider) {
            Location location = locationManager.getLastKnownLocation(provider);

            /*if (location != null) {
                Toast.makeText(context, "phone: " + new Date() + "\ngps: " + new Date(location.getTime()), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(context, "no location", Toast.LENGTH_LONG).show();
            }*/
            
            if (location != null && new Date().getTime() - location.getTime() <= period + PERIOD_DELTA) {
                listener.onPositionUpdate(location);
                return true;
            } else {
                return false;
            }
        }

        @Override
        public void run() {
            if (useFine && tryProvider(LocationManager.GPS_PROVIDER)) {
            } else if (useCoarse && tryProvider(LocationManager.NETWORK_PROVIDER)) {
            } else {
                listener.onPositionUpdate(null);
            }
            handler.postDelayed(this, period);
        }

    };

    private final InternalLocationListener fineLocationListener = new InternalLocationListener();
    private final InternalLocationListener coarseLocationListener = new InternalLocationListener();

    private class InternalLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(final String provider, int status, Bundle extras) {
            if (status == LocationProvider.TEMPORARILY_UNAVAILABLE || status == LocationProvider.OUT_OF_SERVICE) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        locationManager.removeUpdates(InternalLocationListener.this);
                        locationManager.requestLocationUpdates(provider, period, 0, InternalLocationListener.this);
                    }
                }, RETRY_PERIOD);
            }
        }

    }

}
