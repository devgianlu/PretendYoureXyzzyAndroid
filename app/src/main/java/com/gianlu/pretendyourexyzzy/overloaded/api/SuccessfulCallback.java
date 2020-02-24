package com.gianlu.pretendyourexyzzy.overloaded.api;

import org.jetbrains.annotations.NotNull;

public interface SuccessfulCallback {
    void onSuccessful();

    void onFailed(@NotNull Exception ex);
}
