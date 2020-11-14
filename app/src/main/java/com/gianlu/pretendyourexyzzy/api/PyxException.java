package com.gianlu.pretendyourexyzzy.api;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.util.List;
import java.util.Objects;

public class PyxException extends Exception {
    public final JSONObject obj;
    public final String errorCode;
    List<Exception> exceptions = null;

    PyxException(@NonNull JSONObject obj) {
        super(obj.optString("ec") + " -> " + obj.toString());
        this.errorCode = obj.optString("ec");
        this.obj = obj;
    }

    public static boolean solveNotRegistered(@Nullable Context context, @Nullable Exception ex) {
        if (isNotRegistered(ex)) {
            if (context == null) return true;

            InstanceHolder.holder().invalidate();
            // TODO: Launch something?
            return true;
        } else {
            return false;
        }
    }

    public static boolean isNotRegistered(@Nullable Exception ex) {
        return ex instanceof PyxException && Objects.equals(((PyxException) ex).errorCode, "nr");
    }

    public boolean hadException(@NonNull Class<? extends Exception> clazz) {
        if (exceptions == null) return false;

        for (Exception ex : exceptions)
            if (ex.getClass() == clazz)
                return true;

        return false;
    }

    public boolean shouldRetry() {
        return Objects.equals(errorCode, "se") || Objects.equals(errorCode, "nr");
    }
}
