package com.gianlu.pretendyourexyzzy;

import com.bumptech.glide.Glide;
import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.pretendyourexyzzy.api.BaseCardUrlLoader;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.starred.StarredCardsDatabase;

import java.io.InputStream;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.signal.SignalDatabaseHelper;

public class ThisApplication extends AnalyticsApplication {
    public static final String USER_AGENT = "PYX Android by devgianlu";

    @Override
    protected boolean isDebug() {
        return BuildConfig.DEBUG;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Glide.get(this).getRegistry().prepend(BaseCard.class, InputStream.class, new BaseCardUrlLoader.Factory());
        SignalDatabaseHelper.init(this);

        StarredCardsDatabase.migrateFromPrefs(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        OverloadedApi.close();
        SignalDatabaseHelper.get().close();
    }
}
