package com.gianlu.pretendyourexyzzy.NetIO;

import android.support.annotation.NonNull;

import com.gianlu.commonutils.NameValuePair;

public class PyxRequest {
    public final Pyx.Op op;
    public final NameValuePair[] params;

    PyxRequest(@NonNull Pyx.Op op, NameValuePair... params) {
        this.op = op;
        this.params = params;
    }
}
