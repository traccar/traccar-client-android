package org.traccar.client;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Toast;

public class AndroidPositionProvider extends PositionProvider implements LocationListener {

    private LocationManager locationManager;
    private String provider;

    public AndroidPositionProvider(Context context, PositionListener listener) {
        super(context, listener);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        provider = getProvider(preferences.getString(MainFragment.KEY_ACCURACY, "medium"));
    }

    @SuppressLint("MissingPermission")
    public void startUpdates() {
        try {
            locationManager.requestLocationUpdates(
                    provider, distance > 0 || angle > 0 ? MINIMUM_INTERVAL : interval, 0, this);
        } catch (RuntimeException e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public void stopUpdates() {
        locationManager.removeUpdates(this);
    }

    @SuppressLint("MissingPermission")
    public void requestSingleLocation() {
        try {
            Location location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            if (location != null) {
                listener.onPositionUpdate(new Position(deviceId, location, getBatteryLevel(context)));
            } else {
                locationManager.requestSingleUpdate(provider, new LocationListener() {
                    @Override
                    public void onLocationChanged(Location location) {
                        listener.onPositionUpdate(new Position(deviceId, location, getBatteryLevel(context)));
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
                }, Looper.myLooper());
            }
        } catch (RuntimeException e) {
            listener.onPositionError(e);
        }
    }

    private static String getProvider(String accuracy) {
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
        processLocation(location);
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

}
