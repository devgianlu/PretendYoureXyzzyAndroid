package com.gianlu.pretendyourexyzzy;

import com.gianlu.commonutils.AnalyticsApplication;
import com.gianlu.commonutils.ConnectivityChecker;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class ThisApplication extends AnalyticsApplication {

    @Override
    protected boolean isDebug() {
        return BuildConfig.DEBUG;
    }

    @Override
    protected int getTrackerConfiguration() {
        return R.xml.tracking;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(!isDebug());

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
