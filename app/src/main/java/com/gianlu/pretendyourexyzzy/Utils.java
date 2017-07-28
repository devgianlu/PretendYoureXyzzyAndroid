package com.gianlu.pretendyourexyzzy;

import android.support.annotation.Nullable;

import com.gianlu.commonutils.Toaster;

import java.util.Objects;

import cz.msebera.android.httpclient.client.CookieStore;
import cz.msebera.android.httpclient.cookie.Cookie;

public class Utils {

    @Nullable
    public static Cookie findCookie(CookieStore store, String name) {
        for (Cookie cookie : store.getCookies())
            if (Objects.equals(cookie.getName(), name))
                return cookie;

        return null;
    }

    public static class Messages {
        public static final Toaster.Message FAILED_LOADING = new Toaster.Message(R.string.failedLoading, true);
        public static final Toaster.Message FAILED_SEND_MESSAGE = new Toaster.Message(R.string.failedSendMessage, true);
        public static final Toaster.Message FAILED_JOINING = new Toaster.Message(R.string.failedJoining, true);
        public static final Toaster.Message WRONG_PASSWORD = new Toaster.Message(R.string.wrongPassword, false);
        public static final Toaster.Message GAME_FULL = new Toaster.Message(R.string.gameFull, false);
        public static final Toaster.Message FAILED_SPECTATING = new Toaster.Message(R.string.failedSpectating, true);
        public static final Toaster.Message FAILED_LEAVING = new Toaster.Message(R.string.failedLeaving, true);
    }
}
