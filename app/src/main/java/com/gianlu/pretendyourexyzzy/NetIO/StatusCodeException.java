package com.gianlu.pretendyourexyzzy.NetIO;

import java.io.IOException;

import cz.msebera.android.httpclient.StatusLine;

public class StatusCodeException extends IOException {
    public StatusCodeException(StatusLine sl) {
        this(sl.getStatusCode(), sl.getReasonPhrase());
    }

    public StatusCodeException(int code, String message) {
        super(code + ": " + message);
    }
}
