package com.gianlu.pretendyourexyzzy.overloaded.api;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.pretendyourexyzzy.GPGamesHelper;
import com.google.android.gms.games.achievement.Achievement;
import com.google.android.gms.games.event.Event;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public final class OverloadedUtils {

    private OverloadedUtils() {
    }

    @Nullable
    private static Achievement findAchievement(@NonNull Iterable<Achievement> achievements, @NonNull String id) {
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
        List<Achievement> best = new ArrayList<>(3);
        Achievement ach = findAchievement(achievements, GPGamesHelper.ACH_CARDCAST);
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

    public static boolean isSignedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    @NonNull
    static HttpUrl overloadedServerUrl(@NonNull String path) {
        return HttpUrl.get("http://192.168.1.25:8080/" + path); // FIXME: Testing url
    }

    @NonNull
    static RequestBody singletonJsonBody(@NonNull String key, String value) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put(key, value);
        return jsonBody(obj);
    }

    @NonNull
    static RequestBody jsonBody(@NonNull JSONObject obj) {
        return RequestBody.create(obj.toString().getBytes(), MediaType.get("application/json"));
    }

    @NonNull
    public static <R> Task<R> loggingCallbacks(@NonNull Task<R> task, @NonNull String taskName) {
        task.addOnFailureListener(ex -> Logging.log(String.format("Failed processing task %s!", taskName), ex))
                .addOnSuccessListener(r -> Logging.log(String.format("Task %s completed successfully, result: %s", taskName, String.valueOf(r)), false));
        return task;
    }

    public static <R> void successfulCallback(@NonNull Task<R> task, @Nullable Activity activity, @NonNull OnSuccessListener<R> listener) {
        if (activity == null) {
            if (listener instanceof Activity)
                task.addOnSuccessListener((Activity) listener, listener);
            else task.addOnSuccessListener(listener);
        } else {
            task.addOnSuccessListener(activity, listener);
        }
    }

    public static void failureCallback(@NonNull Task<?> task, @Nullable Activity activity, @NonNull OnFailureListener listener) {
        if (activity == null) {
            if (listener instanceof Activity)
                task.addOnFailureListener((Activity) listener, listener);
            else task.addOnFailureListener(listener);
        } else {
            task.addOnFailureListener(activity, listener);
        }
    }

    public static <R> void callbacks(@NonNull Task<R> task, @Nullable Activity activity, @NonNull OnSuccessListener<R> successListener, @NonNull OnFailureListener failureListener) {
        successfulCallback(task, activity, successListener);
        failureCallback(task, activity, failureListener);
    }
}
