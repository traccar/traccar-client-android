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
package org.traccar.client.provider;

import android.content.Context;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;

import org.traccar.client.R;
import org.traccar.client.activity.StatusActivity;
import org.traccar.client.util.Utilities;

public class PositionProvider {

    public static final String PROVIDER_MIXED = "mixed";
    public static final long PERIOD_DELTA = 10 * 1000;
    public static final long RETRY_PERIOD = 60 * 1000;
    private final Handler handler;
    private final LocationManager locationManager;
    private final long period;
    private final PositionListener listener;
    private final Context context;
    private final InternalLocationListener fineLocationListener = new InternalLocationListener();
    private final InternalLocationListener coarseLocationListener = new InternalLocationListener();
    private boolean useFine;
    private boolean useCoarse;

    private String latestHdop;
    private String latestPdop;
    private String latestVdop;
    private String geoIdHeight;
    private String ageOfDgpsData;
    private String dgpsId;


    private final Runnable updateTask = new Runnable() {

        private long lastTime;

        private boolean tryProvider(String provider) {
            Location location = locationManager.getLastKnownLocation(provider);

            if (location != null && location.getTime() != lastTime) {
                lastTime = location.getTime();

                Bundle b = new Bundle();
                b.putString("HDOP", getLatestHdop());
                b.putString("PDOP", getLatestPdop());
                b.putString("VDOP", getLatestVdop());
                b.putString("GEOIDHEIGHT", getGeoIdHeight());
                b.putString("AGEOFDGPSDATA", getAgeOfDgpsData());
                b.putString("DGPSID", getDgpsId());

                location.setExtras(b);

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
            } catch (Exception e) {
                StatusActivity.addMessage(context.getString(R.string.status_provider_missing));
            }
        }
        if (useCoarse) {
            try {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, period, 0, coarseLocationListener);
            } catch (Exception e) {
                StatusActivity.addMessage(context.getString(R.string.status_provider_missing));
            }
        }
        handler.postDelayed(updateTask, period);
    }

    public void stopUpdates() {
        handler.removeCallbacks(updateTask);
        locationManager.removeUpdates(fineLocationListener);
        locationManager.removeUpdates(coarseLocationListener);
    }

    public interface PositionListener {
        public void onPositionUpdate(Location location);
    }

    private class InternalLocationListener implements LocationListener,GpsStatus.NmeaListener {

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

        @Override
        public void onNmeaReceived(long timestamp, String nmeaSentence) {
            if(Utilities.IsNullOrEmpty(nmeaSentence)){
                return;
            }

            String[] nmeaParts = nmeaSentence.split(",");

            if (nmeaParts[0].equalsIgnoreCase("$GPGSA")) {

                if (nmeaParts.length > 15 && !Utilities.IsNullOrEmpty(nmeaParts[15])) {
                    setLatestPdop(nmeaParts[15]);
                }

                if (nmeaParts.length > 16 &&!Utilities.IsNullOrEmpty(nmeaParts[16])) {
                    setLatestHdop(nmeaParts[16]);
                }

                if (nmeaParts.length > 17 &&!Utilities.IsNullOrEmpty(nmeaParts[17]) && !nmeaParts[17].startsWith("*")) {
                    setLatestVdop(nmeaParts[17].split("\\*")[0]);
                }
            }


            if (nmeaParts[0].equalsIgnoreCase("$GPGGA")) {
                if (nmeaParts.length > 8 &&!Utilities.IsNullOrEmpty(nmeaParts[8])) {
                    setLatestHdop(nmeaParts[16]);
                }

                if (nmeaParts.length > 11 &&!Utilities.IsNullOrEmpty(nmeaParts[11])) {
                    setGeoIdHeight(nmeaParts[11]);
                }

                if (nmeaParts.length > 13 &&!Utilities.IsNullOrEmpty(nmeaParts[13])) {
                    setAgeOfDgpsData(nmeaParts[13]);
                }

                if (nmeaParts.length > 14 &&!Utilities.IsNullOrEmpty(nmeaParts[14]) && !nmeaParts[14].startsWith("*")) {
                    setDgpsId(nmeaParts[14].split("\\*")[0]);
                }
            }
        }
    }



    public String getLatestPdop() {
        return latestPdop;
    }

    public void setLatestPdop(String latestPdop) {
        this.latestPdop = latestPdop;
    }

    public String getLatestHdop() {
        return latestHdop;
    }

    public void setLatestHdop(String latestHdop) {
        this.latestHdop = latestHdop;
    }

    public String getLatestVdop() {
        return latestVdop;
    }

    public void setLatestVdop(String latestVdop) {
        this.latestVdop = latestVdop;
    }

    public String getGeoIdHeight() {
        return geoIdHeight;
    }

    public void setGeoIdHeight(String geoIdHeight) {
        this.geoIdHeight = geoIdHeight;
    }

    public String getAgeOfDgpsData() {
        return ageOfDgpsData;
    }

    public void setAgeOfDgpsData(String ageOfDgpsData) {
        this.ageOfDgpsData = ageOfDgpsData;
    }

    public String getDgpsId() {
        return dgpsId;
    }

    public void setDgpsId(String dgpsId) {
        this.dgpsId = dgpsId;
    }
}
