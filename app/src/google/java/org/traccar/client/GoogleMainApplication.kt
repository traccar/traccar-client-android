/*
 * Copyright 2017 - 2021 Anton Tananaev (anton@traccar.org)
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

import android.app.Activity
import android.content.IntentFilter
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.tasks.Task
import com.google.firebase.analytics.FirebaseAnalytics

class GoogleMainApplication : MainApplication() {

    private var firebaseAnalytics: FirebaseAnalytics? = null

    override fun onCreate() {
        super.onCreate()
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        val filter = IntentFilter()
        filter.addAction(TrackingService.ACTION_STARTED)
        filter.addAction(TrackingService.ACTION_STOPPED)
        registerReceiver(ServiceReceiver(), filter)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    override fun handleRatingFlow(activity: Activity) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val ratingShown = preferences.getBoolean(KEY_RATING_SHOWN, false)
        val totalDuration = preferences.getLong(ServiceReceiver.KEY_DURATION, 0)
        if (!ratingShown && totalDuration > RATING_THRESHOLD) {
            val reviewManager = ReviewManagerFactory.create(activity)
            reviewManager.requestReviewFlow().addOnCompleteListener { infoTask: Task<ReviewInfo?> ->
                if (infoTask.isSuccessful) {
                    val flow = reviewManager.launchReviewFlow(activity, infoTask.result)
                    flow.addOnCompleteListener { preferences.edit().putBoolean(KEY_RATING_SHOWN, true).apply() }
                }
            }
        }
    }

    companion object {
        private const val KEY_RATING_SHOWN = "ratingShown"
        private const val RATING_THRESHOLD = -24 * 60 * 60 * 1000L
    }
}
