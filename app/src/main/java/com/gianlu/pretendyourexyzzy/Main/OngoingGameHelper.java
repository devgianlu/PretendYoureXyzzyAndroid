package com.gianlu.pretendyourexyzzy.Main;


import android.support.annotation.Nullable;

public final class OngoingGameHelper {
    private static Listener listener;

    public static void setup(Listener listener) {
        OngoingGameHelper.listener = listener;
    }

    @Nullable
    public static Listener get() {
        return listener;
    }

    public interface Listener {
        boolean canModifyCardcastDecks();

        void addCardcastDeck(String code);

        void addCardcastStarredDecks();
    }
}
