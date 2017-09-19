package com.gianlu.pretendyourexyzzy;

import android.app.Application;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.ConnectivityChecker;
import com.gianlu.commonutils.Prefs;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class ThisApplication extends Application {
    public static final String CATEGORY_USER_INPUT = "User input";
    public static final String ACTION_STARRED_CARD_ADD = "Starred card added";
    public static final String ACTION_JOIN_GAME = "Joined game";
    public static final String ACTION_SPECTATE_GAME = "Spectating game";
    public static final String ACTION_LEFT_GAME = "Left game";
    public static final String ACTION_SENT_GLOBAL_MSG = "Sent message on global chat";
    public static final String ACTION_SENT_GAME_MSG = "Sent message on game chat";
    public static final String ACTION_ADDED_CARDCAST = "Added Cardcast deck";
    public static final String ACTION_JUDGE_CARD = "Judged card";
    public static final String ACTION_PLAY_CUSTOM_CARD = "Played custom card";
    public static final String ACTION_PLAY_CARD = "Played card";
    public static final String ACTION_DONATE_OPEN = "Donation dialog opened";
    public static final String ACTION_STARRED_DECK_ADD = "Starred deck added";
    private static Tracker tracker;

    @NonNull
    private static Tracker getTracker(Application application) {
        if (tracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(application.getApplicationContext());
            analytics.enableAutoActivityReports(application);
            tracker = analytics.newTracker(R.xml.tracking);
            tracker.enableAdvertisingIdCollection(true);
            tracker.enableExceptionReporting(true);
        }

        return tracker;
    }

    public static void sendAnalytics(Context context, @Nullable Map<String, String> map) {
        if (tracker != null && context != null && !Prefs.getBoolean(context, Prefs.Keys.TRACKING_DISABLE, false) && !BuildConfig.DEBUG)
            tracker.send(map);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(getBaseContext()));

        CommonUtils.setDebug(BuildConfig.DEBUG);
        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(!BuildConfig.DEBUG);
        tracker = getTracker(this);

        ConnectivityChecker.setUserAgent(getString(R.string.app_name));
        ConnectivityChecker.setProvider(new ConnectivityChecker.URLProvider() {
            @Override
            public URL getUrl(boolean useDotCom) throws MalformedURLException {
                return new URL("http://pretendyoure.xyz/zy/");
            }

            @Override
            public boolean validateResponse(HttpURLConnection connection) throws IOException {
                return connection.getResponseCode() == 200;
            }
        });
    }
}
