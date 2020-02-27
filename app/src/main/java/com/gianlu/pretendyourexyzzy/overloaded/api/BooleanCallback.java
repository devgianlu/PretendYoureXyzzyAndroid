package com.gianlu.pretendyourexyzzy.overloaded.api;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;

@UiThread
public interface BooleanCallback {
    void onResult(boolean result);

    void onFailed(@NonNull Exception ex);
}
