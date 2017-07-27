package com.gianlu.pretendyourexyzzy;

import android.app.Application;

import com.gianlu.commonutils.CommonUtils;

public class ThisApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        CommonUtils.setDebug(BuildConfig.DEBUG);
    }
}
