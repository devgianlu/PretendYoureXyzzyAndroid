package com.gianlu.pretendyourexyzzy.NetIO;

import java.io.IOException;

import okhttp3.Response;

public class StatusCodeException extends IOException {
    StatusCodeException(Response resp) {
        this(resp.code(), resp.message());
    }

    StatusCodeException(int code, String message) {
        super(code + ": " + message);
    }
}
