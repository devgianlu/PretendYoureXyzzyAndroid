package com.gianlu.pretendyourexyzzy.overloaded;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.pretendyourexyzzy.GPGamesHelper;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.google.android.gms.games.achievement.Achievement;
import com.google.android.gms.games.event.Event;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.Utils;
import xyz.gianlu.pyxoverloaded.model.UserData;

public final class OverloadedUtils {
    public static final String ACTION_OPEN_CHAT = "overloaded_open_chat";
    public static final String ACTION_SEND_CHAT = "overloaded_send_chat";
    public static final String ACTION_SHOW_PROFILE = "overloaded_show_profile";
    public static final String ACTION_ADD_FRIEND = "overloaded_add_friend";
    public static final String ACTION_REMOVE_FRIEND = "overloaded_remove_friend";

    private OverloadedUtils() {
    }

    @NonNull
    public static List<String> toAchievementsIds(@NonNull Collection<Achievement> achievements) {
        List<String> list = new ArrayList<>(achievements.size());
        for (Achievement ach : achievements) list.add(ach.getAchievementId());
        return list;
    }

    @Nullable
    public static Achievement findAchievement(@NonNull Iterable<Achievement> achievements, @NonNull String id) {
        for (Achievement ach : achievements)
            if (ach.getAchievementId().equals(id))
                return ach;

        return null;
    }

    @Nullable
    private static Achievement findBestAchievementOf(@NonNull Iterable<Achievement> achievements, @NonNull String[] progression) {
        Achievement currentBest = null;
        for (Achievement ach : achievements) {
            if (ach.getState() != Achievement.STATE_UNLOCKED || !CommonUtils.contains(progression, ach.getAchievementId()))
                continue;

            if (currentBest == null || ach.getTotalSteps() > currentBest.getTotalSteps())
                currentBest = ach;
        }

        return currentBest;
    }

    @NonNull
    public static List<Achievement> getBestAchievements(@NonNull Iterable<Achievement> achievements) {
        List<Achievement> best = new ArrayList<>(5);
        Achievement ach = findAchievement(achievements, GPGamesHelper.ACH_CUSTOM_DECK);
        if (ach != null && ach.getState() == Achievement.STATE_UNLOCKED) best.add(ach);
        ach = findBestAchievementOf(achievements, GPGamesHelper.ACHS_PEOPLE_GAME);
        if (ach != null) best.add(ach);
        ach = findBestAchievementOf(achievements, GPGamesHelper.ACHS_WIN_ROUNDS);
        if (ach != null) best.add(ach);
        return best;
    }

    @NonNull
    public static List<Achievement> getUnlockedAchievements(@NonNull Iterable<Achievement> achievements) {
        List<Achievement> list = new ArrayList<>(5);
        for (Achievement ach : achievements)
            if (ach.getState() == Achievement.STATE_UNLOCKED)
                list.add(ach);
        return list;
    }


    @Nullable
    public static Event findEvent(@NonNull Iterable<Event> events, @NonNull String id) {
        for (Event ev : events)
            if (ev.getEventId().equals(id))
                return ev;

        return null;
    }

    public static boolean checkUsernameValid(@NonNull String str) {
        return Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{2,29}$").matcher(str).matches();
    }

    /**
     * @return Whether the user is signed in and fully registered.
     * @see OverloadedApi#isFullyRegistered()
     */
    public static boolean isSignedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null && OverloadedApi.get().isFullyRegistered();
    }

    /**
     * @return A task which will resolve to whether the user is signed in and fully registered.
     */
    @NonNull
    public static Task<Boolean> waitReady() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null)
            return Tasks.forResult(false);

        return OverloadedApi.get().userData().continueWith(task -> {
            try {
                UserData data = task.getResult();
                return data != null && data.purchaseStatus.ok;
            } catch (Exception ex) {
                return false;
            }
        });
    }

    @NonNull
    public static String getServerId(@NonNull Pyx.Server server) {
        return server.url.host() + ":" + server.url.port() + server.url.encodedPath();
    }

    @NonNull
    public static String getServeCustomDeckUrl(@NonNull String shareCode) {
        return Utils.overloadedServerUrl("ServeCustomDeck") + "?shareCode=" + shareCode;
    }

    @NonNull
    public static String getCardImageUrl(@NonNull String id) {
        return Utils.overloadedServerUrl("Images/GetCardImage") + "?id=" + id;
    }
}
