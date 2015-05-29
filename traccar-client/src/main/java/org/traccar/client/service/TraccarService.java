package org.traccar.client.service;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.apache.http.Header;
import org.traccar.client.R;
import org.traccar.client.activity.StatusActivity;
import org.traccar.client.activity.TraccarActivity;
import org.traccar.client.provider.PositionProvider;
import org.traccar.client.util.Utilities;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Queue;
import java.util.TimeZone;

/**
 * @author Chathura Wijesinghe <cdanasiri@gmail.com> on 5/29/15.
 */
public class TraccarService extends Service {
    public static final String LOG_TAG = "Traccar.TraccarService";

    private static final AsyncHttpClient httpClient = new AsyncHttpClient();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String id;
    private String address;
    private int port;
    private int interval;
    private String provider;
    private boolean extended;
    private RequestParams mRequestParams;

    private SharedPreferences sharedPreferences;
    private PositionProvider positionProvider;

    private PowerManager.WakeLock wakeLock;

    private Queue<RequestParams> messageQueue;
    private PositionProvider.PositionListener positionListener = new PositionProvider.PositionListener() {

        @Override
        public void onPositionUpdate(Location location) {
            if (location != null) {
                setNewLocation(createLocationMessage(location, getBatteryLevel()));
            }
        }
    };
    SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            StatusActivity.addMessage(getString(R.string.status_preference_update));
            try {
                if (key.equals(TraccarActivity.KEY_ADDRESS)) {

                    address = sharedPreferences.getString(TraccarActivity.KEY_ADDRESS, null);

                } else if (key.equals(TraccarActivity.KEY_PORT)) {

                    port = Integer.valueOf(sharedPreferences.getString(TraccarActivity.KEY_PORT, null));

                } else if (key.equals(TraccarActivity.KEY_INTERVAL)) {

                    interval = Integer.valueOf(sharedPreferences.getString(TraccarActivity.KEY_INTERVAL, null));
                    positionProvider.stopUpdates();
                    positionProvider = new PositionProvider(TraccarService.this, provider, interval * 1000, positionListener);
                    positionProvider.startUpdates();

                } else if (key.equals(TraccarActivity.KEY_ID)) {

                    id = sharedPreferences.getString(TraccarActivity.KEY_ID, null);

                } else if (key.equals(TraccarActivity.KEY_PROVIDER)) {

                    provider = sharedPreferences.getString(TraccarActivity.KEY_PROVIDER, null);
                    positionProvider.stopUpdates();
                    positionProvider = new PositionProvider(TraccarService.this, provider, interval * 1000, positionListener);
                    positionProvider.startUpdates();

                } else if (key.equals(TraccarActivity.KEY_EXTENDED)) {

                    extended = sharedPreferences.getBoolean(TraccarActivity.KEY_EXTENDED, false);

                }
            } catch (Exception error) {
                Log.w(LOG_TAG, error);
            }
        }

    };

    @Override
    public void onCreate() {
        StatusActivity.addMessage(getString(R.string.status_service_create));

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
        wakeLock.acquire();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            id = sharedPreferences.getString(TraccarActivity.KEY_ID, null);
            address = sharedPreferences.getString(TraccarActivity.KEY_ADDRESS, null);
            provider = sharedPreferences.getString(TraccarActivity.KEY_PROVIDER, null);
            port = Integer.valueOf(sharedPreferences.getString(TraccarActivity.KEY_PORT, null));
            interval = Integer.valueOf(sharedPreferences.getString(TraccarActivity.KEY_INTERVAL, null));
            extended = sharedPreferences.getBoolean(TraccarActivity.KEY_EXTENDED, false);
        } catch (Exception error) {
            Log.w(LOG_TAG, error);
        }

        positionProvider = new PositionProvider(this, provider, interval * 1000, positionListener);
        positionProvider.startUpdates();

        sharedPreferences.registerOnSharedPreferenceChangeListener(preferenceChangeListener);

        messageQueue = new LinkedList<RequestParams>();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        StatusActivity.addMessage(getString(R.string.status_service_destroy));

        if (sharedPreferences != null) {
            sharedPreferences.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        }

        if (positionProvider != null) {
            positionProvider.stopUpdates();
        }
        httpClient.cancelAllRequests(true);
        wakeLock.release();
    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    public double getBatteryLevel() {
        if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.ECLAIR) {
            Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, 1);
            return (level * 100.0) / scale;
        } else {
            return 0;
        }
    }

    public void setNewLocation(RequestParams params) {
        messageQueue.offer(params);
        if (isInternetAvailable()) {
            sendMessage(messageQueue.poll());
        }
    }

    private void sendMessage(RequestParams requestParams) {

//        TODO set Request time out and rety count ;)
        httpClient.get(this, getAbsoluteUrl(), requestParams, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                StatusActivity.addMessage(getString(R.string.status_location_update));
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                StatusActivity.addMessage(getString(R.string.status_send_fail));
            }

        });
    }

    private String getAbsoluteUrl() {
        if (address != null)
            return "http://" + address + ":" + port;
        return null;
    }


    public boolean isInternetAvailable() {

        return  isNetworkConnected();

//        TODO Handle me please Check the reachability and send the message , Should not run on Main thread (will throws exeption)
//        if (isNetworkConnected()) {
//            try {
//                InetAddress ipAddr = InetAddress.getByName(address);
//                if (Utilities.IsNullOrEmpty(ipAddr.getHostAddress())) {
//                    return false;
//                } else {
//                    return true;
//                }
//            } catch (Exception e) {
//                return false;
//            }
//        } else
//            return false;
    }


    public boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null) {
            // There are no active networks.
            return false;
        } else
            return true;
    }

    //    http://demo.traccar.org:5055/?id=123456&lat={0}&lon={1}&timestamp={2}&hdop={3}&altitude={4}&speed={5}
    private RequestParams createLocationMessage(Location l, double battery) {

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.ENGLISH);
        calendar.setTimeInMillis(l.getTime());

        String lat = String.valueOf(l.getLatitude());
        String lon = String.valueOf(l.getLongitude());
        String alt = String.valueOf(l.getAltitude());
        String speed = String.valueOf(l.getSpeed() * 1.943844); // speed in knots

        Bundle b = l.getExtras();

        String hdop = b.getString("HDOP");


        mRequestParams = new RequestParams();
        mRequestParams.add("id", id);
        mRequestParams.add("lat", lat);
        mRequestParams.add("lon", lon);
        mRequestParams.add("timestamp", dateFormat.format(calendar.getTime()));
        mRequestParams.add("hdop", hdop);
        mRequestParams.add("altitude", alt);
        mRequestParams.add("speed", speed);

        return mRequestParams;
    }

}
