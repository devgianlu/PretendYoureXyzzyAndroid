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
    }
}
