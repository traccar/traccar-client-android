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

import java.io.OutputStream;
import java.net.Socket;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;

/**
 * Connection with autoreconnect
 */
public class Connection {

    private String address;
    private int port;

    private String connectMessage;

    private Socket socket;
    private OutputStream socketStream;

    private String status;

    private Context context;

    /**
     * Initialize connection parameters
     */
    public Connection(String address, int port, String message) {
        this.address = address;
        this.port = port;
        connectMessage = message;
        setStatus(Traccar.STATUS_DISCONNECTED);
    }

    /**
     * Get address
     */
    public String getAddress() {
        return address;
    }

    /**
     * Get port
     */
    public int getPort() {
        return port;
    }

    /**
     * Set context
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * Set connection status
     */
    private void setStatus(String status) {
        if (!status.equals(this.status)) {
            this.status = status;
            if (context != null) {
                Intent intent = new Intent(Traccar.MSG_CONNECTION_STATUS);
                intent.putExtra(Traccar.EXTRA_STATUS, status);
                context.sendBroadcast(intent);
            }
        }
    }

    /**
     * Get connection status
     */
    public String getStatus() {
        return status;
    }

    /**
     * Establish connection
     */
    public void connect() {
        try {
            socket = new Socket(address, port);
            socket.setSoTimeout(Traccar.SOCKET_TIMEOUT);
            socketStream = socket.getOutputStream();
            setStatus(Traccar.STATUS_CONNECTED);
            send(connectMessage);
        } catch (Exception e) {
            reconnect();
        }
    }

    /**
     * Send message
     */
    public void send(String message) {
        try {
            if (socketStream != null) {
                socketStream.write(message.getBytes());
                socketStream.flush();
            }
        } catch (Exception e) {
            reconnect();
        }
    }

    /**
     * Close connection
     */
    public void close() {
        handler.removeCallbacks(task);
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception e) {
            }
            socket = null;
        }
        socketStream = null;
        setStatus(Traccar.STATUS_DISCONNECTED);
    }

    private void reconnect() {
        close();
        handler.postDelayed(task, Traccar.RECONNECT_DELAY);
    }

    private Handler handler = new Handler();
    private ReconnectTask task = new ReconnectTask();

    class ReconnectTask implements Runnable {
        @Override
        public void run() {
            connect();
        }
    }
}
