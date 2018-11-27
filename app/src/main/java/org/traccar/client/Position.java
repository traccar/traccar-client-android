/*
 * Copyright 2015 - 2018 Anton Tananaev (anton.tananaev@gmail.com)
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

import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.util.Base64;

import java.util.Date;

import static android.util.Base64.NO_WRAP;

public class Position {

    public Position() {
    }

    public Position(final String deviceId,
                    final Location location,
                    final double battery,
                    final String Authorization) {
        this.deviceId = deviceId;
        time = new Date(location.getTime());
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        altitude = location.getAltitude();
        speed = location.getSpeed() * 1.943844; // speed in knots
        course = location.getBearing();
        if (location.getProvider() != null && !location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            accuracy = location.getAccuracy();
        }
        this.battery = battery;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            this.mock = location.isFromMockProvider();
        }

        this.Auth = Base64.encodeToString(Authorization.getBytes(), NO_WRAP); //NO_WRAP is Android specific option.
    }

    private long id;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    private String deviceId;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    private Date time;

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    private double latitude;

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    private double longitude;

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    private double altitude;

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    private double speed;

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    private double course;

    public double getCourse() {
        return course;
    }

    public void setCourse(double course) {
        this.course = course;
    }

    private double accuracy;

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    private double battery;

    public double getBattery() {
        return battery;
    }

    public void setBattery(double battery) {
        this.battery = battery;
    }

    private boolean mock;

    public boolean getMock() {
        return mock;
    }

    public void setMock(boolean mock) {
        this.mock = mock;
    }

    private String Auth;

    public String getAuth() {
        return Auth;
    }

    public void setAuth(String auth) {
        Auth = auth;
    }
}
