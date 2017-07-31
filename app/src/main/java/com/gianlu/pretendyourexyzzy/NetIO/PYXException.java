package com.gianlu.pretendyourexyzzy.NetIO;

import org.json.JSONObject;

public class PYXException extends Exception {
    public final JSONObject obj;
    public final String errorCode;

    public PYXException(JSONObject obj) {
        super(obj.optString("ec") + " -> " + obj.toString());
        this.errorCode = obj.optString("ec");
        this.obj = obj;
    }
}
