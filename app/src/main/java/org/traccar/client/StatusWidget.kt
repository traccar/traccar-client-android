/*
 * Copyright 2016 - 2022 Anton Tananaev (anton@traccar.org)
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

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.*
import android.widget.RemoteViews

import androidx.preference.PreferenceManager

/**
 * Implementation of Status Widget functionality, which can display the status of the service -
 * whether it is enabled or not.
 */
class StatusWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        update(context, appWidgetManager, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
        val filter = IntentFilter()
        filter.addAction(TrackingService.ACTION_STARTED)
        filter.addAction(TrackingService.ACTION_STOPPED)
        // Note: we must register through the app context as a workaround
        // for 'BroadcastReceiver components are not allowed to register to receive intents'
        context.applicationContext.registerReceiver(Companion, filter)
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
        context.applicationContext.unregisterReceiver(Companion)
    }

    companion object: BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (TrackingService.ACTION_STARTED == intent.action
                    || TrackingService.ACTION_STOPPED == intent.action) {
                updateWidgets(context)
            }
        }

        // Performs update for all widgets of this class.
        fun updateWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val appWidgetIds = manager.getAppWidgetIds(ComponentName(context, StatusWidget::class.java.name))
            update(context, manager, appWidgetIds)
        }

        // Performs update for the widgets with given identifiers.
        internal fun update(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            val enabled = prefs.getBoolean(MainFragment.KEY_STATUS, false)
            // There may be multiple widgets active, so update all of them
            for (appWidgetId in appWidgetIds) {
                // Construct the RemoteViews object
                val views = RemoteViews(context.packageName, R.layout.status_widget)
                views.setImageViewResource(R.id.ivEnabled, if (enabled) R.mipmap.ic_start else R.mipmap.ic_stop)

                // Instruct the widget manager to update the widget
                appWidgetManager.updateAppWidget(appWidgetId, views)
            }
        }
    }

}