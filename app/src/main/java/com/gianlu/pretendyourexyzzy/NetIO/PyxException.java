package com.gianlu.pretendyourexyzzy.NetIO;

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

    public boolean shouldRetry() {
        return Objects.equals(errorCode, "se") || Objects.equals(errorCode, "nr");
    }
}
