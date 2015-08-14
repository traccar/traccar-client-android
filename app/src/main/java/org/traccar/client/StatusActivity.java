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

import java.text.DateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;

public class StatusActivity extends ListActivity {

    private static final int LIMIT = 20;

    private static final LinkedList<String> messages = new LinkedList<String>();
    private static final Set<ArrayAdapter<String>> adapters = new HashSet<ArrayAdapter<String>>();

    private static void notifyAdapters() {
        for (ArrayAdapter<String> adapter : adapters) {
            adapter.notifyDataSetChanged();
        }
    }

    public static void addMessage(String message) {
        DateFormat format = DateFormat.getTimeInstance(DateFormat.SHORT);
        message = format.format(new Date()) + " - " + message;
        messages.add(message);
        while (messages.size() > LIMIT) {
            messages.removeFirst();
        }
        notifyAdapters();
    }

    public static void clearMessages() {
        messages.clear();
        notifyAdapters();
    }

    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.status);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, android.R.id.text1, messages);
        setListAdapter(adapter);
        adapters.add(adapter);
    }

    @Override
    protected void onDestroy() {
        adapters.remove(adapter);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.status, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.clear) {
            clearMessages();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
