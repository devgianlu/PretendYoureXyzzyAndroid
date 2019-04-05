package com.gianlu.pretendyourexyzzy;

import com.gianlu.commonutils.Preferences.Prefs;

import androidx.annotation.NonNull;

public final class BlockedUsers {

    private BlockedUsers() {
    }

    public static boolean isBlocked(@NonNull String name) {
        return Prefs.setContains(PK.BLOCKED_USERS, name);
    }

    public static void block(@NonNull String name) {
        Prefs.addToSet(PK.BLOCKED_USERS, name);
    }

    public static void unblock(@NonNull String name) {
        Prefs.removeFromSet(PK.BLOCKED_USERS, name);
    }
}
