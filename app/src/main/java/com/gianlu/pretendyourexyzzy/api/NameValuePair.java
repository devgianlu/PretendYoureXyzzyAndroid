package com.gianlu.pretendyourexyzzy.api;

public class NameValuePair {
    private final String key;
    private final String value;

    public NameValuePair(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String key() {
        return key;
    }

    public String value(String fallback) {
        return value == null ? fallback : value;
    }

    public String value() {
        return value;
    }
}
