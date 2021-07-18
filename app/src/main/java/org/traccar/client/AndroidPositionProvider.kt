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
package org.traccar.client

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper

class AndroidPositionProvider(context: Context, listener: PositionListener) : PositionProvider(context, listener), LocationListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val provider = getProvider(preferences.getString(MainFragment.KEY_ACCURACY, "medium"))

    @SuppressLint("MissingPermission")
    override fun startUpdates() {
        try {
            locationManager.requestLocationUpdates(
                    provider, if (distance > 0 || angle > 0) MINIMUM_INTERVAL else interval, 0f, this)
        } catch (e: RuntimeException) {
            listener.onPositionError(e)
        }
    }

    override fun stopUpdates() {
        locationManager.removeUpdates(this)
    }

    @Suppress("DEPRECATION", "MissingPermission")
    override fun requestSingleLocation() {
        try {
            val location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            if (location != null) {
                listener.onPositionUpdate(Position(deviceId, location, getBatteryLevel(context)))
            } else {
                locationManager.requestSingleUpdate(provider, object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        listener.onPositionUpdate(Position(deviceId, location, getBatteryLevel(context)))
                    }

                    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }, Looper.myLooper())
            }
        } catch (e: RuntimeException) {
            listener.onPositionError(e)
        }
    }

    override fun onLocationChanged(location: Location) {
        processLocation(location)
    }

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    private fun getProvider(accuracy: String?): String {
        return when (accuracy) {
            "high" -> LocationManager.GPS_PROVIDER
            "low"  -> LocationManager.PASSIVE_PROVIDER
            else   -> LocationManager.NETWORK_PROVIDER
        }
    }

}
