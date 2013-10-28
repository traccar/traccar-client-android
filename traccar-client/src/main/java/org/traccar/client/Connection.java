/*
 * Copyright 2012 - 2013 Anton Tananaev (anton.tananaev@gmail.com)
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

import java.io.Closeable;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Asynchronous connection
 * 
 * All methods should be called from UI thread only.
 */
public class Connection implements Closeable {

    public static final String LOG_TAG = "Traccar.Connection";
    public static final int SOCKET_TIMEOUT = 10 * 1000;

    /**
     * Callback interface
     */
    public interface ConnectionHandler {
        void onConnected(boolean result);
        void onSent(boolean result);
    }

    private ConnectionHandler handler;

    private Socket socket;
    private OutputStream socketStream;

    private boolean closed;
    private boolean busy;

    public boolean isClosed() {
        return closed;
    }

    public boolean isBusy() {
        return busy;
    }

    public Connection(ConnectionHandler handler) {
        this.handler = handler;
        closed = false;
        busy = false;
    }

    public void connect(final String address, final int port) {
        busy = true;

        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                try {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(address, port));
                    socket.setSoTimeout(SOCKET_TIMEOUT);
                    socketStream = socket.getOutputStream();
                    return true;
                } catch (Exception e) {
                    Log.w(LOG_TAG, e.getMessage());
                    return false;
                }
            }

            @Override
            protected void onCancelled() {
                if (!closed) {
                    busy = false;
                    handler.onConnected(false);
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (!closed) {
                    busy = false;
                    handler.onConnected(result);
                }
            }

        }.execute();

    }

    public void send(String message) {
        busy = true;

        new AsyncTask<String, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(String... params) {
                try {
                    socketStream.write(params[0].getBytes());
                    socketStream.flush();
                    return true;
                } catch (Exception e) {
                    Log.w(LOG_TAG, e.getMessage());
                    return false;
                }
            }

            @Override
            protected void onCancelled() {
                if (!closed) {
                    busy = false;
                    handler.onSent(false);
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (!closed) {
                    busy = false;
                    handler.onSent(result);
                }
            }

        }.execute(message);

    }

    @Override
    public void close() {
        closed = true;
        try {
            if (socketStream != null) {
                socketStream.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

}
