/*
 * Copyright 2023 Anton-V-K
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

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

import androidx.preference.PreferenceManager

class StatusWidget : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (TrackingService.ACTION_STARTED == intent.action)
            updateWidgets(context, true)
        else if (TrackingService.ACTION_STOPPED == intent.action)
            updateWidgets(context, false)
        else
            super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val enabled = prefs.getBoolean(MainFragment.KEY_STATUS, false)
        update(context, appWidgetManager, appWidgetIds, enabled)
    }

    fun updateWidgets(context: Context, enabled: Boolean) {
        val manager = AppWidgetManager.getInstance(context)
        val appWidgetIds = manager.getAppWidgetIds(ComponentName(context, StatusWidget::class.java.name))
        update(context, manager, appWidgetIds, enabled)
    }

    private fun update(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray, enabled: Boolean) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.status_widget)
            views.setImageViewResource(R.id.image_enabled, if (enabled) R.mipmap.ic_start else R.mipmap.ic_stop)

            val intent = Intent(context, MainActivity::class.java)
            val clickIntent = PendingIntent.getActivity(context, 0, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.image_enabled, clickIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

}
