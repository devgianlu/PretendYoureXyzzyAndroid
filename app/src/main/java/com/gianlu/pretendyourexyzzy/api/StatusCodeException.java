package com.gianlu.pretendyourexyzzy.api;

import java.io.IOException;

import okhttp3.Response;

public class StatusCodeException extends IOException {
    public StatusCodeException(Response resp) {
        this(resp.code(), resp.message());
    }

    private StatusCodeException(int code, String message) {
        super(code + ((message == null || message.isEmpty()) ? "" : (": " + message)));
    }
}
