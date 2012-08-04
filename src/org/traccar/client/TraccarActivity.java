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

import java.util.*;
import android.app.*;
import android.content.*;
import android.os.*;
import 	android.text.*;
import android.text.method.*;
import android.text.util.*;
import android.view.*;
import android.widget.*;
import android.telephony.TelephonyManager;

/**
 * Global parameters
 */
class Traccar {
    public static final String EXTRA_ADDRESS = "address";
    public static final String EXTRA_PORT = "port";
    public static final String EXTRA_PERIOD = "period";
    public static final String EXTRA_STATUS = "status";

    public static final String DEFAULT_ADDRESS = "193.193.165.166";
    public static final String DEFAULT_PORT = "20100";
    public static final String DEFAULT_PERIOD = "60";

    public static final String TRACCAR_SETTINGS = "traccar_settings";

    public static final long RECONNECT_DELAY = 10 * 1000;
    public static final int SOCKET_TIMEOUT = 2000;

    public static final String MSG_UPDATE = "org.traccar.client.MSG_UPDATE";
    public static final String MSG_SERVICE_CONFIG = "org.traccar.client.MSG_SERVICE_CONFIG";
    public static final String MSG_SERVICE_STATUS = "org.traccar.client.SERVICE_STATUS";
    public static final String MSG_CONNECTION_STATUS = "org.traccar.client.CONNECTION_STATUS";

    public static final String STATUS_STOPPED = "Stopped";
    public static final String STATUS_RUNNING = "Running";

    public static final String STATUS_CONNECTED = "Connected";
    public static final String STATUS_DISCONNECTED = "Disconnected";
}

/**
 * Main user interface
 */
public class TraccarActivity extends Activity {

	private EditText editAddress;
    private EditText editPort;
    private EditText editPeriod;
    private EditText editDevice;
    private EditText editService;
    private EditText editConnection;
    private ToggleButton button;

    public boolean isServiceRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> services = activityManager.getRunningServices(Integer.MAX_VALUE);
        for (ActivityManager.RunningServiceInfo runningServiceInfo : services) {
            if (runningServiceInfo.service.getClassName().equals(TraccarService.class.getName())){
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // DO SOMETHING WITH IT
        try {
        	StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        	StrictMode.setThreadPolicy(policy);
        } catch(Throwable e) {
        }

        // Bind elements
        editAddress = (EditText) findViewById(R.id.edit_address);
        editPort = (EditText) findViewById(R.id.edit_port);
        editPeriod = (EditText) findViewById(R.id.edit_period);
        editDevice = (EditText) findViewById(R.id.edit_device);
        editService = (EditText) findViewById(R.id.edit_service);
        editConnection = (EditText) findViewById(R.id.edit_connection);
        button = (ToggleButton) findViewById(R.id.button);
        
        // Set device identifier
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        editDevice.setText(telephonyManager.getDeviceId());

        editService.setText(Traccar.STATUS_STOPPED);
        editConnection.setText(Traccar.STATUS_DISCONNECTED);
        button.setChecked(isServiceRunning());
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	//super.onCreateOptionsMenu(menu);
    	MenuInflater inflater = getMenuInflater();
    	inflater.inflate(R.menu.main, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) { 	
    	if(item.getItemId() == R.id.about){
    		showAboutDialog();
    		return true;
    	}
    	return false;
    }
    
    public void showAboutDialog() {
        TextView message = new TextView(this);
    	SpannableString str = new SpannableString(getString(R.string.about_text));
    	Linkify.addLinks(str, Linkify.WEB_URLS);
    	message.setText(str);
    	message.setMovementMethod(LinkMovementMethod.getInstance());
    	int padding = (int) (10 * getResources().getDisplayMetrics().scaledDensity);
    	message.setPadding(padding, padding, padding, padding);

    	AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(R.string.about_option);
        dialog.setView(message);
        dialog.setPositiveButton(R.string.about_close, null);
        dialog.show();
    }

    private BroadcastReceiver serviceConfigReceiver = new BroadcastReceiver() {
    	public void onReceive(Context context, Intent intent) {
    		editAddress.setText(intent.getStringExtra(Traccar.EXTRA_ADDRESS));
    		editPort.setText(String.valueOf(intent.getIntExtra(Traccar.EXTRA_PORT, 0)));
    		editPeriod.setText(String.valueOf(intent.getIntExtra(Traccar.EXTRA_PERIOD, 0)));
    	}
    };

    private BroadcastReceiver serviceStatusReceiver = new BroadcastReceiver() {
    	public void onReceive(Context context, Intent intent) {
    		editService.setText(intent.getStringExtra(Traccar.EXTRA_STATUS));
    	}
    };

    private BroadcastReceiver connectionStatusReceiver = new BroadcastReceiver() {
    	public void onReceive(Context context, Intent intent) {
    		editConnection.setText(intent.getStringExtra(Traccar.EXTRA_STATUS));
    	}
    };
    
    @Override
    protected void onStart() {
        super.onStart();
        loadSettings();

        // Register receivers
        registerReceiver(serviceConfigReceiver, new IntentFilter(Traccar.MSG_SERVICE_CONFIG));
        registerReceiver(serviceStatusReceiver, new IntentFilter(Traccar.MSG_SERVICE_STATUS));
        registerReceiver(connectionStatusReceiver, new IntentFilter(Traccar.MSG_CONNECTION_STATUS));

        // Request update
        sendBroadcast(new Intent(Traccar.MSG_UPDATE));
    }
    
    @Override
    protected void onStop() {
    	super.onStop();
    	saveSettings();

        // Unregister receivers
    	unregisterReceiver(serviceConfigReceiver);
    	unregisterReceiver(serviceStatusReceiver);
    	unregisterReceiver(connectionStatusReceiver);
    }

    public void onButtonClick(View v) {
        if (button.isChecked()) {
            try {
            	Intent serviceIntent = new Intent(this, TraccarService.class);
                serviceIntent.putExtra(Traccar.EXTRA_ADDRESS, editAddress.getText().toString());
                serviceIntent.putExtra(Traccar.EXTRA_PORT, Integer.parseInt(editPort.getText().toString()));
                serviceIntent.putExtra(Traccar.EXTRA_PERIOD, Integer.parseInt(editPeriod.getText().toString()));
                startService(serviceIntent);
            } catch (NumberFormatException e) {
                Toast.makeText(this, R.string.error_input, Toast.LENGTH_SHORT).show();
                button.setChecked(false);
            }
        } else {
            stopService(new Intent(this, TraccarService.class));
        }
    }

    public void saveSettings() {
    	SharedPreferences settings = getSharedPreferences(Traccar.TRACCAR_SETTINGS, 0);
    	SharedPreferences.Editor editor = settings.edit();
        editor.putString(Traccar.EXTRA_ADDRESS, editAddress.getText().toString());
        editor.putString(Traccar.EXTRA_PORT, editPort.getText().toString());
        editor.putString(Traccar.EXTRA_PERIOD, editPeriod.getText().toString());
        editor.commit();
    }
    
    public void loadSettings() {
    	SharedPreferences settings = getSharedPreferences(Traccar.TRACCAR_SETTINGS, 0);
    	editAddress.setText(settings.getString(Traccar.EXTRA_ADDRESS, Traccar.DEFAULT_ADDRESS));
    	editPort.setText(settings.getString(Traccar.EXTRA_PORT, Traccar.DEFAULT_PORT));
    	editPeriod.setText(settings.getString(Traccar.EXTRA_PERIOD, Traccar.DEFAULT_PERIOD));
    }
}
