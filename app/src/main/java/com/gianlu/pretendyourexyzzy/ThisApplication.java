package com.gianlu.pretendyourexyzzy;

import android.app.Application;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.ConnectivityChecker;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class ThisApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        CommonUtils.setDebug(BuildConfig.DEBUG);

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
