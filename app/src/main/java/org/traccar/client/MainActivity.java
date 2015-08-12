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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.telephony.TelephonyManager;
import android.view.Menu;
import android.view.MenuItem;

@SuppressWarnings("deprecation")
public class MainActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    public static final String LOG_TAG = "traccar";

    public static final String KEY_DEVICE = "id";
    public static final String KEY_ADDRESS = "address";
    public static final String KEY_PORT = "port";
    public static final String KEY_INTERVAL = "interval";
    public static final String KEY_PROVIDER = "provider";
    public static final String KEY_STATUS = "status";

    private boolean firstLaunch; // DELME

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firstLaunch = !PreferenceManager.getDefaultSharedPreferences(this).contains(KEY_PORT);
        addPreferencesFromResource(R.xml.preferences);
        initPreferences();
        if (getPreferenceScreen().getSharedPreferences().getBoolean(KEY_STATUS, false)) {
            setPreferencesEnabled(false);
            startService(new Intent(this, TrackingService.class));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkAndShowPortDialog(); // DELME
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    private void setPreferencesEnabled(boolean enabled) {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        preferenceScreen.findPreference(KEY_DEVICE).setEnabled(enabled);
        preferenceScreen.findPreference(KEY_ADDRESS).setEnabled(enabled);
        preferenceScreen.findPreference(KEY_PORT).setEnabled(enabled);
        preferenceScreen.findPreference(KEY_INTERVAL).setEnabled(enabled);
        preferenceScreen.findPreference(KEY_PROVIDER).setEnabled(enabled);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_STATUS)) {
            if (sharedPreferences.getBoolean(KEY_STATUS, false)) {
                setPreferencesEnabled(false);
                startService(new Intent(this, TrackingService.class));
            } else {
                stopService(new Intent(this, TrackingService.class));
                setPreferencesEnabled(true);
            }
        } else if (key.equals(KEY_DEVICE)) {
            findPreference(KEY_DEVICE).setSummary(sharedPreferences.getString(KEY_DEVICE, null));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.status) {
            startActivity(new Intent(this, StatusActivity.class));
            return true;
        } else if (item.getItemId() == R.id.about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String id = telephonyManager.getDeviceId();

        SharedPreferences sharedPreferences = getPreferenceScreen().getSharedPreferences();

        if (!sharedPreferences.contains(KEY_DEVICE)) {
            sharedPreferences.edit().putString(KEY_DEVICE, id).commit();
        }
        findPreference(KEY_DEVICE).setSummary(sharedPreferences.getString(KEY_DEVICE, id));
    }

    // TEMPORARY PORT CHANGE DIALOG (DELME)

    private void checkAndShowPortDialog() {

        String KEY_SHOWN_PORT_DIALOG = "portDialogHasBeenShown";

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!firstLaunch) {
            if (!preferences.contains(KEY_SHOWN_PORT_DIALOG)) {
                showDialog(0);
            }
        }

        preferences.edit().putBoolean(KEY_SHOWN_PORT_DIALOG, true).commit();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMessage("Communication protocol used by the app has changed. Now it uses port 5055 on Traccar server. Do you want to change port to 5055?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putString(KEY_PORT, "5055").commit();
                    }
                })
                .setNegativeButton("No", null);

        return builder.create();
    }

}
