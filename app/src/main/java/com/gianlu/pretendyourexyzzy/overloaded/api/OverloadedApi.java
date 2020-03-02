package com.gianlu.pretendyourexyzzy.overloaded.api;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedSignInHelper;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedSignInHelper.SignInProvider;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.internal.Util;

import static com.gianlu.pretendyourexyzzy.overloaded.api.OverloadedUtils.callbacks;
import static com.gianlu.pretendyourexyzzy.overloaded.api.OverloadedUtils.jsonBody;
import static com.gianlu.pretendyourexyzzy.overloaded.api.OverloadedUtils.loggingCallbacks;
import static com.gianlu.pretendyourexyzzy.overloaded.api.OverloadedUtils.overloadedServerUrl;
import static com.gianlu.pretendyourexyzzy.overloaded.api.OverloadedUtils.singletonJsonBody;

public class OverloadedApi {
    private final static OverloadedApi instance = new OverloadedApi();
    private final OkHttpClient client = new OkHttpClient();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final WebSocketHolder webSocket = new WebSocketHolder();
    private FirebaseUser user;
    private volatile OverloadedToken lastToken;
    private volatile UserData userDataCached = null;
    private volatile Map<String, FriendStatus> friendsStatusCached = null;

    private OverloadedApi() {
        FirebaseAuth.getInstance().addAuthStateListener(fa -> {
            user = fa.getCurrentUser();
            Logging.log(String.format("Auth state updated! {user: %s}", user), false);
        });
    }

    @NonNull
    public static OverloadedApi get() {
        return instance;
    }

    public void loggedOutFromPyxServer() {
        OverloadedUtils.loggingCallbacks(Tasks.call(executorService, () -> {
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

    public void listUsers(@NonNull Pyx.Server server, @Nullable Activity activity, @NonNull UsersCallback callback) {
        callbacks(Tasks.call(executorService, () -> {
            JSONObject obj = serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Pyx/ListOnline"))
                    .post(OverloadedUtils.singletonJsonBody("serverUrl", server.url.toString())));

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

    public boolean canLink() {
        if (user == null && updateUser())
            return false;

        List<String> providers = new ArrayList<>(OverloadedSignInHelper.providerIds());
        for (UserInfo info : user.getProviderData()) {
            Iterator<String> iterator = providers.iterator();
            while (iterator.hasNext()) {
                if (Objects.equals(iterator.next(), info.getProviderId()))
                    iterator.remove();
            }
        }

        return providers.size() > 0;
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

    @NonNull
    public List<String> linkableProviderNames(@NonNull Context context) {
        if (user == null && updateUser())
            return Collections.emptyList();

        List<String> names = new ArrayList<>();
        for (SignInProvider provider : OverloadedSignInHelper.SIGN_IN_PROVIDERS) {
            if (!hasLinkedProvider(provider.id))
                names.add(context.getString(provider.nameRes));
        }

        return names;
    }

    @NonNull
    public List<String> linkableProviderIds() {
        if (user == null && updateUser())
            return Collections.emptyList();

        List<String> ids = new ArrayList<>();
        for (SignInProvider provider : OverloadedSignInHelper.SIGN_IN_PROVIDERS) {
            if (!hasLinkedProvider(provider.id))
                ids.add(provider.id);
        }

        return ids;
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
    public Task<Void> loggedIntoPyxServer(@NonNull Pyx.Server server, @NonNull String nickname, @Nullable String idCode) {
        return OverloadedUtils.loggingCallbacks(userData(false).continueWith(executorService, new NonNullContinuation<UserData, Void>() {
            @Override
            public Void then(@NonNull UserData userData) throws Exception {
                if (!userData.username.equals(nickname))
                    return null;

                JSONObject params = new JSONObject();
                params.put("serverUrl", server.url.toString());
                params.put("nickname", nickname);
                if (idCode != null) params.put("idCode", idCode);
                serverRequest(new Request.Builder()
                        .url(overloadedServerUrl("Pyx/Login"))
                        .post(OverloadedUtils.jsonBody(params)));
                return null;
            }
        }), "logIntoPyx");
    }

    public void userData(@Nullable Activity activity, boolean preferCache, @NonNull UserDataCallback callback) {
        OverloadedUtils.callbacks(userData(preferCache), activity, callback::onUserData, callback::onFailed);
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
            Logging.log(ex);
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
    private JSONObject serverRequest(@NonNull Request.Builder reqBuilder) throws OverloadedException {
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
                        try {
                            JSONObject obj = serverRequest(new Request.Builder()
                                    .url(overloadedServerUrl("User/Register"))
                                    .post(Util.EMPTY_REQUEST));
                            return new UserData(obj.getJSONObject("userData"));
                        } catch (OverloadedServerException ex) {
                            if (ex.code == 403) {
                                JSONObject obj = serverRequest(new Request.Builder()
                                        .url(overloadedServerUrl("User/Data"))
                                        .post(Util.EMPTY_REQUEST));
                                return new UserData(obj);
                            } else {
                                throw ex;
                            }
                        }
                    }
                });

        OverloadedUtils.callbacks(task, activity, callback::onUserData, callback::onFailed);
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

    public void startChat(@NonNull String username, @Nullable Activity activity, @NonNull ChatCallback callback) {
        callbacks(Tasks.call(executorService, () -> {
            JSONObject obj = serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/Start"))
                    .post(singletonJsonBody("username", username)));
            return new Chat(obj);
        }), activity, callback::onChat, callback::onFailed);
    }

    public void listChats(@Nullable Activity activity, @NonNull ChatsCallback callback) {
        callbacks(Tasks.call(executorService, () -> {
            JSONObject obj = serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/List"))
                    .post(Util.EMPTY_REQUEST));
            return Chat.parse(obj.getJSONArray("chats"));
        }), activity, callback::onChats, callback::onFailed);
    }

    public void sendMessage(@NonNull String chatId, @NonNull String text, @Nullable Activity activity, @NonNull ChatMessageCallback callback) {
        callbacks(Tasks.call(executorService, () -> {
            JSONObject body = new JSONObject();
            body.put("chatId", chatId);
            body.put("message", text);
            JSONObject obj = serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/Send"))
                    .post(jsonBody(body)));
            return new ChatMessage(obj);
        }), activity, callback::onMessage, callback::onFailed);
    }

    public void getMessages(@NonNull String chatId, @Nullable Activity activity, @NonNull ChatMessagesCallback callback) {
        callbacks(Tasks.call(executorService, () -> {
            JSONObject obj = serverRequest(new Request.Builder()
                    .url(overloadedServerUrl("Chat/Messages"))
                    .post(singletonJsonBody("chatId", chatId)));
            return ChatMessage.parse(obj.getJSONArray("messages"));
        }), activity, callback::onMessages, callback::onFailed);
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

    public static class Chat {
        public final String id;
        public final List<String> participants;
        public ChatMessage lastMsg;

        Chat(@NonNull JSONObject obj) throws JSONException {
            id = obj.getString("id");
            participants = CommonUtils.toStringsList(obj.getJSONArray("participants"), false);
            JSONObject lastMsgObj = obj.optJSONObject("lastMsg");
            if (lastMsgObj != null) lastMsg = new ChatMessage(lastMsgObj);
            else lastMsg = null;
        }

        @NonNull
        public static List<Chat> parse(@NonNull JSONArray array) throws JSONException {
            List<Chat> chats = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) chats.add(new Chat(array.getJSONObject(i)));
            return chats;
        }

        @NonNull
        public String getOtherUsername() {
            OverloadedApi.UserData user = OverloadedApi.get().userDataCached();
            if (user == null) throw new IllegalStateException();
            return participants.get(Objects.equals(participants.get(0), user.username) ? 1 : 0);
        }
    }

    public static class ChatMessage {
        public final String id;
        public final String text;
        public final long timestamp;
        public final String from;

        public ChatMessage(@NonNull JSONObject obj) throws JSONException {
            id = obj.getString("id");
            text = obj.getString("text");
            timestamp = obj.getLong("timestamp");
            from = obj.getString("from");
        }

        @NonNull
        public static List<ChatMessage> parse(@NonNull JSONArray array) throws JSONException {
            List<ChatMessage> chats = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++)
                chats.add(new ChatMessage(array.getJSONObject(i)));
            return chats;
        }
    }

    public static class Event {
        public final Type type;
        public final JSONObject obj;

        Event(@NonNull Type type, @NonNull JSONObject obj) {
            this.type = type;
            this.obj = obj;
        }

        public enum Type {
            USER_LEFT_SERVER("uls"), USER_JOINED_SERVER("ujs"), CHAT_MESSAGE("cm");

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

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            JSONObject obj;
            Event.Type type;
            try {
                obj = new JSONObject(text);
                type = Event.Type.parse(obj.getString("type"));
            } catch (JSONException ex) {
                Logging.log("Failed parsing event: " + text, ex);
                return;
            }

            if (type == null) {
                Logging.log("Unknown event type: " + text, true);
                return;
            }

            Event event = new Event(type, obj);
            for (EventListener listener : new ArrayList<>(listeners)) {
                handler.post(() -> {
                    try {
                        listener.onEvent(event);
                    } catch (JSONException ex) {
                        Logging.log("Failed handling event: " + text, ex);
                    }
                });
            }
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

    public static final class FriendStatus {
        public final String username;
        public final boolean mutual;
        public String serverId;

        FriendStatus(@NotNull String username, @NotNull JSONObject obj) throws JSONException {
            this.username = username;
            mutual = obj.getBoolean("mutual");
            serverId = CommonUtils.optString(obj, "loggedServer");
        }

        @NonNull
        static Map<String, FriendStatus> parse(@NonNull JSONObject obj) throws JSONException {
            Map<String, FriendStatus> map = new HashMap<>();
            Iterator<String> iter = obj.keys();
            while (iter.hasNext()) {
                String username = iter.next();
                map.put(username, new FriendStatus(username, obj.getJSONObject(username)));
            }

            return map;
        }

        @Nullable
        public String server() {
            return serverId;
        }

        public void update(@Nullable String serverId) {
            this.serverId = serverId;
        }
    }

    public static final class UserData {
        public final PurchaseStatus purchaseStatus;
        public final String purchaseToken;
        public final String username;
        public final List<String> friends;

        UserData(@NotNull JSONObject obj) throws JSONException {
            this.username = obj.getString("username");
            this.purchaseStatus = PurchaseStatus.parse(obj.getString("purchaseStatus"));
            this.purchaseToken = obj.getString("purchaseToken");
            this.friends = CommonUtils.toStringsList(obj.getJSONArray("friends"), false);
        }

        @Override
        public String toString() {
            return "UserData{" +
                    "purchaseStatus=" + purchaseStatus +
                    ", purchaseToken='" + purchaseToken + '\'' +
                    ", username='" + username + '\'' +
                    '}';
        }

        public boolean hasUsername() {
            return username != null && !username.isEmpty();
        }

        public enum PurchaseStatus {
            NONE("none"), OK("ok"), PENDING("pending");

            private final String val;

            PurchaseStatus(String val) {
                this.val = val;
            }

            @NonNull
            private static PurchaseStatus parse(@Nullable String val) {
                if (val == null) throw new IllegalArgumentException("Can't parse null value.");

                for (PurchaseStatus status : values()) {
                    if (Objects.equals(status.val, val))
                        return status;
                }

                throw new IllegalArgumentException("Unknown purchaseStatus: " + val);
            }

            @NonNull
            public String toString(@NonNull Context context) {
                int res;
                switch (this) {
                    case NONE:
                        res = R.string.none;
                        break;
                    case OK:
                        res = R.string.ok;
                        break;
                    case PENDING:
                        res = R.string.pending;
                        break;
                    default:
                        res = R.string.unknown;
                        break;
                }

                return context.getString(res);
            }
        }
    }
}
