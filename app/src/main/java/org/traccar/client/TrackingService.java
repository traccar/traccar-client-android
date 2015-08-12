/*
 * Copyright 2012 - 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;

public class TrackingService extends Service implements PositionProvider.PositionListener {

    private ClientController clientController;
    private PositionProvider positionProvider;
    
    private WakeLock wakeLock;

    @Override
    public void onCreate() {
        StatusActivity.addMessage(getString(R.string.status_service_create));

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        wakeLock.acquire();

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        String id = sharedPreferences.getString(MainActivity.KEY_ID, null);

        clientController = new ClientController(this,
                sharedPreferences.getString(MainActivity.KEY_ADDRESS, null),
                Integer.parseInt(sharedPreferences.getString(MainActivity.KEY_PORT, null)),
                ProtocolFormatter.createLoginMessage(id));
        clientController.start();

        positionProvider = new PositionProvider(this, this, id,
                sharedPreferences.getString(MainActivity.KEY_PROVIDER, null),
                Integer.parseInt(sharedPreferences.getString(MainActivity.KEY_INTERVAL, null)) * 1000);
        positionProvider.startUpdates();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        StatusActivity.addMessage(getString(R.string.status_service_destroy));

        if (positionProvider != null) {
        	positionProvider.stopUpdates();
        }

        if (clientController != null) {
        	clientController.stop();
        }

        wakeLock.release();
    }

    @Override
    public void onPositionUpdate(Position position) {
        if (position != null) {
            StatusActivity.addMessage(getString(R.string.status_location_update));
            clientController.setNewLocation(ProtocolFormatter.createLocationMessage(position.location, 0));
        }
    }

}
