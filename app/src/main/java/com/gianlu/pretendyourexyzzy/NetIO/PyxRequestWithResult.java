package com.gianlu.pretendyourexyzzy.NetIO;

import androidx.annotation.NonNull;

public class PyxRequestWithResult<E> extends PyxRequest {
    public final Pyx.Processor<E> processor;

    public PyxRequestWithResult(@NonNull Pyx.Op op, @NonNull Pyx.Processor<E> processor, Param... params) {
        super(op, params);
        this.processor = processor;
    }
}
