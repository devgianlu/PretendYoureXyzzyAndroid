package com.gianlu.pretendyourexyzzy.api;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import com.gianlu.pretendyourexyzzy.LoadingActivity;

import org.json.JSONObject;

import java.util.Objects;

public class PyxException extends Exception {
    public final JSONObject obj;
    public final String errorCode;

    PyxException(JSONObject obj) {
        super(obj.optString("ec") + " -> " + obj.toString());
        this.errorCode = obj.optString("ec");
        this.obj = obj;
    }

    public static boolean solveNotRegistered(@Nullable Context context, @Nullable Exception ex) {
        if (isNotRegistered(ex)) {
            if (context == null) return true;

            context.startActivity(new Intent(context, LoadingActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            return true;
        } else {
            return false;
        }
    }

    public static boolean isNotRegistered(@Nullable Exception ex) {
        return ex instanceof PyxException && Objects.equals(((PyxException) ex).errorCode, "nr");
    }

    public boolean shouldRetry() {
        return Objects.equals(errorCode, "se") || Objects.equals(errorCode, "nr");
    }
}
