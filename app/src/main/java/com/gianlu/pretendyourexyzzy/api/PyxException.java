package com.gianlu.pretendyourexyzzy.api;

import androidx.annotation.NonNull;

import com.gianlu.pretendyourexyzzy.R;

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

    public boolean shouldRetry() {
        return Objects.equals(errorCode, "se") || Objects.equals(errorCode, "nr");
    }

    public int getPyxMessage() {
        switch (errorCode) {
            case "rn":
                return R.string.reservedNickname;
            case "in":
                return R.string.invalidNickname;
            case "niu":
                return R.string.alreadyUsedNickname;
            case "tmu":
                return R.string.tooManyUsers;
            case "iid":
                return R.string.invalidIdCode;
            default:
                return R.string.failedLoading_changeServerRetry;
        }
    }
}
