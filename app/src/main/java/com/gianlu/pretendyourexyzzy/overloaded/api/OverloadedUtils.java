package com.gianlu.pretendyourexyzzy.overloaded.api;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Pattern;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public final class OverloadedUtils {

    private OverloadedUtils() {
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
        return RequestBody.create(obj.toString().getBytes(), MediaType.get("application/json"));
    }

    static <R> void successfulCallback(@NonNull Task<R> task, @Nullable Activity activity, @NonNull OnSuccessListener<R> listener) {
        if (activity == null) {
            if (listener instanceof Activity)
                task.addOnSuccessListener((Activity) listener, listener);
            else task.addOnSuccessListener(listener);
        } else {
            task.addOnSuccessListener(activity, listener);
        }
    }

    static void failureCallback(@NonNull Task<?> task, @Nullable Activity activity, @NonNull OnFailureListener listener) {
        if (activity == null) {
            if (listener instanceof Activity)
                task.addOnFailureListener((Activity) listener, listener);
            else task.addOnFailureListener(listener);
        } else {
            task.addOnFailureListener(activity, listener);
        }
    }

    static <R> void callbacks(@NonNull Task<R> task, @Nullable Activity activity, @NonNull OnSuccessListener<R> successListener, @NonNull OnFailureListener failureListener) {
        successfulCallback(task, activity, successListener);
        failureCallback(task, activity, failureListener);
    }
}
