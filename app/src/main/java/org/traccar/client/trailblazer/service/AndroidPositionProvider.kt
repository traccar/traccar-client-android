/*
 * Copyright 2019 - 2021 Anton Tananaev (anton@traccar.org)
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
package org.traccar.client.trailblazer.service

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import org.traccar.client.Position
import org.traccar.client.trailblazer.ui.Trailblazer.Companion.TAG

/**
 * The AndroidPositionProvider class is responsible for obtaining location updates and handling location-related operations,
 * integrating Android's LocationManager and LocationListener.
 */
class AndroidPositionProvider(context: Context, listener: PositionListener) : PositionProvider(context, listener), LocationListener {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val provider =
        getProvider("medium") // getProvider(preferences.getString(MainFragment.KEY_ACCURACY, "high"))

    @SuppressLint("MissingPermission")
    override fun startUpdates() {
        try {
            locationManager.requestLocationUpdates(
                    provider, if (distance > 0 || angle > 0) MINIMUM_INTERVAL else interval, 0f, this)
            Log.i(TAG, "Started location updates successfully")
        } catch (e: RuntimeException) {
            Log.e(TAG, "Failed to start location updates: ${e.message}", e)
            listener.onPositionError(e)
        }
    }

    override fun stopUpdates() {
        Log.d(TAG, "stopUpdates() called")

        try {
            locationManager.removeUpdates(this)
            Log.i(TAG, "Location updates stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop location updates: ${e.message}", e)
        }
    }

    @Suppress("DEPRECATION", "MissingPermission")
    override fun requestSingleLocation() {
        Log.d(TAG, "requestSingleLocation() called")

        try {
            val location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            if (location != null) {
                Log.i(TAG, "Last known location found: $location")
                listener.onPositionUpdate(Position(deviceId, location, getBatteryStatus(context)))
            } else {
                Log.i(TAG, "Last known location not available, requesting single update...")
                locationManager.requestSingleUpdate(provider, object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        Log.i(TAG, "Received single location update: $location")
                        listener.onPositionUpdate(
                            Position(
                                deviceId,
                                location,
                                getBatteryStatus(context)
                            )
                        )
                    }

                    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }, Looper.myLooper())
            }
        } catch (e: RuntimeException) {
            Log.e(TAG, "Error in requestSingleLocation: ${e.message}", e)
            listener.onPositionError(e)
        }
    }

    override fun onLocationChanged(location: Location) {
        Log.d(TAG, "onLocationChanged() called with location: $location")
        processLocation(location)
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {
        Log.d(TAG, "onStatusChanged() called with provider: $provider, status: $status")
    }

    override fun onProviderEnabled(provider: String) {
        Log.d(TAG, "onProviderEnabled() called with provider: $provider")
    }

    override fun onProviderDisabled(provider: String) {
        Log.d(TAG, "onProviderDisabled() called with provider: $provider")
    }

    private fun getProvider(accuracy: String?): String {
        Log.d(TAG, "getProvider() called with accuracy: $accuracy")

        return when (accuracy) {
            "high" -> {
                Log.i(TAG, "Using GPS provider for high accuracy")
                LocationManager.GPS_PROVIDER
            }

            "low" -> {
                Log.i(TAG, "Using PASSIVE provider for low accuracy")
                LocationManager.PASSIVE_PROVIDER
            }

            else -> {
                Log.i(TAG, "Using NETWORK provider for default accuracy")
                LocationManager.NETWORK_PROVIDER
            }
        }
    }
}