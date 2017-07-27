package com.gianlu.pretendyourexyzzy.NetIO;

import org.json.JSONObject;

public class PYXException extends Exception {
    public PYXException(JSONObject obj) {
        super(obj.optString("ec"));
    }
}
