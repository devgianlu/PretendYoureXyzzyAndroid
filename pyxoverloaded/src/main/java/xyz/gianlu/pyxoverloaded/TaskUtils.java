package xyz.gianlu.pyxoverloaded;

import android.app.Activity;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

public final class TaskUtils {

    private TaskUtils() {
    }

    private static final String TAG = TaskUtils.class.getSimpleName();

    @NonNull
    public static <R> Task<R> loggingCallbacks(@NonNull Task<R> task, @NonNull String taskName) {
        task.addOnFailureListener(ex -> Log.d(TAG, String.format("Failed processing task %s!", taskName), ex))
                .addOnSuccessListener(r -> Log.d(TAG, String.format("Task %s completed successfully, result: %s", taskName, r)));
        return task;
    }

    public static <R> void successfulCallback(@NonNull Task<R> task, @Nullable Activity activity, @NonNull OnSuccessListener<R> listener) {
        if (activity == null) {
            if (listener instanceof Activity)
                task.addOnSuccessListener((Activity) listener, listener);
            else
                task.addOnSuccessListener(listener);
        } else {
            task.addOnSuccessListener(activity, listener);
        }
    }

    public static void failureCallback(@NonNull Task<?> task, @Nullable Activity activity, @NonNull OnFailureListener listener) {
        if (activity == null) {
            if (listener instanceof Activity)
                task.addOnFailureListener((Activity) listener, listener);
            else
                task.addOnFailureListener(listener);
        } else {
            task.addOnFailureListener(activity, listener);
        }
    }

    public static <R> void callbacks(@NonNull Task<R> task, @Nullable Activity activity, @NonNull OnSuccessListener<R> successListener, @NonNull OnFailureListener failureListener) {
        successfulCallback(task, activity, successListener);
        failureCallback(task, activity, failureListener);
    }
}
