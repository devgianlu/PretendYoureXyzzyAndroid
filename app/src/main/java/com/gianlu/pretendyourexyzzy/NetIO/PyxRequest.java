package com.gianlu.pretendyourexyzzy.NetIO;

import com.gianlu.commonutils.NameValuePair;

import androidx.annotation.NonNull;

public class PyxRequest {
    public final Pyx.Op op;
    public final NameValuePair[] params;

    PyxRequest(@NonNull Pyx.Op op, NameValuePair... params) {
        this.op = op;
        this.params = params;
    }
}
