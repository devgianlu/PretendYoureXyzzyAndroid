package xyz.gianlu.pyxoverloaded;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.preferences.Prefs;
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
import org.whispersystems.libsignal.state.PreKeyRecord;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

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
import xyz.gianlu.pyxoverloaded.callback.SuccessCallback;
import xyz.gianlu.pyxoverloaded.callback.UserDataCallback;
import xyz.gianlu.pyxoverloaded.callback.UsersCallback;
import xyz.gianlu.pyxoverloaded.model.FriendStatus;
import xyz.gianlu.pyxoverloaded.model.UserData;
import xyz.gianlu.pyxoverloaded.signal.DbSignalStore;
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

    @NonNull
    private static String getDeviceId() {
        String id = Prefs.getString(OverloadedPK.OVERLOADED_DEVICE_ID, null);
        if (id == null) {
            id = CommonUtils.randomString(8, ThreadLocalRandom.current(), "abcdefghijklmnopqrstuvwxyz1234567890");
            Prefs.putString(OverloadedPK.OVERLOADED_DEVICE_ID, id);
        }

        return id;
    }

    void sharePreKeys() {
        loggingCallbacks(Tasks.call(executorService, () -> {
            JSONObject body = new JSONObject();
            body.put("registrationId", DbSignalStore.get().getLocalRegistrationId());
            body.put("deviceId", SignalProtocolHelper.getLocalDeviceId());
            body.put("identityKey", Base64.encodeToString(DbSignalStore.get().getIdentityKeyPair().getPublicKey().serialize(), Base64.NO_WRAP));
            body.put("signedPreKey", Utils.toServerJson(SignalProtocolHelper.getLocalSignedPreKey()));

            JSONArray preKeysArray = new JSONArray();
            List<PreKeyRecord> preKeys = SignalProtocolHelper.generateSomePreKeys();
            for (PreKeyRecord key : preKeys) preKeysArray.put(Utils.toServerJson(key));
            body.put("preKeys", preKeysArray);

            serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/ShareKeys"))
                    .post(jsonBody(body)));
            return null;
        }), "share-pre-keys");
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
                    .header("X-Device-Id", getDeviceId())
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
    Task<UserData> userData(boolean preferCache) {
        if (preferCache && userDataCached != null)
            return Tasks.forResult(userDataCached);

        return Tasks.call(executorService, () -> {
            JSONObject obj = serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("User/Data"))
                    .post(Util.EMPTY_REQUEST));

            return userDataCached = new UserData(obj);
        });
    }

    public boolean isFullyRegistered() {
        return userDataCached != null && userDataCached.hasUsername() && userDataCached.purchaseStatus == UserData.PurchaseStatus.OK;
    }

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
                .addHeader("X-Device-Id", getDeviceId()).build();
        try (Response resp = client.newCall(req).execute()) {
            Log.v(TAG, String.format("%s -> %d", req.url().encodedPath(), resp.code()));

            if (resp.code() == 503) {
                String estimatedEnd = resp.header("X-Estimated-End");
                if (estimatedEnd != null)
                    throw new MaintenanceException(Long.parseLong(estimatedEnd));
            } else if (resp.code() == 409) {
                throw new TwoDevicesException();
            }

            if (resp.code() < 200 || resp.code() > 299)
                throw OverloadedServerException.forStatusCode(resp);

            ResponseBody body = resp.body();
            if (body == null) throw new IllegalStateException();

            String str = body.string();
            if (str.isEmpty()) return new JSONObject();
            else return new JSONObject(str);
        } catch (IOException | JSONException ex) {
            if (retry && ex instanceof SocketTimeoutException)
                return serverRequest(reqBuilder, false);
            else
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
                        return userDataCached = new UserData(obj.getJSONObject("userData"));
                    }
                });

        callbacks(task, activity, callback::onUserData, callback::onFailed);
    }

    public void verifyPurchase(@NonNull String purchaseToken, @Nullable Activity activity, @NonNull UserDataCallback callback) {
        callbacks(Tasks.call(executorService, () -> {
            JSONObject obj = serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("User/VerifyPurchase"))
                    .post(singletonJsonBody("purchaseToken", purchaseToken)));
            return userDataCached = new UserData(obj.getJSONObject("userData"));
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
            return userDataCached = new UserData(obj.getJSONObject("userData"));
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

            dispatchLocalEvent(Event.Type.REMOVED_FRIEND, CommonUtils.singletonJsonObject("username", username));
            return friendsStatusCached = FriendStatus.parse(obj);
        }), activity, callback::onFriendsStatus, callback::onFailed);
    }

    public void addFriend(@NotNull String username, @Nullable Activity activity, @NotNull FriendsStatusCallback callback) {
        callbacks(Tasks.call(executorService, () -> {
            JSONObject obj = serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("User/AddFriend"))
                    .post(singletonJsonBody("username", username)));

            Map<String, FriendStatus> map = FriendStatus.parse(obj);
            FriendStatus status = map.get(username);
            if (status != null) dispatchLocalEvent(Event.Type.ADDED_FRIEND, status.toJSON());
            return friendsStatusCached = map;
        }), activity, callback::onFriendsStatus, callback::onFailed);
    }

    public void deleteAccount(@Nullable Activity activity, @NotNull SuccessCallback callback) {
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

    void dispatchLocalEvent(@NonNull Event.Type type, @NonNull JSONObject data) {
        webSocket.dispatchEvent(new Event(type, data));
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

    @WorkerThread
    private void handleEvent(@NonNull OverloadedApi.Event event) throws JSONException {
        if (event.type == Event.Type.ADDED_AS_FRIEND) {
            if (friendsStatusCached == null) return;

            FriendStatus status = new FriendStatus(event.data);
            for (String username : new ArrayList<>(friendsStatusCached.keySet())) {
                if (Objects.equals(status.username, username)) {
                    friendsStatusCached.put(username, status);
                    break;
                }
            }
        } else if (event.type == Event.Type.REMOVED_AS_FRIEND) {
            if (friendsStatusCached == null) return;

            String removedUsername = event.data.getString("username");
            for (String username : new ArrayList<>(friendsStatusCached.keySet())) {
                if (Objects.equals(removedUsername, username)) {
                    FriendStatus status = friendsStatusCached.get(username);
                    if (status != null) friendsStatusCached.put(username, status.notMutual());
                    break;
                }
            }
        } else if (event.type == Event.Type.SHARE_KEYS_LOW) {
            if (chatInstance != null) sharePreKeys();
        }
    }

    @UiThread
    public interface EventListener {
        void onEvent(@NonNull Event event) throws JSONException;
    }

    public static class Event {
        public final Type type;
        public final JSONObject data;

        Event(@NonNull Type type, @NonNull JSONObject data) {
            this.type = type;
            this.data = data;
        }

        @NotNull
        @Override
        public String toString() {
            return "Event{type=" + type + ", data=" + data + '}';
        }

        public enum Type {
            USER_LEFT_SERVER("uls"), USER_JOINED_SERVER("ujs"), ENCRYPTED_CHAT_MESSAGE("ecm"),
            PING("p"), SHARE_KEYS_LOW("skl"), CHAT_MESSAGE("cm"),
            ADDED_FRIEND("adf"), REMOVED_FRIEND("rmf"), ADDED_AS_FRIEND("adaf"), REMOVED_AS_FRIEND("rmaf");

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

        @WorkerThread
        void dispatchEvent(@NonNull Event event) {
            Log.v(TAG, event.type + " -> " + event.data.toString());

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
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
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

            dispatchEvent(new Event(type, data == null ? new JSONObject() : data));
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
    }

    public static class TwoDevicesException extends OverloadedException {
        TwoDevicesException() {
        }
    }

    public static class OverloadedServerException extends OverloadedException {
        public final int code;

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

        boolean expired() {
            return expiration <= System.currentTimeMillis();
        }
    }
}
