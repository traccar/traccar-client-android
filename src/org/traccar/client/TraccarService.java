/*
 * Copyright 2012 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.io.*;
import java.net.*;
import java.util.*;
import android.app.Service;
import android.content.*;
import android.location.*;
import android.os.*;
import android.telephony.TelephonyManager;
import android.widget.Toast;

/**
 * Background service
 */
public class TraccarService extends Service implements LocationListener {

	private int period;
	private String deviceId;
    private Connection connection;
    private LocationManager locationManager;

    /*
     * Location methods
     */

    @Override
    public void onLocationChanged(Location location) {
        connection.send(Protocol.createNmea(location));
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    /*
     * Status methods 
     */

    private String status;
    
    private void setStatus(String status) {
    	if (!status.equals(this.status)) {
	    	this.status = status;
    		Intent intent = new Intent(Traccar.MSG_SERVICE_STATUS);
    		intent.putExtra(Traccar.EXTRA_STATUS, status);
    		sendBroadcast(intent);
    	}
    }

    private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
    	public void onReceive(Context context, Intent intent) {
    		// Send config
    		intent = new Intent(Traccar.MSG_SERVICE_CONFIG);
    		intent.putExtra(Traccar.EXTRA_ADDRESS, connection.getAddress());
    		intent.putExtra(Traccar.EXTRA_PORT, connection.getPort());
    		intent.putExtra(Traccar.EXTRA_PERIOD, period);
    		sendBroadcast(intent);
    		
    		// Send service status
    		intent = new Intent(Traccar.MSG_SERVICE_STATUS);
    		intent.putExtra(Traccar.EXTRA_STATUS, status);
    		sendBroadcast(intent);
    		
    		// Send connection status
    		intent = new Intent(Traccar.MSG_CONNECTION_STATUS);
    		intent.putExtra(Traccar.EXTRA_STATUS, connection.getStatus());
    		sendBroadcast(intent);
    	}
    };

    /*
     * Service methods
     */
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	// Read device id
    	TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        deviceId = telephonyManager.getDeviceId();

    	// Create connection
    	connection = new Connection(
    			intent.getStringExtra(Traccar.EXTRA_ADDRESS),
    			intent.getIntExtra(Traccar.EXTRA_PORT, 0),
    			Protocol.createId(deviceId));
    	connection.setContext(this);
    	connection.connect();

    	// Request location updates
    	locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        period = intent.getIntExtra(Traccar.EXTRA_PERIOD, 0);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, period * 1000, 0, this);

        // Report status
        registerReceiver(updateReceiver, new IntentFilter(Traccar.MSG_UPDATE));
        setStatus(Traccar.STATUS_RUNNING);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
    	// Close connection
    	if (connection) {
    		connection.close();
    	}
    	
    	// Stop location updates
    	if (locationManager) {
    		locationManager.removeUpdates(this);
    	}

        // Report status
        unregisterReceiver(updateReceiver);
        setStatus(Traccar.STATUS_STOPPED);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
