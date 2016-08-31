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

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TrackingService extends Service {

    private static final String TAG = TrackingService.class.getSimpleName();
    private static final int NOTIFICATION_ID = 1;

    private TrackingController trackingController;

    @SuppressWarnings("deprecation")
    private static Notification createNotification(Context context) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);

        Notification notification = new Notification(android.R.drawable.stat_notify_sync_noanim, null, 0);
        try {
            Method method = notification.getClass().getMethod("setLatestEventInfo", Context.class, CharSequence.class, CharSequence.class, PendingIntent.class);
            try {
                method.invoke(notification, context, context.getString(R.string.app_name), context.getString(R.string.settings_status_on_summary), pendingIntent);
            } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                Log.w(TAG, e);
            }
        } catch (SecurityException | NoSuchMethodException e) {
            Log.w(TAG, e);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            notification.priority = Notification.PRIORITY_MIN;
        }

        return notification;
    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    public static class HideNotificationService extends Service {
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onCreate() {
            startForeground(NOTIFICATION_ID, createNotification(this));
            stopForeground(true);
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            stopSelfResult(startId);
            return START_NOT_STICKY;
        }
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "service create");
        StatusActivity.addMessage(getString(R.string.status_service_create));

        trackingController = new TrackingController(this);
        trackingController.start();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
            startForeground(NOTIFICATION_ID, createNotification(this));
            startService(new Intent(this, HideNotificationService.class));
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onStart(Intent intent, int startId) {
        if (intent != null) {
            AutostartReceiver.completeWakefulIntent(intent);
        }
    }

    @TargetApi(Build.VERSION_CODES.ECLAIR)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        onStart(intent, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "service destroy");
        StatusActivity.addMessage(getString(R.string.status_service_destroy));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ECLAIR) {
            stopForeground(true);
        }

        if (trackingController != null) {
            trackingController.stop();
        }
    }

}
