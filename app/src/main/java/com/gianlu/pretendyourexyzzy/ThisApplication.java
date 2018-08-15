package com.gianlu.pretendyourexyzzy;

import com.bumptech.glide.Glide;
import com.gianlu.commonutils.Analytics.AnalyticsApplication;
import com.gianlu.pretendyourexyzzy.NetIO.BaseCardUrlLoader;
import com.gianlu.pretendyourexyzzy.NetIO.Models.BaseCard;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;

import java.io.InputStream;

public class ThisApplication extends AnalyticsApplication {
    public static final String USER_AGENT = "PYX Android by devgianlu";

    @Override
    protected boolean isDebug() {
        return BuildConfig.DEBUG;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Pyx.instantiate();
        Glide.get(this).getRegistry().prepend(BaseCard.class, InputStream.class, new BaseCardUrlLoader.Factory());
    }
}
