package com.gianlu.pretendyourexyzzy;

import com.bumptech.glide.Glide;
import com.gianlu.commonutils.analytics.AnalyticsApplication;
import com.gianlu.pretendyourexyzzy.api.glide.BaseCardGlideLoader;
import com.gianlu.pretendyourexyzzy.api.glide.FriendStatusGlideLoader;
import com.gianlu.pretendyourexyzzy.api.glide.PlayerGlideLoader;
import com.gianlu.pretendyourexyzzy.api.glide.UserDataGlideLoader;
import com.gianlu.pretendyourexyzzy.api.models.GameInfo;
import com.gianlu.pretendyourexyzzy.api.models.cards.BaseCard;
import com.gianlu.pretendyourexyzzy.starred.StarredCardsDatabase;

import java.io.InputStream;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.model.FriendStatus;
import xyz.gianlu.pyxoverloaded.model.UserData;
import xyz.gianlu.pyxoverloaded.signal.SignalDatabaseHelper;
import xyz.gianlu.pyxoverloaded.signal.SignalProtocolHelper;

public class ThisApplication extends AnalyticsApplication {
    public static final String USER_AGENT = "PYX Android by devgianlu";

    @Override
    protected boolean isDebug() {
        return BuildConfig.DEBUG;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Glide.get(this).getRegistry().prepend(BaseCard.class, InputStream.class, new BaseCardGlideLoader.Factory());
        Glide.get(this).getRegistry().prepend(FriendStatus.class, InputStream.class, new FriendStatusGlideLoader.Factory());
        Glide.get(this).getRegistry().prepend(UserData.class, InputStream.class, new UserDataGlideLoader.Factory());
        Glide.get(this).getRegistry().prepend(GameInfo.Player.class, InputStream.class, new PlayerGlideLoader.Factory());

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
