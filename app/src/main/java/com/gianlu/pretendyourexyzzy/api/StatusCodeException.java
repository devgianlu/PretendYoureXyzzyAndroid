package com.gianlu.pretendyourexyzzy.api;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Response;

public class StatusCodeException extends IOException {
    public final int code;

    public StatusCodeException(@NotNull Response resp) {
        this(resp.code(), resp.message());
    }

    private StatusCodeException(int code, String message) {
        super(code + ((message == null || message.isEmpty()) ? "" : (": " + message)));
        this.code = code;
    }
}
