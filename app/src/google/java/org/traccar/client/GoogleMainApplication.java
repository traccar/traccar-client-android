/*
 * Copyright 2017 Anton Tananaev (anton@traccar.org)
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
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceManager;

import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;

public class GoogleMainApplication extends MainApplication {

    private static final String KEY_RATING_SHOWN = "ratingShown";
    private static final long RATING_THRESHOLD = -24 * 60 * 60 * 1000L;

    private FirebaseAnalytics firebaseAnalytics;

    @Override
    public void onCreate() {
        super.onCreate();
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        IntentFilter filter = new IntentFilter();
        filter.addAction(TrackingService.ACTION_STARTED);
        filter.addAction(TrackingService.ACTION_STOPPED);
        registerReceiver(new ServiceReceiver(), filter);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Override
    public void handleRatingFlow(@NonNull Activity activity) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean ratingShown = preferences.getBoolean(KEY_RATING_SHOWN, false);
        long totalDuration = preferences.getLong(ServiceReceiver.KEY_DURATION, 0);
        if (!ratingShown && totalDuration > RATING_THRESHOLD) {
            ReviewManager reviewManager = ReviewManagerFactory.create(activity);
            reviewManager.requestReviewFlow().addOnCompleteListener(infoTask -> {
                if (infoTask.isSuccessful()) {
                    Task<Void> flow = reviewManager.launchReviewFlow(activity, infoTask.getResult());
                    flow.addOnCompleteListener(flowTask -> {
                        preferences.edit().putBoolean(KEY_RATING_SHOWN, true).apply();
                    });
                }
            });
        }
    }

}
