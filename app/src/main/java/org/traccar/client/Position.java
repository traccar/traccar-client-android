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
import android.os.Build;

import java.util.Date;

public class Position {

    public Position() {
    }

    public Position(String deviceId, Location location, double battery, String setting, long interval, Long delta) {
        this.deviceId = deviceId;
        this.time = new Date(location.getTime());
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        if (location.hasAltitude()) {
            this.altitude = location.getAltitude();
        }
        if (location.hasSpeed()) {
            this.speed = location.getSpeed() * 1.943844F; // speed in knots
        }
        if (location.hasBearing()) {
            this.course = location.getBearing();
        }
        if (location.hasAccuracy()) {
            this.accuracy = location.getAccuracy();
        }
        this.battery = battery;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            this.mock = location.isFromMockProvider();
        }
        this.provider = location.getProvider();
        this.setting = setting;
        this.interval = interval;
        this.delta = delta;
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

    private Double altitude;

    public Double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    private Float speed;

    public Float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    private Float course;

    public Float getCourse() {
        return course;
    }

    public void setCourse(float course) {
        this.course = course;
    }

    private Float accuracy;

    public Float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
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

    private String provider;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    private String setting;

    public String getSetting() { return setting; }

    public void setSetting(String setting) { this.setting = setting; }

    private long interval;

    public long getInterval() { return interval; }

    public void setInterval(long interval) { this.interval = interval; }

    private Long delta;

    public Long getDelta() { return delta; }

    public void setDelta(long delta) { this.delta = delta; }

}
