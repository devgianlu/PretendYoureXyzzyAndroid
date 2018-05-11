package com.gianlu.pretendyourexyzzy;

import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.commonutils.ConnectivityChecker;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;

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
    public void onCreate() {
        super.onCreate();

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

        Pyx.instantiate(this);
    }
}
