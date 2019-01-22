/*
 * Copyright 2015 Anton Tananaev (anton@traccar.org)
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

public class NetworkManager extends BroadcastReceiver {

    private static final String TAG = NetworkManager.class.getSimpleName();
    private String broadcast;
    private SharedPreferences preferences;

    private Context context;
    private NetworkHandler handler;
    private ConnectivityManager connectivityManager;

    public NetworkManager(Context context, NetworkHandler handler) {
        this.context = context;
        this.handler = handler;
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public interface NetworkHandler {
        void onNetworkUpdate(boolean isOnline);
    }

    public boolean isOnline() {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    public void start() {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        broadcast = preferences.getString(MainFragment.KEY_BROADCAST, " ");
        IntentFilter filter = new IntentFilter();
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        if(broadcast.trim().length() > 0) {
            filter.addAction(broadcast);
        }
        context.registerReceiver(this, filter);
    }

    public void stop() {
        context.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION) && handler != null) {
            boolean isOnline = isOnline();
            Log.i(TAG, "network " + (isOnline ? "on" : "off"));
            handler.onNetworkUpdate(isOnline);
        } else if (intent.getAction().equals(broadcast)) {
            Log.i(TAG, "EmergencyAlarmButton triggered!");
            Intent intentOut = new Intent(Intent.ACTION_DEFAULT, null, this.context, ShortcutActivity.class);
            intentOut.putExtra(ShortcutActivity.EXTRA_ACTION, ShortcutActivity.ACTION_SOS);
            this.context.startActivity(intentOut);
        }
    }

}
