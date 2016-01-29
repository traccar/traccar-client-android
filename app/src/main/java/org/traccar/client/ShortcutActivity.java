/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.Toast;

public class ShortcutActivity extends Activity {

    public static final String EXTRA_ACTION = "shortcutAction";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkShortcutAction(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        checkShortcutAction(intent);
    }

    private void checkShortcutAction(Intent intent) {
        if (intent.hasExtra(EXTRA_ACTION)) {
            boolean start = intent.getBooleanExtra(EXTRA_ACTION, false);
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit().putBoolean(MainActivity.KEY_STATUS, start).commit();
            if (start) {
                startService(new Intent(this, TrackingService.class));
                Toast.makeText(this, R.string.status_service_create, Toast.LENGTH_SHORT).show();
            } else {
                stopService(new Intent(this, TrackingService.class));
                Toast.makeText(this, R.string.status_service_destroy, Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    }

}
