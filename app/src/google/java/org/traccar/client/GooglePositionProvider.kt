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
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices

class GooglePositionProvider(context: Context, listener: PositionListener) : PositionProvider(context, listener) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @Suppress("DEPRECATION", "MissingPermission")
    override fun startUpdates() {
        val locationRequest = LocationRequest()
        locationRequest.priority = getPriority(preferences.getString(MainFragment.KEY_ACCURACY,"medium"))
        locationRequest.interval = if (distance > 0 || angle > 0) MINIMUM_INTERVAL else interval
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    override fun stopUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    @SuppressLint("MissingPermission")
    override fun requestSingleLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                listener.onPositionUpdate(Position(deviceId, location, getBatteryLevel(context)))
            }
        }
    }

    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                processLocation(location)
            }
        }
    }

    private fun getPriority(accuracy: String?): Int {
        return when (accuracy) {
            "high" -> LocationRequest.PRIORITY_HIGH_ACCURACY
            "low"  -> LocationRequest.PRIORITY_LOW_POWER
            else   -> LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
        }
    }
}
