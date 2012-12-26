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

import java.io.Closeable;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

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

    public Connection(ConnectionHandler handler) {
        this.handler = handler;
    }

    public void connect(String address, int port) {

        new AsyncTask<SocketAddress, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(SocketAddress... params) {
                try {
                    socket = new Socket();
                    socket.connect(params[0]);
                    socket.setSoTimeout(SOCKET_TIMEOUT);
                    socketStream = socket.getOutputStream();
                    handler.onConnected(true);
                } catch (Exception e) {
                    close();
                    return false;
                }
                return true;
            }

            @Override
            protected void onCancelled(Boolean result) {
                handler.onConnected(false);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                handler.onConnected(result);
            }

        }.execute(InetSocketAddress.createUnresolved(address, port));

    }

    public void send(String message) {

        new AsyncTask<String, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(String... params) {
                try {
                    socketStream.write(params[0].getBytes());
                    socketStream.flush();
                } catch (Exception e) {
                    close();
                    return false;
                }
                return true;
            }

            @Override
            protected void onCancelled(Boolean result) {
                handler.onSent(false);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                handler.onSent(result);
            }

        }.execute(message);

    }

    @Override
    public void close() {
        try {
            if (socketStream != null) {
                socketStream.close();
                socketStream = null;
            }
            if (socket != null) {
                socket.close();
                socket = null;
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, e.getMessage());
        }
    }

}
