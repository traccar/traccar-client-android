/*
 * Copyright 2012 - 2017 Anton Tananaev (anton.tananaev@gmail.com)
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

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.TwoStatePreference;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.URLUtil;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

@SuppressWarnings("deprecation")
public class MainActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String KEY_DEVICE = "id";
    public static final String KEY_URL = "url";
    public static final String KEY_INTERVAL = "interval";
    public static final String KEY_DISTANCE = "distance";
    public static final String KEY_ANGLE = "angle";
    public static final String KEY_PROVIDER = "provider";
    public static final String KEY_STATUS = "status";

    private static final int PERMISSIONS_REQUEST_LOCATION = 2;

    private SharedPreferences sharedPreferences;

    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildConfig.HIDDEN_APP) {
            removeLauncherIcon();
        }

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        migrateLegacyPreferences(sharedPreferences);
        addPreferencesFromResource(R.xml.preferences);
        initPreferences();

        findPreference(KEY_DEVICE).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return newValue != null && !newValue.equals("");
            }
        });
        findPreference(KEY_URL).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                return (newValue != null) && validateServerURL(newValue.toString());
            }
        });

        findPreference(KEY_INTERVAL).setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null) {
                    try {
                        int value = Integer.parseInt((String) newValue);
                        return value > 0;
                    } catch (NumberFormatException e) {
                        Log.w(TAG, e);
                    }
                }
                return false;
            }
        });

        Preference.OnPreferenceChangeListener numberValidationListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null) {
                    try {
                        int value = Integer.parseInt((String) newValue);
                        return value >= 0;
                    } catch (NumberFormatException e) {
                        Log.w(TAG, e);
                    }
                }
                return false;
            }
        };
        findPreference(KEY_DISTANCE).setOnPreferenceChangeListener(numberValidationListener);
        findPreference(KEY_ANGLE).setOnPreferenceChangeListener(numberValidationListener);

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, AutostartReceiver.class), 0);

        if (sharedPreferences.getBoolean(KEY_STATUS, false)) {
            startTrackingService(true, false);
        }
    }

    private void removeLauncherIcon() {
        String className = MainActivity.class.getCanonicalName().replace(".MainActivity", ".Launcher");
        ComponentName componentName = new ComponentName(getPackageName(), className);
        PackageManager packageManager = getPackageManager();
        if (packageManager.getComponentEnabledSetting(componentName) != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
            packageManager.setComponentEnabledSetting(
                    componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setMessage(getString(R.string.hidden_alert));
            builder.setPositiveButton(android.R.string.ok, null);
            builder.show();
        }
    }

    private void addShortcuts(String action, int name) {
        Intent shortcutIntent = new Intent(Intent.ACTION_MAIN);
        shortcutIntent.setComponent(new ComponentName(getPackageName(), ShortcutActivity.class.getCanonicalName()));
        shortcutIntent.putExtra(ShortcutActivity.EXTRA_ACTION, action);
        Intent installShortCutIntent = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
        installShortCutIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
        installShortCutIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, getString(name));
        installShortCutIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(this, R.mipmap.ic_launcher));

        sendBroadcast(installShortCutIntent);
    }

    private boolean hasPermission(String permission) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            return true;
        }
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onResume() {
        super.onResume();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    private void setPreferencesEnabled(boolean enabled) {
        findPreference(KEY_DEVICE).setEnabled(enabled);
        findPreference(KEY_URL).setEnabled(enabled);
        findPreference(KEY_INTERVAL).setEnabled(enabled);
        findPreference(KEY_DISTANCE).setEnabled(enabled);
        findPreference(KEY_ANGLE).setEnabled(enabled);
        findPreference(KEY_PROVIDER).setEnabled(enabled);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_STATUS)) {
            if (sharedPreferences.getBoolean(KEY_STATUS, false)) {
                startTrackingService(true, false);
            } else {
                stopTrackingService();
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
        } else if (item.getItemId() == R.id.shortcuts) {
            addShortcuts(ShortcutActivity.EXTRA_ACTION_START, R.string.shortcut_start);
            addShortcuts(ShortcutActivity.EXTRA_ACTION_STOP, R.string.shortcut_stop);
            addShortcuts(ShortcutActivity.EXTRA_ACTION_SOS, R.string.shortcut_sos);
            return true;
        } else if (item.getItemId() == R.id.about) {
            startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initPreferences() {
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        if (!sharedPreferences.contains(KEY_DEVICE)) {
            String id = String.valueOf(new Random().nextInt(900000) + 100000);
            sharedPreferences.edit().putString(KEY_DEVICE, id).commit();
            ((EditTextPreference) findPreference(KEY_DEVICE)).setText(id);
        }
        findPreference(KEY_DEVICE).setSummary(sharedPreferences.getString(KEY_DEVICE, null));
    }

    private void startTrackingService(boolean checkPermission, boolean permission) {
        if (checkPermission) {
            Set<String> missingPermissions = new HashSet<>();
            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                missingPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (!hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                missingPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
            if (missingPermissions.isEmpty()) {
                permission = true;
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(missingPermissions.toArray(new String[missingPermissions.size()]),
                            PERMISSIONS_REQUEST_LOCATION);
                }
                return;
            }
        }

        if (permission) {
            setPreferencesEnabled(false);
            startService(new Intent(this, TrackingService.class));
            alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    15000, 15000, alarmIntent);
        } else {
            sharedPreferences.edit().putBoolean(KEY_STATUS, false).commit();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                TwoStatePreference preference = (TwoStatePreference) findPreference(KEY_STATUS);
                preference.setChecked(false);
            } else {
                CheckBoxPreference preference = (CheckBoxPreference) findPreference(KEY_STATUS);
                preference.setChecked(false);
            }
        }
    }

    private void stopTrackingService() {
        alarmManager.cancel(alarmIntent);
        stopService(new Intent(this, TrackingService.class));
        setPreferencesEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST_LOCATION) {
            boolean granted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            startTrackingService(false, granted);
        }
    }

    private boolean validateServerURL(String userUrl) {
        EditTextPreference preference = (EditTextPreference) findPreference(KEY_URL);

        if (userUrl == null || userUrl.trim().length() == 0) {
            preference.setText(getString(R.string.settings_url_default_value));
            preference.setSummary(R.string.settings_url_summary);
            findPreference(KEY_STATUS).setEnabled(true);
            return false;
        }
        int port = Uri.parse(userUrl).getPort();
        if (URLUtil.isValidUrl(userUrl) && (port >= -1 && port <= 65535)
                && (URLUtil.isHttpUrl(userUrl) || URLUtil.isHttpsUrl(userUrl))) {
            preference.setSummary(R.string.settings_url_summary);
            findPreference(KEY_STATUS).setEnabled(true);
        } else {
            preference.setSummary(R.string.error_invalid_url_summary);
            findPreference(KEY_STATUS).setEnabled(false);
            Toast.makeText(MainActivity.this, R.string.error_msg_invalid_url, Toast.LENGTH_LONG).show();
        }
        return true;
    }

    private void migrateLegacyPreferences(SharedPreferences preferences) {
        String port = preferences.getString("port", null);
        if (port != null) {
            Log.d(TAG, "migrateLegacyPreferences: migrating to URL preference");

            String host = preferences.getString("address", getString(R.string.settings_url_default_value));
            String scheme = preferences.getBoolean("secure", false) ? "https" : "http";

            Uri.Builder builder = new Uri.Builder();
            builder.scheme(scheme).encodedAuthority(host + ":" + port).build();
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_URL, builder.toString());

            editor.remove("port");
            editor.remove("address");
            editor.remove("secure");
            editor.commit();
        }
    }
}
