package xyz.gianlu.pyxoverloaded;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.auth.UserInfo;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.internal.Util;
import xyz.gianlu.pyxoverloaded.callback.BooleanCallback;
import xyz.gianlu.pyxoverloaded.callback.FriendsStatusCallback;
import xyz.gianlu.pyxoverloaded.callback.UserDataCallback;
import xyz.gianlu.pyxoverloaded.callback.UsersCallback;
import xyz.gianlu.pyxoverloaded.model.FriendStatus;
import xyz.gianlu.pyxoverloaded.model.UserData;

import static xyz.gianlu.pyxoverloaded.TaskUtils.callbacks;
import static xyz.gianlu.pyxoverloaded.TaskUtils.loggingCallbacks;
import static xyz.gianlu.pyxoverloaded.Utils.jsonBody;
import static xyz.gianlu.pyxoverloaded.Utils.overloadedServerUrl;
import static xyz.gianlu.pyxoverloaded.Utils.singletonJsonBody;

public class OverloadedApi {
    private final static OverloadedApi instance = new OverloadedApi();
    private static final String TAG = OverloadedApi.class.getSimpleName();
    private static OverloadedChatApi chatInstance;
    final ExecutorService executorService = Executors.newCachedThreadPool();
    private final OkHttpClient client = new OkHttpClient();
    private final WebSocketHolder webSocket = new WebSocketHolder();
    private FirebaseUser user;
    private volatile OverloadedToken lastToken;
    private volatile UserData userDataCached = null;
    private volatile Map<String, FriendStatus> friendsStatusCached = null;

    private OverloadedApi() {
        FirebaseAuth.getInstance().addAuthStateListener(fa -> {
            user = fa.getCurrentUser();
            Log.i(TAG, String.format("Auth state updated! {user: %s}", user));
        });
    }

    public static void init(@NonNull Context context) {
        chatInstance = new OverloadedChatApi(context, instance);
    }

    @NonNull
    public static OverloadedApi get() {
        return instance;
    }

    @NonNull
    public static OverloadedChatApi chat() {
        return chatInstance;
    }

    public static void close() {
        chatInstance.close();
    }

    public void loggedOutFromPyxServer() {
        loggingCallbacks(Tasks.call(executorService, () -> {
            serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Pyx/Logout"))
                    .post(Util.EMPTY_REQUEST));
            return true;
        }), "logoutFromPyx");
    }

    public void openWebSocket() {
        loggingCallbacks(Tasks.call(executorService, (Callable<Void>) () -> {
            if (lastToken == null || lastToken.expired()) {
                if (user == null && updateUser())
                    throw new NotSignedInException();

                updateTokenSync();
                if (lastToken == null)
                    throw new NotSignedInException();
            }

            webSocket.client = client.newWebSocket(new Request.Builder().get()
                    .header("Authorization", "FirebaseToken " + lastToken.token)
                    .url(overloadedServerUrl("Events")).build(), webSocket);
            return null;
        }), "openWebSocket");
    }

    public void listUsers(@NonNull HttpUrl serverUrl, @Nullable Activity activity, @NonNull UsersCallback callback) {
        callbacks(Tasks.call(executorService, () -> {
            JSONObject obj = serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Pyx/ListOnline"))
                    .post(singletonJsonBody("serverUrl", serverUrl.toString())));

            JSONArray array = obj.getJSONArray("users");
            List<String> list = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) list.add(array.getString(i));
            return list;
        }), activity, callback::onUsers, callback::onFailed);
    }

    private boolean updateUser() {
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return true;
        } else {
            user.reload();
            return false;
        }
    }

    @Nullable
    public FirebaseUser firebaseUser() {
        if (user == null && updateUser()) return null;
        else return user;
    }

    public boolean hasLinkedProvider(@NonNull String id) {
        if (user == null && updateUser())
            return false;

        for (UserInfo info : user.getProviderData()) {
            if (info.getProviderId().equals(id))
                return true;
        }

        return false;
    }

    @Nullable
    public UserInfo getProviderUserInfo(@NonNull String id) {
        if (user == null && updateUser())
            return null;

        for (UserInfo info : user.getProviderData())
            if (info.getProviderId().equals(id))
                return info;

        return null;
    }

    public void link(@NonNull AuthCredential credential, @NonNull OnCompleteListener<Void> listener) {
        if (user == null && updateUser())
            return;

        user.linkWithCredential(credential)
                .continueWithTask(task -> user.reload())
                .addOnCompleteListener(listener);
    }

    @NonNull
    public Task<Void> loggedIntoPyxServer(@NonNull HttpUrl serverUrl, @NonNull String nickname, @Nullable String idCode) {
        return loggingCallbacks(userData(false).continueWith(executorService, new NonNullContinuation<UserData, Void>() {
            @Override
            public Void then(@NonNull UserData userData) throws Exception {
                if (!userData.username.equals(nickname))
                    return null;

                JSONObject params = new JSONObject();
                params.put("serverUrl", serverUrl.toString());
                params.put("nickname", nickname);
                if (idCode != null) params.put("idCode", idCode);
                serverRequest(new Request.Builder()
                        .url(overloadedServerUrl("Pyx/Login"))
                        .post(jsonBody(params)));
                return null;
            }
        }), "logIntoPyx");
    }

    public void userData(@Nullable Activity activity, boolean preferCache, @NonNull UserDataCallback callback) {
        callbacks(userData(preferCache), activity, callback::onUserData, callback::onFailed);
    }

    @WorkerThread
    private void updateTokenSync() {
        if (user == null && updateUser()) throw new IllegalStateException();

        try {
            lastToken = Tasks.await(user.getIdToken(true).continueWith(new NonNullContinuation<GetTokenResult, OverloadedToken>() {
                @Override
                public OverloadedToken then(@NonNull GetTokenResult result) {
                    return OverloadedToken.from(result);
                }
            }));
        } catch (ExecutionException | InterruptedException ex) {
            Log.e(TAG, "Failed updating token.", ex);
            lastToken = null;
        }
    }

    public void userData(@Nullable Activity activity, @NonNull UserDataCallback callback) {
        userData(activity, false, callback);
    }

    @NonNull
    private Task<UserData> userData(boolean preferCache) {
        if (preferCache && userDataCached != null)
            return Tasks.forResult(userDataCached);

        return Tasks.call(executorService, () -> {
            JSONObject obj = serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("User/Data"))
                    .post(Util.EMPTY_REQUEST));

            return userDataCached = new UserData(obj);
        });
    }

    @NonNull
    @WorkerThread
    JSONObject serverRequest(@NonNull Request.Builder reqBuilder) throws OverloadedException {
        if (lastToken == null || lastToken.expired()) {
            if (user == null && updateUser())
                throw new NotSignedInException();

            updateTokenSync();
            if (lastToken == null)
                throw new NotSignedInException();
        }

        Request req = reqBuilder.addHeader("Authorization", "FirebaseToken " + lastToken.token).build();
        try (Response resp = client.newCall(req).execute()) {
            if (resp.code() < 200 || resp.code() > 299)
                throw OverloadedServerException.forStatusCode(resp);

            ResponseBody body = resp.body();
            if (body == null) throw new IllegalStateException();

            String str = body.string();
            if (str.isEmpty()) return new JSONObject();
            else return new JSONObject(str);
        } catch (IOException | JSONException ex) {
            throw new OverloadedServerException(req, ex);
        }
    }

    public void registerUser(@NonNull FirebaseUser user, @Nullable Activity activity, @NonNull UserDataCallback callback) {
        Task<UserData> task = user.getIdToken(true)
                .continueWith(new NonNullContinuation<GetTokenResult, OverloadedToken>() {
                    @Override
                    public OverloadedToken then(@NonNull GetTokenResult result) {
                        return lastToken = OverloadedToken.from(result);
                    }
                })
                .continueWith(executorService, new NonNullContinuation<OverloadedToken, UserData>() {
                    @Override
                    public UserData then(@NonNull OverloadedToken token) throws OverloadedException, JSONException {
                        JSONObject obj = serverRequest(new Request.Builder()
                                .url(overloadedServerUrl("User/Register"))
                                .post(Util.EMPTY_REQUEST));
                        return new UserData(obj.getJSONObject("userData"));
                    }
                });

        callbacks(task, activity, callback::onUserData, callback::onFailed);
    }

    public void verifyPurchase(@NonNull String purchaseToken, @Nullable Activity activity, @NonNull UserDataCallback callback) {
        callbacks(Tasks.call(executorService, () -> {
            JSONObject obj = serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("User/VerifyPurchase"))
                    .post(singletonJsonBody("purchaseToken", purchaseToken)));
            return new UserData(obj.getJSONObject("userData"));
        }), activity, callback::onUserData, callback::onFailed);
    }

    public void isUsernameUnique(@NonNull String username, @Nullable Activity activity, @NonNull BooleanCallback callback) {
        callbacks(Tasks.call(executorService, () -> {
            JSONObject obj = serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("IsUsernameUnique"))
                    .post(singletonJsonBody("username", username)));
            return obj.getBoolean("unique");
        }), activity, callback::onResult, callback::onFailed);
    }

    public void setUsername(@NonNull String username, @Nullable Activity activity, @NonNull UserDataCallback callback) {
        callbacks(Tasks.call(executorService, () -> {
            JSONObject obj = serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("User/SetUsername"))
                    .post(singletonJsonBody("username", username)));
            return new UserData(obj.getJSONObject("userData"));
        }), activity, callback::onUserData, callback::onFailed);
    }

    @Nullable
    public Map<String, FriendStatus> friendsStatusCache() {
        return friendsStatusCached;
    }

    public void friendsStatus(@Nullable Activity activity, @NonNull FriendsStatusCallback callback) {
        callbacks(Tasks.call(executorService, () -> {
            JSONObject obj = serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("User/FriendsStatus"))
                    .post(Util.EMPTY_REQUEST));
            return friendsStatusCached = FriendStatus.parse(obj);
        }), activity, callback::onFriendsStatus, callback::onFailed);
    }

    public void removeFriend(@NotNull String username, @Nullable Activity activity, @NotNull FriendsStatusCallback callback) {
        callbacks(Tasks.call(executorService, () -> {
            JSONObject obj = serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("User/RemoveFriend"))
                    .post(singletonJsonBody("username", username)));
            return friendsStatusCached = FriendStatus.parse(obj);
        }), activity, callback::onFriendsStatus, callback::onFailed);
    }

    public void addFriend(@NotNull String username, @Nullable Activity activity, @NotNull FriendsStatusCallback callback) {
        callbacks(Tasks.call(executorService, () -> {
            JSONObject obj = serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("User/AddFriend"))
                    .post(singletonJsonBody("username", username)));
            return friendsStatusCached = FriendStatus.parse(obj);
        }), activity, callback::onFriendsStatus, callback::onFailed);
    }

    void dispatchLocalEvent(@NonNull Event.Type type, @NonNull JSONObject obj) {
        webSocket.dispatchEvent(new Event(type, obj));
    }

    public void addEventListener(@NonNull EventListener listener) {
        webSocket.listeners.add(listener);
    }

    public void removeEventListener(@NonNull EventListener listener) {
        webSocket.listeners.remove(listener);
    }

    public void logout() {
        if (webSocket.client != null) {
            webSocket.client.close(1000, null);
            webSocket.client = null;
        }

        updateUser();
    }

    @Nullable
    public UserData userDataCached() {
        return userDataCached;
    }

    @UiThread
    public interface EventListener {
        void onEvent(@NonNull Event event) throws JSONException;
    }

    public static class Event {
        public final Type type;
        public final JSONObject obj;

        Event(@NonNull Type type, @NonNull JSONObject obj) {
            this.type = type;
            this.obj = obj;
        }

        @NotNull
        @Override
        public String toString() {
            return "Event{type=" + type + ", obj=" + obj + '}';
        }

        public enum Type {
            USER_LEFT_SERVER("uls"), USER_JOINED_SERVER("ujs"), CHAT_MESSAGE("cm"), PING("p");

            private final String code;

            Type(@NotNull String code) {
                this.code = code;
            }

            @Nullable
            static Type parse(@NonNull String str) {
                for (Type type : values())
                    if (type.code.equals(str))
                        return type;

                return null;
            }
        }
    }

    private static class WebSocketHolder extends WebSocketListener {
        final List<EventListener> listeners = new ArrayList<>();
        private final Handler handler = new Handler(Looper.getMainLooper());
        public WebSocket client;

        void dispatchEvent(@NonNull Event event) {
            try {
                chatInstance.handleEvent(event);
            } catch (JSONException ex) {
                Log.e(TAG, "Failed handling event in worker: " + event, ex);
            }

            for (EventListener listener : new ArrayList<>(listeners)) {
                handler.post(() -> {
                    try {
                        listener.onEvent(event);
                    } catch (JSONException ex) {
                        Log.e(TAG, "Failed handling event: " + event, ex);
                    }
                });
            }
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            JSONObject obj;
            Event.Type type;
            try {
                obj = new JSONObject(text);
                type = Event.Type.parse(obj.getString("type"));
            } catch (JSONException ex) {
                Log.e(TAG, "Failed parsing event: " + text, ex);
                return;
            }

            if (type == null) {
                Log.w(TAG, "Unknown event type: " + text);
                return;
            }

            if (type == Event.Type.PING) {
                webSocket.send("_");
                return;
            }

            dispatchEvent(new Event(type, obj));
        }
    }

    public static class OverloadedException extends Exception {
        OverloadedException() {
        }

        OverloadedException(String message) {
            super(message);
        }

        OverloadedException(String message, Throwable cause) {
            super(message, cause);
        }

        OverloadedException(Throwable cause) {
            super(cause);
        }
    }

    private static class OverloadedServerException extends OverloadedException {
        final int code;

        private OverloadedServerException(String msg, int code) {
            super(msg);
            this.code = code;
        }

        OverloadedServerException(@NonNull Request request, @NonNull Throwable ex) {
            super(request.toString(), ex);

            this.code = -1;
        }

        @SuppressLint("DefaultLocale")
        static OverloadedServerException forStatusCode(@NonNull Response resp) {
            try {
                ResponseBody body = resp.body();
                String bodyStr;
                if (body == null || (bodyStr = body.string()).isEmpty())
                    return new OverloadedServerException(String.format("%s -> %d: %s", resp.request(), resp.code(), resp.message()), resp.code());

                return new OverloadedServerException(String.format("%s -> %d: %s", resp.request(), resp.code(), bodyStr), resp.code());
            } catch (IOException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }

    private static class NotSignedInException extends OverloadedException {
    }

    private abstract static class NonNullContinuation<TResult, TContinuationResult> implements Continuation<TResult, TContinuationResult> {

        @Override
        public final TContinuationResult then(@NonNull Task<TResult> task) throws Exception {
            TResult result = task.getResult();
            if (result == null) throw new IllegalStateException();
            return then(result);
        }

        public abstract TContinuationResult then(@NonNull TResult result) throws Exception;
    }

    private static class OverloadedToken {
        private final String token;
        private final long expiration;

        private OverloadedToken(@NonNull String token, long expiration) {
            this.token = token;
            this.expiration = expiration;
        }

        @NonNull
        static OverloadedToken from(@NonNull GetTokenResult result) {
            if (result.getToken() == null) throw new IllegalArgumentException();
            return new OverloadedToken(result.getToken(), result.getExpirationTimestamp());
        }

        public boolean expired() {
            return expiration <= System.currentTimeMillis();
        }
    }
}
