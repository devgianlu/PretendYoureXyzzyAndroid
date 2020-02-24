package com.gianlu.pretendyourexyzzy;

import android.app.Activity;
import android.content.Context;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.pretendyourexyzzy.overloaded.api.OverloadedUtils;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.EventsClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.achievement.Achievement;
import com.google.android.gms.games.achievement.AchievementBuffer;
import com.google.android.gms.games.event.Event;
import com.google.android.gms.games.event.EventBuffer;

import org.intellij.lang.annotations.MagicConstant;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

public final class GPGamesHelper {
    public static final String EVENT_CARDS_PLAYED = "CgkIus2n760REAIQAQ";
    public static final String EVENT_ROUNDS_PLAYED = "CgkIus2n760REAIQAg";
    public static final String EVENT_ROUNDS_JUDGED = "CgkIus2n760REAIQAw";
    public static final String EVENT_ROUNDS_WON = "CgkIus2n760REAIQBA";
    public static final String EVENT_GAMES_WON = "CgkIus2n760REAIQBg";
    public static final String ACH_WIN_10_ROUNDS = "CgkIus2n760REAIQBw";
    public static final String ACH_WIN_30_ROUNDS = "CgkIus2n760REAIQCA";
    public static final String ACH_WIN_69_ROUNDS = "CgkIus2n760REAIQCQ";
    public static final String ACH_WIN_420_ROUNDS = "CgkIus2n760REAIQCg";
    public static final String ACH_3_PEOPLE_GAME = "CgkIus2n760REAIQDA";
    public static final String ACH_5_PEOPLE_GAME = "CgkIus2n760REAIQDQ";
    public static final String ACH_10_PEOPLE_GAME = "CgkIus2n760REAIQDg ";
    public static final String ACH_CARDCAST = "CgkIus2n760REAIQDw";
    public static final String LEAD_WIN_RATE = "CgkIus2n760REAIQEA";
    public static final String[] ACHS_WIN_ROUNDS = new String[]{ACH_WIN_10_ROUNDS, ACH_WIN_30_ROUNDS, ACH_WIN_69_ROUNDS, ACH_WIN_420_ROUNDS};
    public static final String[] ACHS_PEOPLE_GAME = new String[]{ACH_3_PEOPLE_GAME, ACH_5_PEOPLE_GAME, ACH_10_PEOPLE_GAME};
    private static EventsClient eventsClient;
    private static AchievementsClient achievementsClient;
    private static LeaderboardsClient leaderboardsClient;

    private GPGamesHelper() {
    }

    @Contract("null -> false")
    private static boolean checkAccount(@Nullable GoogleSignInAccount account) {
        if (!OverloadedUtils.isSignedIn())
            return false;

        if (account == null)
            return false;

        for (Scope scope : account.getGrantedScopes()) {
            if (scope.getScopeUri().equals("https://www.googleapis.com/auth/games_lite"))
                return true;
        }

        return false;
    }

    @Nullable
    private static EventsClient eventsClient(@NonNull Context context) {
        if (eventsClient != null) return eventsClient;

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (checkAccount(account)) return eventsClient = Games.getEventsClient(context, account);
        else return null;
    }

    @Nullable
    private static AchievementsClient achievementsClient(@NonNull Context context) {
        if (achievementsClient != null) return achievementsClient;

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (checkAccount(account))
            return achievementsClient = Games.getAchievementsClient(context, account);
        else
            return null;
    }


    @Nullable
    private static LeaderboardsClient leaderboardsClient(@NonNull Context context) {
        if (leaderboardsClient != null) return leaderboardsClient;

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (checkAccount(account))
            return leaderboardsClient = Games.getLeaderboardsClient(context, account);
        else
            return null;
    }

    public static void setPopupView(@NonNull Activity activity, @MagicConstant(flagsFromClass = Gravity.class) int gravity) {
        View root = activity.getWindow().getDecorView().findViewById(android.R.id.content);
        if (root != null)
            GPGamesHelper.setPopupView(activity, root, gravity);
    }

    public static void setPopupView(@NonNull Context context, @NonNull View view, @MagicConstant(flagsFromClass = Gravity.class) int gravity) {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (!checkAccount(account)) return;

        GamesClient client = Games.getGamesClient(context, account);
        client.setViewForPopups(view);
        client.setGravityForPopups(gravity);
    }

    public static void incrementEvent(@NonNull Context context, int amount, @NonNull String... events) {
        EventsClient client = eventsClient(context);
        if (client != null) {
            for (String ev : events)
                client.increment(ev, amount);
        }
    }

    public static void updateWinRate(@NotNull Context context) {
        EventsClient eventsClient = eventsClient(context);
        LeaderboardsClient leaderboardsClient = leaderboardsClient(context);
        if (eventsClient == null || leaderboardsClient == null) return;

        eventsClient.loadByIds(true, EVENT_ROUNDS_PLAYED, EVENT_ROUNDS_WON).addOnSuccessListener(data -> {
            EventBuffer buffer = data.get();
            if (buffer == null) return;

            Event playedEvent = OverloadedUtils.findEvent(buffer, EVENT_ROUNDS_PLAYED);
            Event wonEvent = OverloadedUtils.findEvent(buffer, EVENT_ROUNDS_WON);
            if (playedEvent == null || wonEvent == null) return;

            long score = (long) (((float) wonEvent.getValue() / (float) playedEvent.getValue()) * 100 * 10000);
            leaderboardsClient.submitScore(LEAD_WIN_RATE, score);
        });
    }

    public static void achievementSteps(@NonNull Context context, int steps, @NonNull String... achievements) {
        AchievementsClient client = achievementsClient(context);
        if (client != null) {
            for (String ac : achievements)
                client.setSteps(ac, steps);
        }
    }

    public static void unlockAchievement(@NonNull Context context, @NonNull String... achievements) {
        AchievementsClient client = achievementsClient(context);
        if (client != null) {
            for (String ac : achievements)
                client.unlock(ac);
        }
    }

    public static void incrementAchievement(@NonNull Context context, int amount, @NonNull String... achievements) {
        AchievementsClient client = achievementsClient(context);
        if (client != null) {
            for (String ac : achievements)
                client.increment(ac, amount);
        }
    }

    public static void loadAchievements(@NonNull Context context, @Nullable Activity activity, @NonNull LoadIterable<Achievement> callback) {
        AchievementsClient client = achievementsClient(context);
        if (client == null) {
            callback.onFailed(new Exception("Failed initializing client!"));
            return;
        }

        OverloadedUtils.callbacks(client.load(false), activity, data -> {
            AchievementBuffer buffer = data.get();
            if (buffer == null) {
                callback.onLoaded(Collections.emptyList());
            } else {
                callback.onLoaded(buffer);
                buffer.release();
            }
        }, callback::onFailed);
    }

    public static void loadEvents(@NonNull Context context, @Nullable Activity activity, @NonNull LoadIterable<Event> callback) {
        EventsClient client = eventsClient(context);
        if (client == null) {
            callback.onFailed(new Exception("Failed initializing client!"));
            return;
        }

        OverloadedUtils.callbacks(client.load(false), activity, data -> {
            EventBuffer buffer = data.get();
            if (buffer == null) {
                callback.onLoaded(Collections.emptyList());
            } else {
                callback.onLoaded(buffer);
                buffer.release();
            }
        }, callback::onFailed);
    }

    public interface LoadIterable<T> {
        void onLoaded(@NonNull Iterable<T> result);

        void onFailed(@NonNull Exception ex);
    }
}
