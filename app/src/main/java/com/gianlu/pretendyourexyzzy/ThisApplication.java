package com.gianlu.pretendyourexyzzy;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.commonutils.preferences.CommonPK;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.pretendyourexyzzy.api.glide.BaseCardGlideLoader;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.starred.StarredCardsDatabase;

import java.io.InputStream;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.signal.SignalDatabaseHelper;
import xyz.gianlu.pyxoverloaded.signal.SignalProtocolHelper;

public class ThisApplication extends AnalyticsApplication {
    public static final String USER_AGENT = "PYX Android by devgianlu";

    @Override
    protected boolean isDebug() {
        return BuildConfig.DEBUG;
    }

    @Nullable
    @Override
    protected String getGithubProjectName() {
        return "PretendYoureXyzzyAndroid";
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Prefs.putBoolean(CommonPK.CRASH_REPORT_ENABLED, true);
        Prefs.putBoolean(CommonPK.TRACKING_ENABLED, true);

        Glide.get(this).getRegistry().prepend(BaseCard.class, InputStream.class, new BaseCardGlideLoader.Factory());

        SignalDatabaseHelper.init(this);
        SignalProtocolHelper.getLocalDeviceId(this);

        OverloadedApi.chat(this);

        StarredCardsDatabase.migrateFromPrefs(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        OverloadedApi.close();
        SignalDatabaseHelper.get().close();
    }
}
