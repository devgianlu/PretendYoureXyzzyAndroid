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

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.internal.Util;
import okio.BufferedSink;
import okio.GzipSink;
import okio.Okio;
import xyz.gianlu.pyxoverloaded.callback.BooleanCallback;
import xyz.gianlu.pyxoverloaded.callback.FriendsStatusCallback;
import xyz.gianlu.pyxoverloaded.callback.SuccessCallback;
import xyz.gianlu.pyxoverloaded.callback.UserDataCallback;
import xyz.gianlu.pyxoverloaded.callback.UsersCallback;
import xyz.gianlu.pyxoverloaded.model.FriendStatus;
import xyz.gianlu.pyxoverloaded.model.UserData;
import xyz.gianlu.pyxoverloaded.signal.SignalProtocolHelper;

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
    private final OkHttpClient client = new OkHttpClient.Builder().addInterceptor(new GzipRequestInterceptor()).build();
    private final WebSocketHolder webSocket = new WebSocketHolder();
    private FirebaseUser user;
    private volatile OverloadedToken lastToken;
    private volatile UserData userDataCached = null;
    private Task<UserData> userDataTask = null;
    private volatile Map<String, FriendStatus> friendsStatusCached = null;
    private MaintenanceException lastMaintenanceException = null;

    private OverloadedApi() {
        FirebaseAuth.getInstance().addAuthStateListener(fa -> {
            user = fa.getCurrentUser();
            Log.i(TAG, String.format("Auth state updated! {user: %s}", user));
        });
    }

    private static void init(@NonNull Context context) {
        if (chatInstance == null) chatInstance = new OverloadedChatApi(context, instance);
    }

    @Contract(pure = true)
    @NonNull
    public static OverloadedApi get() {
        return instance;
    }

    @NonNull
    public static OverloadedChatApi chat(@NonNull Context context) {
        init(context);
        return chatInstance;
    }

    public static void close() {
        chatInstance.close();
        instance.webSocket.close();
    }

    /////////////////////////////////////////
    //////////////// Internal ///////////////
    /////////////////////////////////////////

    @NonNull
    @WorkerThread
    JSONObject serverRequest(@NonNull Request.Builder reqBuilder) throws OverloadedException {
        return serverRequest(reqBuilder, true);
    }

    @NonNull
    @WorkerThread
    private JSONObject serverRequest(@NonNull Request.Builder reqBuilder, boolean retry) throws OverloadedException {
        if (lastToken == null || lastToken.expired()) {
            if (user == null && updateUser())
                throw new NotSignedInException();

            updateTokenSync();
            if (lastToken == null)
                throw new NotSignedInException();
        }

        Request req = reqBuilder.addHeader("Authorization", "FirebaseToken " + lastToken.token)
                .addHeader("X-Device-Id", String.valueOf(SignalProtocolHelper.getLocalDeviceId())).build();
        try (Response resp = client.newCall(req).execute()) {
            Log.v(TAG, String.format("%s -> %d", req.url().encodedPath(), resp.code()));

            if (resp.code() == 503) {
                String estimatedEnd = resp.header("X-Estimated-End");
                if (estimatedEnd != null)
                    throw lastMaintenanceException = new MaintenanceException(Long.parseLong(estimatedEnd));
            } else if (resp.code() == 409) {
                throw new TwoDevicesException();
            }

            ResponseBody body = resp.body();
            if (body == null) throw new IllegalStateException();

            JSONObject obj;
            String str = body.string();
            if (str.isEmpty()) obj = new JSONObject();
            else obj = new JSONObject(str);

            if (resp.code() < 200 || resp.code() > 299)
                throw OverloadedServerException.forStatusCode(resp, obj);

            return obj;
        } catch (IOException | JSONException ex) {
            if (retry && ex instanceof SocketTimeoutException)
                return serverRequest(reqBuilder, false);
            else
                throw new OverloadedServerException(req, ex);
        }
    }


    /////////////////////////////////////////
    /////////////// Pyx server //////////////
    /////////////////////////////////////////

    /**
     * Report that user has logged out from PYX server.
     */
    public void loggedOutFromPyxServer() {
        loggingCallbacks(Tasks.call(executorService, () -> {
            serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Pyx/Logout"))
                    .post(Util.EMPTY_REQUEST));
            return true;
        }), "logoutFromPyx");
    }

    /**
     * Report that user has logged into a PYX server.
     *
     * @param serverUrl The server URL
     * @param nickname  The nickname used
     * @return The ongoing {@link Task}
     */
    @NonNull
    public Task<Void> loggedIntoPyxServer(@NonNull HttpUrl serverUrl, @NonNull String nickname) {
        return loggingCallbacks(userData(true).continueWith(executorService, new NonNullContinuation<UserData, Void>() {
            @Override
            public Void then(@NonNull UserData userData) throws Exception {
                if (!userData.username.equals(nickname))
                    return null;

                JSONObject params = new JSONObject();
                params.put("serverUrl", serverUrl.toString());
                params.put("nickname", nickname);
                serverRequest(new Request.Builder()
                        .url(overloadedServerUrl("Pyx/Login"))
                        .post(jsonBody(params)));
                return null;
            }
        }), "logIntoPyx");
    }


    /////////////////////////////////////////
    //////////// User & providers ///////////
    /////////////////////////////////////////

    /**
     * @return The current {@link FirebaseUser} instance or {@code null} if not logged in.
     */
    @Nullable
    public FirebaseUser firebaseUser() {
        if (user == null && updateUser()) return null;
        else return user;
    }

    /**
     * @return Whether the use is registered and has payed regularly.
     */
    public boolean isFullyRegistered() {
        return userDataCached != null && userDataCached.purchaseStatus == UserData.PurchaseStatus.OK;
    }

    /**
     * Checks if the current user has this provided linked to his profile.
     *
     * @param id The provider ID
     * @return Whether the provider is linked
     */
    public boolean hasLinkedProvider(@NonNull String id) {
        if (user == null && updateUser())
            return false;

        for (UserInfo info : user.getProviderData()) {
            if (info.getProviderId().equals(id))
                return true;
        }

        return false;
    }

    /**
     * Gets the user info of the given provider.
     *
     * @param id The provider ID
     * @return An {@link UserInfo} for the given provider
     */
    @Nullable
    public UserInfo getProviderUserInfo(@NonNull String id) {
        if (user == null && updateUser())
            return null;

        for (UserInfo info : user.getProviderData())
            if (info.getProviderId().equals(id))
                return info;

        return null;
    }

    /**
     * Updates the Firebase token <b>synchronously</b>.
     */
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

    /**
     * Updates te local {@link FirebaseUser} instance.
     *
     * @return Whether the user is <b>not</b> logged
     */
    private boolean updateUser() {
        user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            return true;
        } else {
            user.reload();
            return false;
        }
    }

    /**
     * Links the current profile with another provider.
     *
     * @param credential The credentials of the new provider
     * @param listener   A listener for completion
     */
    public void link(@NonNull AuthCredential credential, @NonNull OnCompleteListener<Void> listener) {
        if (user == null && updateUser())
            return;

        user.linkWithCredential(credential)
                .continueWithTask(task -> user.reload())
                .addOnCompleteListener(listener);
    }


    /////////////////////////////////////////
    ////////////// Public API ///////////////
    /////////////////////////////////////////

    /**
     * Registers the user on the server. This can be used to update the purchase token only if the user is already registered.
     *
     * @param username      The username, may be {@code null}
     * @param purchaseToken The purchase token
     * @param activity      The caller {@link Activity}
     * @param callback      The callback containing the latest {@link UserData}
     */
    public void registerUser(@Nullable String username, @NonNull String purchaseToken, @Nullable Activity activity, @NonNull UserDataCallback callback) {
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
                        JSONObject body = new JSONObject();
                        if (username != null) body.put("username", username);
                        body.put("purchaseToken", purchaseToken);

                        JSONObject obj = serverRequest(new Request.Builder()
                                .url(overloadedServerUrl("User/Register"))
                                .post(jsonBody(body)));
                        return userDataCached = new UserData(obj.getJSONObject("userData"));
                    }
                });

        callbacks(task, activity, callback::onUserData, callback::onFailed);
    }

    /**
     * Checks whether the provided username is unique.
     *
     * @param username The username to check
     * @param activity The caller {@link Activity}
     * @param callback The callback containing the boolean response
     */
    public void isUsernameUnique(@NonNull String username, @Nullable Activity activity, @NonNull BooleanCallback callback) {
        callbacks(Tasks.call(executorService, () -> {
            JSONObject obj = serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("IsUsernameUnique"))
                    .post(singletonJsonBody("username", username)));
            return obj.getBoolean("unique");
        }), activity, callback::onResult, callback::onFailed);
    }

    /**
     * Gets all online users on the specified server.
     *
     * @param serverUrl The server URL
     * @param activity  The caller {@link Activity}
     * @param callback  The callback containing the list of users
     */
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

    /**
     * Opens/creates the websocket connection with the server.
     */
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
                    .header("X-Device-Id", String.valueOf(SignalProtocolHelper.getLocalDeviceId()))
                    .header("Authorization", "FirebaseToken " + lastToken.token)
                    .url(overloadedServerUrl("Events")).build(), webSocket);
            return null;
        }), "openWebSocket");
    }

    /**
     * Deletes the current account (from the server too) and signs out.
     *
     * @param activity The caller {@link Activity}
     * @param callback The callback for completion
     */
    public void deleteAccount(@Nullable Activity activity, @NonNull SuccessCallback callback) {
        callbacks(Tasks.call(executorService, () -> {
            serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("User/Delete"))
                    .post(Util.EMPTY_REQUEST));
            return null;
        }), activity, a -> {
            logout();
            callback.onSuccessful();
        }, callback::onFailed);
    }

    /**
     * Logs out the current user and closes the websocket connection.
     */
    public void logout() {
        FirebaseAuth.getInstance().signOut();
        updateUser();

        if (webSocket.client != null) {
            webSocket.client.close(1000, null);
            webSocket.client = null;
        }
    }

    /**
     * @return Whether the server is under maintenance.
     */
    public boolean isUnderMaintenance() {
        if (lastMaintenanceException == null) return false;

        if (lastMaintenanceException.estimatedEnd < System.currentTimeMillis()) {
            lastMaintenanceException = null;
            return false;
        }

        return true;
    }


    /////////////////////////////////////////
    /////////////// User data ///////////////
    /////////////////////////////////////////

    public void userData(@Nullable Activity activity, boolean preferCache, @NonNull UserDataCallback callback) {
        callbacks(userData(preferCache), activity, callback::onUserData, callback::onFailed);
    }

    public void userData(@Nullable Activity activity, @NonNull UserDataCallback callback) {
        userData(activity, false, callback);
    }

    @NonNull
    Task<UserData> userData(boolean preferCache) {
        if (preferCache && userDataCached != null)
            return Tasks.forResult(userDataCached);

        if (userDataTask != null && !userDataTask.isComplete()) // Reuse task if already running
            return userDataTask;

        return userDataTask = Tasks.call(executorService, () -> {
            JSONObject obj = serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("User/Data"))
                    .post(Util.EMPTY_REQUEST));

            return userDataCached = new UserData(obj);
        });
    }

    @Nullable
    public UserData userDataCached() {
        return userDataCached;
    }


    /////////////////////////////////////////
    //////////////// Friends ////////////////
    /////////////////////////////////////////

    @Nullable
    public Map<String, FriendStatus> friendsStatusCache() {
        return friendsStatusCached;
    }

    /**
     * Gets the list of friends and their status (includes friends requests).
     *
     * @param activity The caller {@link Activity}
     * @param callback The callback containing the list of friends
     */
    public void friendsStatus(@Nullable Activity activity, @NonNull FriendsStatusCallback callback) {
        callbacks(Tasks.call(executorService, () -> {
            JSONObject obj = serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("User/FriendsStatus"))
                    .post(Util.EMPTY_REQUEST));
            return friendsStatusCached = FriendStatus.parse(obj);
        }), activity, callback::onFriendsStatus, callback::onFailed);
    }

    /**
     * Removes a friend or denies a friend request.
     *
     * @param username The target username
     * @param activity The caller {@link Activity}
     * @param callback The callback containing the status of the removed friend
     */
    public void removeFriend(@NonNull String username, @Nullable Activity activity, @NonNull FriendsStatusCallback callback) {
        callbacks(Tasks.call(executorService, () -> {
            JSONObject obj = serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("User/RemoveFriend"))
                    .post(singletonJsonBody("username", username)));

            Map<String, FriendStatus> map = friendsStatusCached = FriendStatus.parse(obj);
            dispatchLocalEvent(Event.Type.REMOVED_FRIEND, username);
            return map;
        }), activity, callback::onFriendsStatus, callback::onFailed);
    }

    /**
     * Adds a friend or makes a friend request.
     *
     * @param username The target username
     * @param activity The caller {@link Activity}
     * @param callback The callback containing the status of the removed friend
     */
    public void addFriend(@NonNull String username, @Nullable Activity activity, @NonNull FriendsStatusCallback callback) {
        callbacks(Tasks.call(executorService, () -> {
            JSONObject obj = serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("User/AddFriend"))
                    .post(singletonJsonBody("username", username)));

            Map<String, FriendStatus> map = friendsStatusCached = FriendStatus.parse(obj);
            dispatchLocalEvent(Event.Type.ADDED_FRIEND, username);
            return map;
        }), activity, callback::onFriendsStatus, callback::onFailed);
    }


    /////////////////////////////////////////
    //////////////// Events /////////////////
    /////////////////////////////////////////

    void dispatchLocalEvent(@NonNull Event.Type type, @NonNull Object data) {
        webSocket.dispatchEvent(new Event(type, null, data));
    }

    public void addEventListener(@NonNull EventListener listener) {
        webSocket.listeners.add(listener);
    }

    public void removeEventListener(@NonNull EventListener listener) {
        webSocket.listeners.remove(listener);
    }

    @WorkerThread
    private void handleEvent(@NonNull OverloadedApi.Event event) throws JSONException {
        if (event.type == Event.Type.ADDED_AS_FRIEND) {
            if (friendsStatusCached == null) return;

            FriendStatus status = new FriendStatus(event.data);
            if (status.mutual) friendsStatusCached.put(status.username, status);
            else friendsStatusCached.put(status.username, status.asRequest());
        } else if (event.type == Event.Type.REMOVED_AS_FRIEND) {
            if (friendsStatusCached == null) return;

            friendsStatusCached.remove(event.data.getString("username"));
        } else if (event.type == Event.Type.SHARE_KEYS_LOW) {
            if (chatInstance != null) chatInstance.sharePreKeys();
        }
    }

    @UiThread
    public interface EventListener {
        void onEvent(@NonNull Event event) throws JSONException;
    }

    public static class Event {
        public final Type type;
        public final JSONObject data;
        public final Object obj;

        Event(@NonNull Type type, @Nullable JSONObject data, @Nullable Object obj) {
            this.type = type;
            this.data = data;
            this.obj = obj;

            if (obj == null && data == null)
                throw new IllegalStateException();

            if (!type.local && obj != null)
                throw new IllegalStateException();

            if (type.local && data != null)
                throw new IllegalStateException();
        }

        @NonNull
        @Override
        public String toString() {
            return "Event{type=" + type + ", data=" + data + '}';
        }

        public enum Type {
            USER_LEFT_SERVER(false, "uls"), USER_JOINED_SERVER(false, "ujs"), ENCRYPTED_CHAT_MESSAGE(false, "ecm"),
            PING(false, "p"), SHARE_KEYS_LOW(false, "skl"), CHAT_MESSAGE(true, "cm"),
            ADDED_FRIEND(true, "adf"), REMOVED_FRIEND(true, "rmf"), ADDED_AS_FRIEND(false, "adaf"), REMOVED_AS_FRIEND(false, "rmaf");

            private final boolean local;
            private final String code;

            Type(boolean local, @NonNull String code) {
                this.local = local;
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

        @WorkerThread
        void dispatchEvent(@NonNull Event event) {
            Log.v(TAG, event.type + " -> " + (event.data == null ? event.obj : event.data));

            try {
                instance.handleEvent(event);
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
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            JSONObject data;
            Event.Type type;
            try {
                JSONObject obj = new JSONObject(text);
                type = Event.Type.parse(obj.getString("type"));
                data = obj.optJSONObject("data");
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

            dispatchEvent(new Event(type, data == null ? new JSONObject() : data, null));
        }

        void close() {
            if (client != null) client.close(1000, null);
        }
    }

    static class OverloadedException extends Exception {
        OverloadedException() {
        }

        OverloadedException(String message) {
            super(message);
        }

        OverloadedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class MaintenanceException extends OverloadedException {
        public final long estimatedEnd;

        MaintenanceException(long estimatedEnd) {
            this.estimatedEnd = estimatedEnd;
        }

        @NonNull
        public static String messageString(@NonNull Context context, long estimatedEnd) {
            return context.getString(R.string.overloadedStatus_maintenance, new SimpleDateFormat("HH:mm", Locale.getDefault()).format(estimatedEnd));
        }

        @NonNull
        public String messageString(@NonNull Context context) {
            return messageString(context, estimatedEnd);
        }
    }

    public static class TwoDevicesException extends OverloadedException {
        TwoDevicesException() {
        }
    }

    public static class OverloadedServerException extends OverloadedException {
        public final int code;
        public final JSONObject obj;

        private OverloadedServerException(String msg, int code, @NonNull JSONObject obj) {
            super(msg);
            this.code = code;
            this.obj = obj;
        }

        OverloadedServerException(@NonNull Request request, @NonNull Throwable ex) {
            super(request.toString(), ex);
            this.code = -1;
            this.obj = null;
        }

        @SuppressLint("DefaultLocale")
        static OverloadedServerException forStatusCode(@NonNull Response resp, @NonNull JSONObject obj) {
            return new OverloadedServerException(String.format("%s -> %d", resp.request(), resp.code()), resp.code(), obj);
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

        boolean expired() {
            return expiration <= System.currentTimeMillis();
        }
    }

    private static class GzipRequestInterceptor implements Interceptor {
        @NotNull
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            if (originalRequest.body() == null || originalRequest.header("Content-Encoding") != null) {
                return chain.proceed(originalRequest);
            }

            Request compressedRequest = originalRequest.newBuilder()
                    .header("Content-Encoding", "gzip")
                    .method(originalRequest.method(), gzip(originalRequest.body()))
                    .build();
            return chain.proceed(compressedRequest);
        }

        @NotNull
        @Contract("_ -> new")
        private RequestBody gzip(RequestBody body) {
            return new RequestBody() {
                @Override
                public MediaType contentType() {
                    return body.contentType();
                }

                @Override
                public long contentLength() {
                    return -1; // We don't know the compressed length in advance!
                }

                @Override
                public void writeTo(@NotNull BufferedSink sink) throws IOException {
                    BufferedSink gzipSink = Okio.buffer(new GzipSink(sink));
                    body.writeTo(gzipSink);
                    gzipSink.close();
                }
            };
        }
    }
}
