package com.gianlu.pretendyourexyzzy.api;

import androidx.annotation.NonNull;

public class PyxRequest {
    public final Pyx.Op op;
    public final Param[] params;

    PyxRequest(@NonNull Pyx.Op op, Param... params) {
        this.op = op;
        this.params = params;
    }

    static class Param {
        private final String key;
        private final String value;

        Param(String key, String value) {
            this.key = key;
            this.value = value;
        }

        String key() {
            return key;
        }

        String value() {
            return value;
        }

        String value(String fallback) {
            return value == null ? fallback : value;
        }

        @Override
        public String toString() {
            return '{' + key + ": " + value + '}';
        }
    }
}
