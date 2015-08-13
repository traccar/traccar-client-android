/*
 * Copyright 2015 Anton Tananaev (anton.tananaev@gmail.com)
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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;

public class TrackingController implements PositionProvider.PositionListener {

    private static final int RETRY_DELAY = 30 * 1000;

    private boolean isWaiting;

    private Context context;
    private Handler handler;
    private SharedPreferences preferences;

    private PositionProvider positionProvider;
    private DatabaseHelper databaseHelper;

    public TrackingController(Context context) {
        this.context = context;
        handler = new Handler();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        positionProvider = new PositionProvider(context, this,
                preferences.getString(MainActivity.KEY_DEVICE, null),
                preferences.getString(MainActivity.KEY_PROVIDER, null),
                Integer.parseInt(preferences.getString(MainActivity.KEY_INTERVAL, null)) * 1000);
        databaseHelper = new DatabaseHelper(context);
    }

    public void start() {
        positionProvider.startUpdates();
    }

    public void stop() {
        positionProvider.stopUpdates();
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onPositionUpdate(Position position) {
        if (position != null) {
            StatusActivity.addMessage(context.getString(R.string.status_location_update));
            write(position);
        }
    }

    //
    // write -> read -> send -> delete -> read
    //
    // read -> send -> retry -> read -> send
    //

    private void read() {
        databaseHelper.selectPositionAsync(new DatabaseHelper.DatabaseHandler<Position>() {
            @Override
            public void onSuccess(Position result) {
                if (result != null) {
                    send(result);
                } else {
                    isWaiting = true;
                }
            }

            @Override
            public void onFailure(RuntimeException error) {
                retry();
            }
        });
    }

    private void write(Position position) {
        databaseHelper.insertPositionAsync(position, new DatabaseHelper.DatabaseHandler<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (isWaiting) {
                    read();
                    isWaiting = false;
                }
            }

            @Override
            public void onFailure(RuntimeException error) {
            }
        });
    }

    private void delete(Position position) {
        databaseHelper.deletePositionAsync(position.getId(), new DatabaseHelper.DatabaseHandler<Void>() {
            @Override
            public void onSuccess(Void result) {
                read();
            }

            @Override
            public void onFailure(RuntimeException error) {
                retry();
            }
        });
    }

    private void send(final Position position) {
        String request = ProtocolFormatter.formatRequest(
                preferences.getString(MainActivity.KEY_ADDRESS, null),
                Integer.parseInt(preferences.getString(MainActivity.KEY_PORT, null)),
                position);

        RequestManager.sendRequestAsync(request, new RequestManager.RequestHandler() {
            @Override
            public void onSuccess() {
                delete(position);
            }

            @Override
            public void onFailure() {
                StatusActivity.addMessage(context.getString(R.string.status_send_fail));
                retry();
            }
        });
    }

    private void retry() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                read();
            }
        }, RETRY_DELAY);
    }

}
