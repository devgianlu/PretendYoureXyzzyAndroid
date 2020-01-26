package com.gianlu.pretendyourexyzzy;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.games.EventsClient;
import com.google.android.gms.games.Games;

public final class GPGamesHelper {
    public static final String EVENT_CARDS_PLAYED = "CgkIus2n760REAIQAQ";
    public static final String EVENT_ROUNDS_PLAYED = "CgkIus2n760REAIQAg";
    public static final String EVENT_ROUNDS_JUDGED = "CgkIus2n760REAIQAw";
    public static final String EVENT_ROUNDS_WON = "CgkIus2n760REAIQBA";
    public static final String EVENT_GAMES_WON = "CgkIus2n760REAIQBg";

    private GPGamesHelper() {
    }

    @Nullable
    private static EventsClient client(@NonNull Context context) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) return null;
        else return Games.getEventsClient(context, account);
    }

    public static void incrementEvent(@NonNull Context context, @NonNull String event, int amount) {
        EventsClient client = client(context);
        if (client != null) client.increment(event, amount);
    }
}
