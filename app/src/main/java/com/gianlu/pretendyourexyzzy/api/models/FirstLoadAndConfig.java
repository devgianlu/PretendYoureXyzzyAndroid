package com.gianlu.pretendyourexyzzy.api.models;

import androidx.annotation.NonNull;

public class FirstLoadAndConfig {
    public final FirstLoad firstLoad;
    public final CahConfig cahConfig;

    public FirstLoadAndConfig(@NonNull FirstLoad firstLoad, @NonNull CahConfig cahConfig) {
        this.firstLoad = firstLoad;
        this.cahConfig = cahConfig;
    }
}
