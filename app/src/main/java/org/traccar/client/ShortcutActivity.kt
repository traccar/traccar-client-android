/*
 * Copyright 2016 - 2021 Anton Tananaev (anton@traccar.org)
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
package org.traccar.client

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.preference.PreferenceManager
import org.traccar.client.PositionProvider.PositionListener
import org.traccar.client.ProtocolFormatter.formatRequest
import org.traccar.client.RequestManager.RequestHandler
import org.traccar.client.RequestManager.sendRequestAsync

class ShortcutActivity : AppCompatActivity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!executeAction(intent)) {
            setContentView(R.layout.list)
            val items = arrayOf(
                getString(R.string.shortcut_start),
                getString(R.string.shortcut_stop),
                getString(R.string.shortcut_sos)
            )
            val listView = findViewById<ListView>(android.R.id.list)
            listView.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
            listView.onItemClickListener = OnItemClickListener { _, _, position, _ ->
                when (position) {
                    0 -> setShortcutResult(items[position], R.mipmap.ic_start, ACTION_START)
                    1 -> setShortcutResult(items[position], R.mipmap.ic_stop, ACTION_STOP)
                    2 -> setShortcutResult(items[position], R.mipmap.ic_sos, ACTION_SOS)
                }
                finish()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        executeAction(intent)
    }

    private fun setShortcutResult(label: String, @DrawableRes iconResId: Int, action: String) {
        val intent = Intent(Intent.ACTION_DEFAULT, null, this, ShortcutActivity::class.java)
        intent.putExtra(EXTRA_ACTION, action)
        val shortcut = ShortcutInfoCompat.Builder(this, action)
            .setShortLabel(label)
            .setIcon(IconCompat.createWithResource(this, iconResId))
            .setIntent(intent)
            .build()
        setResult(RESULT_OK, ShortcutManagerCompat.createShortcutResultIntent(this, shortcut))
    }

    private fun sendAlarm() {
        PositionProviderFactory.create(this, object : PositionListener {
            override fun onPositionUpdate(position: Position) {
                val preferences = PreferenceManager.getDefaultSharedPreferences(this@ShortcutActivity)
                val request = formatRequest(preferences.getString(MainFragment.KEY_URL, null)!!, position, ALARM_SOS)
                sendRequestAsync(request, object : RequestHandler {
                    override fun onComplete(success: Boolean) {
                        if (success) {
                            Toast.makeText(this@ShortcutActivity, R.string.status_send_success, Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@ShortcutActivity, R.string.status_send_fail, Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            }

            override fun onPositionError(error: Throwable) {
                Toast.makeText(this@ShortcutActivity, error.message, Toast.LENGTH_LONG).show()
            }
        }).requestSingleLocation()
    }

    private fun executeAction(intent: Intent): Boolean {
        val action: String? = if (intent.hasExtra("shortcutAction")) {
            if (intent.getBooleanExtra("shortcutAction", false)) ACTION_START else ACTION_STOP
        } else {
            intent.getStringExtra(EXTRA_ACTION)
        }
        if (action != null) {
            when (action) {
                ACTION_START -> {
                    PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(MainFragment.KEY_STATUS, true).apply()
                    ContextCompat.startForegroundService(this, Intent(this, TrackingService::class.java))
                    Toast.makeText(this, R.string.status_service_create, Toast.LENGTH_SHORT).show()
                }
                ACTION_STOP -> {
                    PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean(MainFragment.KEY_STATUS, false).apply()
                    stopService(Intent(this, TrackingService::class.java))
                    Toast.makeText(this, R.string.status_service_destroy, Toast.LENGTH_SHORT).show()
                }
                ACTION_SOS -> {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        sendAlarm()
                    } else {
                        Toast.makeText(this, R.string.status_send_fail, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            finish()
        }
        return action != null
    }

    companion object {
        const val EXTRA_ACTION = "action"
        const val ACTION_START = "start"
        const val ACTION_STOP = "stop"
        const val ACTION_SOS = "sos"
        private const val ALARM_SOS = "sos"
    }
}
