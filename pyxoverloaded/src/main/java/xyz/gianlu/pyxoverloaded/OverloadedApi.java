package xyz.gianlu.pyxoverloaded;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.misc.NamedThreadFactory;
import com.gianlu.commonutils.preferences.Prefs;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.perf.FirebasePerformance;
import com.google.firebase.perf.metrics.AddTrace;
import com.google.firebase.perf.metrics.Trace;
import com.instacart.library.truetime.TrueTime;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
import xyz.gianlu.pyxoverloaded.model.FriendStatus;
import xyz.gianlu.pyxoverloaded.model.UserData;
import xyz.gianlu.pyxoverloaded.model.UserProfile;
import xyz.gianlu.pyxoverloaded.signal.SignalProtocolHelper;

import static com.gianlu.commonutils.CommonUtils.singletonJsonObject;
import static xyz.gianlu.pyxoverloaded.Utils.overloadedServerUrl;

public class OverloadedApi {
    private final static OverloadedApi instance = new OverloadedApi();
    private static final String TAG = OverloadedApi.class.getSimpleName();
    private static OverloadedChatApi chatInstance;
    final ExecutorService executorService = Executors.newCachedThreadPool(new NamedThreadFactory("overloaded-"));
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("overloaded-scheduler-"));
    private final OkHttpClient client = new OkHttpClient.Builder()
            .readTimeout(5000, TimeUnit.MILLISECONDS)
            .connectTimeout(5000, TimeUnit.MILLISECONDS)
            .addInterceptor(new GzipRequestInterceptor())
            .build();
    private final WebSocketHolder webSocket = new WebSocketHolder();
    private volatile FirebaseUser user;
    private volatile OverloadedToken lastToken;
    private volatile UserData userDataCached = null;
    private volatile Task<UserData> userDataTask = null;
    private volatile Map<String, FriendStatus> friendsStatusCached = null;
    private volatile List<String> serverOverloadedUsersCached = null;
    private Long maintenanceEnd = null;
    private boolean isFirstRequest = true;
    private String clientVersion = "??";

    private OverloadedApi() {
        FirebaseAuth.getInstance().addAuthStateListener(fa -> {
            user = fa.getCurrentUser();
            Log.i(TAG, String.format("Auth state updated! {user: %s}", user));
        });

        executorService.execute(new Runnable() {
            int tries = 0;

            @Override
            public void run() {
                try {
                    TrueTime.build().initialize();
                } catch (IOException ex) {
                    Log.e(TAG, "Failed initializing TrueTime.", ex);
                    if (tries++ < 3) executorService.execute(this);
                }
            }
        });
    }

    public static long now() {
        if (TrueTime.isInitialized()) return TrueTime.now().getTime();
        else return System.currentTimeMillis();
    }

    private static void init(@NonNull Context context) {
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            instance.clientVersion = pi.versionName + "," + pi.versionCode;
        } catch (PackageManager.NameNotFoundException ex) {
            Log.e(TAG, "Failed getting package info.", ex);
        }

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
    public static <R> Task<R> loggingCallbacks(@NonNull Task<R> task, @NonNull String taskName) {
        return task.addOnFailureListener(ex -> Log.d(TAG, String.format("Failed processing task %s!", taskName), ex))
                .addOnSuccessListener(r -> Log.d(TAG, String.format("Task %s completed successfully, result: %s", taskName, r)));
    }

    //region Internal
    @NonNull
    @WorkerThread
    JSONObject makePostRequest(@NonNull String suffix, @Nullable JSONObject json) throws OverloadedException, MaintenanceException {
        return makePostRequest(suffix, json, true);
    }

    @NonNull
    @WorkerThread
    JSONObject makePostRequest(@NonNull String suffix, @Nullable JSONObject json, boolean auth) throws OverloadedException, MaintenanceException {
        RequestBody body;
        if (json == null)
            body = Util.EMPTY_REQUEST;
        else
            body = RequestBody.create(json.toString().getBytes(), MediaType.get("application/json"));

        return makeRequest(suffix, body, auth);
    }

    @NonNull
    @WorkerThread
    private JSONObject makeRequest(@NonNull String suffix, @NonNull RequestBody body, boolean auth) throws OverloadedException, MaintenanceException {
        Trace trace = FirebasePerformance.startTrace("overloaded_request");
        trace.putAttribute("dest", suffix);

        try {
            trace.putAttribute("body_length", String.valueOf(body.contentLength()));
        } catch (IOException ignored) {
        }

        try {
            OverloadedException lastEx = null;
            for (int i = 0; i < 3; i++) {
                Request.Builder req = new Request.Builder()
                        .url(overloadedServerUrl(suffix))
                        .post(body);

                try {
                    req.addHeader("Content-Length", String.valueOf(body.contentLength()));
                    MediaType contentType = body.contentType();
                    if (contentType != null) req.addHeader("Content-Type", contentType.toString());

                    return makeRequestInternal(req, trace, auth);
                } catch (IOException ex) {
                    lastEx = new OverloadedException(req.build().toString(), ex);

                    if (ex instanceof SocketTimeoutException)
                        trace.incrementMetric("tries", 1);
                } catch (OverloadedServerException ex) {
                    lastEx = ex;

                    if (ex.reason.equals(OverloadedServerException.REASON_EXPIRED_TOKEN)) {
                        trace.putAttribute("token_expired", "true");
                        if (lastToken != null) lastToken.updateServerToken(null);
                    } else if (ex.reason.equals(OverloadedServerException.REASON_INVALID_AUTH) && lastToken != null && lastToken.serverToken != null) {
                        trace.putAttribute("token_invalid", "true");
                        if (lastToken != null) lastToken.updateServerToken(null);
                    }
                }
            }

            throw lastEx;
        } finally {
            trace.stop();
        }
    }

    @NonNull
    @WorkerThread
    private JSONObject makeRequestInternal(@NonNull Request.Builder reqBuilder, @NonNull Trace trace, boolean auth) throws OverloadedException, MaintenanceException, IOException {
        if (isFirstRequest || isUnderMaintenance()) {
            for (int i = 0; i < 3; i++) {
                try {
                    Long maintenanceEnd = checkMaintenanceSync();
                    isFirstRequest = false;
                    if (maintenanceEnd != null) throw new MaintenanceException(maintenanceEnd);
                    break;
                } catch (JSONException | IOException ex) {
                    Log.w(TAG, "Failed checking maintenance.", ex);
                }
            }
        }

        if (auth) {
            if (lastToken == null || lastToken.expired()) {
                if (user == null && updateUser())
                    throw new NotSignedInException();

                updateTokenSync();
                if (lastToken == null)
                    throw new NotSignedInException();
            }

            reqBuilder.addHeader("Authorization", lastToken.authHeader());
        }

        trace.putAttribute("auth", String.valueOf(auth));
        trace.putAttribute("has_server_token", String.valueOf(lastToken != null && lastToken.serverToken != null));

        Request req = reqBuilder
                .addHeader("X-Client-Version", clientVersion)
                .addHeader("X-Device-Id", String.valueOf(SignalProtocolHelper.getLocalDeviceId())).build();
        try (Response resp = client.newCall(req).execute()) {
            Log.v(TAG, String.format("%s -> %d", req.url().encodedPath(), resp.code()));

            String serverToken;
            if ((serverToken = resp.header("X-Server-Token", null)) != null && lastToken != null) {
                trace.putAttribute("updated_server_token", "true");
                lastToken.updateServerToken(serverToken);
            } else {
                trace.putAttribute("updated_server_token", "false");
            }

            String serverVersion = resp.header("X-Server-Version");
            if (serverVersion != null) trace.putAttribute("server_version", serverVersion);

            JSONObject obj;
            ResponseBody body = resp.body();
            if (body == null) {
                obj = new JSONObject();
            } else {
                String str = body.string();
                try {
                    if (str.isEmpty()) obj = new JSONObject();
                    else obj = new JSONObject(str);
                } catch (JSONException ex) {
                    throw new IOException(str, ex);
                }
            }

            if (resp.code() < 200 || resp.code() > 299)
                throw OverloadedServerException.create(resp, obj);

            return obj;
        }
    }

    @Nullable
    @WorkerThread
    @AddTrace(name = "overloaded_check_maintenance")
    private Long checkMaintenanceSync() throws JSONException, IOException {
        try (Response resp = client.newCall(new Request.Builder()
                .url(overloadedServerUrl("IsUnderMaintenance"))
                .get().build()).execute()) {
            if (resp.code() != 200) throw new IOException(String.valueOf(resp.code()));

            ResponseBody body = resp.body();
            if (body == null) throw new IOException();

            JSONObject obj = new JSONObject(body.string());
            if (obj.getBoolean("enabled")) maintenanceEnd = obj.getLong("estimatedEnd");
            else maintenanceEnd = null;

            Log.i(TAG, "Updated maintenance status: " + maintenanceEnd);
            return maintenanceEnd;
        }
    }
    //endregion

    //region Pyx

    /**
     * Report that user has logged out from PYX server.
     */
    public void loggedOutFromPyxServer() {
        loggingCallbacks(Tasks.call(executorService, () -> {
            makePostRequest("Pyx/Logout", null);
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

                if (InetAddress.getByName(serverUrl.host()).isSiteLocalAddress())
                    return null;

                JSONObject params = new JSONObject();
                params.put("serverUrl", serverUrl.toString());
                params.put("nickname", nickname);
                makePostRequest("Pyx/Login", params);
                return null;
            }
        }), "logIntoPyx");
    }
    //endregion

    //region User and providers

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
        return !isUnderMaintenance() && userDataCached != null && userDataCached.purchaseStatus.ok;
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
    @AddTrace(name = "overloaded_update_token")
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
    //endregion

    //region Misc

    /**
     * Uploads an image and gets a reference ID in return.
     *
     * @param in The image stream
     * @return A task resolving to the image ID
     */
    @NotNull
    public Task<String> uploadCardImage(@NonNull InputStream in) {
        return Tasks.call(executorService, () -> {
            ByteArrayOutputStream out = new ByteArrayOutputStream(512 * 1024);
            try {
                CommonUtils.copy(in, out);
            } finally {
                in.close();
            }

            String imageEncoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
            JSONObject obj = makePostRequest("Images/UploadCardImage", singletonJsonObject("image", imageEncoded));
            return obj.getString("id");
        });
    }

    /**
     * Links the current profile with another provider.
     *
     * @param credential The credentials of the new provider
     * @return A task resolving to the link operation
     */
    @NonNull
    public Task<Void> link(@NonNull AuthCredential credential) {
        if (user == null && updateUser())
            return Tasks.forException(new NotSignedInException());

        return user.linkWithCredential(credential).continueWithTask(task -> user.reload());
    }

    /**
     * Gets another user profile.
     *
     * @param username The target username
     * @return A task resolving to the {@link UserProfile}
     */
    @NotNull
    public Task<UserProfile> getProfile(@NonNull String username) {
        return Tasks.call(executorService, () -> {
            JSONObject obj = makePostRequest("Profile/Get", singletonJsonObject("username", username));
            return new UserProfile(obj);
        });
    }

    /**
     * Sends the server auth code for Google Play Games to server for linking.
     *
     * @param authCode The server auth code
     */
    public void linkGames(@NonNull String authCode) {
        loggingCallbacks(Tasks.call(executorService, (Callable<Void>) () -> {
            makePostRequest("User/LinkGames", singletonJsonObject("authCode", authCode));
            return null;
        }), "link-play-games");
    }

    /**
     * Registers the user on the server. This can be used to update the purchase token only if the user is already registered.
     *
     * @param username      The username, may be {@code null}
     * @param sku           The product/subscription SKU
     * @param purchaseToken The purchase token
     */
    @NonNull
    @Contract("null, null, null -> fail")
    public Task<UserData> registerUser(@Nullable String username, @Nullable String sku, @Nullable String purchaseToken) {
        if (username == null && (sku == null && purchaseToken == null))
            throw new IllegalStateException();

        if (user == null && updateUser())
            return Tasks.forException(new NotSignedInException());

        return user.getIdToken(true)
                .continueWith(new NonNullContinuation<GetTokenResult, OverloadedToken>() {
                    @Override
                    public OverloadedToken then(@NonNull GetTokenResult result) {
                        return lastToken = OverloadedToken.from(result);
                    }
                })
                .continueWith(executorService, new NonNullContinuation<OverloadedToken, UserData>() {
                    @Override
                    public UserData then(@NonNull OverloadedToken token) throws OverloadedException, JSONException, MaintenanceException {
                        JSONObject body = new JSONObject();
                        if (username != null) body.put("username", username);
                        if (sku != null) body.put("sku", sku);
                        if (purchaseToken != null) body.put("purchaseToken", purchaseToken);

                        JSONObject obj = makePostRequest("User/Register", body);
                        return userDataCached = new UserData(obj.getJSONObject("userData"));
                    }
                });
    }

    /**
     * Checks whether the provided username is unique.
     *
     * @param username The username to check
     * @return A task revolving to whether the username is unique
     */
    @NonNull
    public Task<Boolean> isUsernameUnique(@NonNull String username) {
        return Tasks.call(executorService, () -> {
            try (Response resp = client.newCall(new Request.Builder().url(overloadedServerUrl("IsUsernameUnique"))
                    .post(RequestBody.create(singletonJsonObject("username", username).toString().getBytes(), MediaType.get("application/json"))).build())
                    .execute()) {
                JSONObject obj;
                ResponseBody body = resp.body();
                if (body == null) obj = new JSONObject();
                else obj = new JSONObject(body.string());

                return obj.getBoolean("unique");
            }
        });
    }

    /**
     * Gets all online users on the specified server, everyone can fetch this.
     *
     * @param serverUrl The server URL
     * @return A task resolving to the list of users
     */
    public Task<List<String>> listUsersOnServer(@NonNull HttpUrl serverUrl) {
        return Tasks.call(executorService, () -> {
            JSONObject obj = makePostRequest("Pyx/ListOnline", singletonJsonObject("serverUrl", serverUrl.toString()), false);

            JSONArray array = obj.getJSONArray("users");
            List<String> list = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) list.add(array.getString(i));
            return serverOverloadedUsersCached = list;
        });
    }

    /**
     * Checks if the given user has Overloaded and is on the server (from cache).
     *
     * @param nick The user nickname
     * @return Whether it is an Overloaded user and it is on the same server
     */
    public boolean isOverloadedUserOnServerCached(@NonNull String nick) {
        return serverOverloadedUsersCached != null && serverOverloadedUsersCached.contains(nick);
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

            if (webSocket.client != null) {
                WebSocket client = webSocket.client;
                webSocket.client = null;
                client.cancel();
            }

            webSocket.client = client.newWebSocket(new Request.Builder().get()
                    .header("X-Device-Id", String.valueOf(SignalProtocolHelper.getLocalDeviceId()))
                    .header("Authorization", lastToken.authHeader())
                    .url(overloadedServerUrl("Events")).build(), webSocket);
            return null;
        }), "openWebSocket");
    }

    /**
     * Deletes the current account (from the server too) and signs out.
     *
     * @return A task resolving to the account deletion operation
     */
    @NonNull
    public Task<Void> deleteAccount() {
        return Tasks.call(executorService, (Callable<Void>) () -> {
            makePostRequest("User/Delete", null);
            return null;
        }).addOnFailureListener(ex -> logout());
    }

    /**
     * Logs out the current user and closes the websocket connection.
     */
    public void logout() {
        FirebaseAuth.getInstance().signOut();
        updateUser();

        lastToken = null;
        userDataTask = null;
        userDataCached = null;
        friendsStatusCached = null;
        serverOverloadedUsersCached = null;

        if (webSocket.client != null) {
            webSocket.client.close(1000, null);
            webSocket.client = null;
        }
    }
    //endregion

    //region Maintenance

    /**
     * @return Whether the server is under maintenance (without requesting the server).
     */
    public boolean isUnderMaintenance() {
        return maintenanceEnd != null;
    }

    /**
     * @return When the maintenance SHOULD end.
     */
    public long maintenanceEnd() {
        if (maintenanceEnd == null) throw new IllegalStateException();
        else return maintenanceEnd;
    }
    //endregion

    //region User data
    @NonNull
    public Task<UserData> userData() {
        return userData(false);
    }

    @NonNull
    public Task<UserData> userData(boolean preferCache) {
        if (preferCache && userDataCached != null)
            return Tasks.forResult(userDataCached);

        if (userDataTask != null && !userDataTask.isComplete()) // Reuse task if already running
            return userDataTask;

        return userDataTask = Tasks.call(executorService, () -> {
            JSONObject obj = makePostRequest("User/Data", null);
            return userDataCached = new UserData(obj);
        });
    }

    @Nullable
    public String username() {
        return userDataCached != null ? userDataCached.username : null;
    }

    @Nullable
    public UserData userDataCached() {
        return userDataCached;
    }

    /**
     * Sets an user property to the specified value.
     *
     * @param key   The property key
     * @param value The new property value
     * @return A task resolving to the user property set operation
     */
    @NonNull
    public Task<Void> setUserProperty(@NonNull UserData.PropertyKey key, @Nullable String value) {
        return Tasks.call(executorService, () -> {
            JSONObject body = new JSONObject();
            body.put("key", key.val);
            if (value != null) body.put("value", value);
            makePostRequest("User/SetProperty", body);
            return null;
        });
    }

    /**
     * Removes the user profile image.
     *
     * @return A task resolving to the remove result
     */
    @NotNull
    public Task<Void> removeProfileImage() {
        return Tasks.call(executorService, () -> {
            makePostRequest("Profile/UploadImage", singletonJsonObject("remove", true));
            return null;
        });
    }

    /**
     * Uploads the user profile image.
     *
     * @param in The {@link InputStream} to read the image from
     * @return A task resolving to the upload result
     */
    @NotNull
    public Task<Void> uploadProfileImage(@NotNull InputStream in) {
        return Tasks.call(executorService, () -> {
            ByteArrayOutputStream out = new ByteArrayOutputStream(512 * 1024);
            try {
                CommonUtils.copy(in, out);
            } finally {
                in.close();
            }

            String imageEncoded = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
            makePostRequest("Profile/UploadImage", singletonJsonObject("image", imageEncoded));
            return null;
        });
    }
    //endregion

    //region Friends
    @Nullable
    public Map<String, FriendStatus> friendsStatusCache() {
        return friendsStatusCached;
    }

    public boolean hasFriendCached(@NotNull String username) {
        return friendsStatusCached != null && friendsStatusCached.containsKey(username);
    }

    /**
     * Gets the list of friends and their status (includes friends requests).
     *
     * @return A task resolving to the map of friends
     */
    @NonNull
    public Task<Map<String, FriendStatus>> friendsStatus() {
        return Tasks.call(executorService, () -> {
            JSONObject obj = makePostRequest("User/FriendsStatus", null);
            return friendsStatusCached = FriendStatus.parse(obj);
        });
    }

    /**
     * Removes a friend or denies a friend request.
     *
     * @param username The target username
     * @return A task resolving to the updated friends list
     */
    @NonNull
    public Task<Map<String, FriendStatus>> removeFriend(@NonNull String username) {
        return Tasks.call(executorService, () -> {
            JSONObject obj = makePostRequest("User/RemoveFriend", singletonJsonObject("username", username));

            Map<String, FriendStatus> map = friendsStatusCached = FriendStatus.parse(obj);
            dispatchLocalEvent(Event.Type.REMOVED_FRIEND, username);
            return map;
        });
    }

    /**
     * Adds a friend or makes a friend request.
     *
     * @param username The target username
     * @return A task resolving to the updated friends list
     */
    @NonNull
    public Task<Map<String, FriendStatus>> addFriend(@NonNull String username) {
        return Tasks.call(executorService, () -> {
            JSONObject obj = makePostRequest("User/AddFriend", singletonJsonObject("username", username));

            Map<String, FriendStatus> map = friendsStatusCached = FriendStatus.parse(obj);
            dispatchLocalEvent(Event.Type.ADDED_FRIEND, username);
            return map;
        });
    }
    //endregion

    //region Events
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
        } else if (event.type == OverloadedApi.Event.Type.USER_LEFT_SERVER) {
            if (serverOverloadedUsersCached != null)
                serverOverloadedUsersCached.remove(event.data.getString("nick"));
        } else if (event.type == OverloadedApi.Event.Type.USER_JOINED_SERVER) {
            if (serverOverloadedUsersCached != null)
                serverOverloadedUsersCached.add(event.data.getString("nick"));
        }
    }
    //endregion

    @UiThread
    public interface EventListener {
        void onEvent(@NonNull Event event) throws JSONException;
    }

    public static class MaintenanceException extends Exception {
        public final long maintenanceEnd;

        private MaintenanceException(long maintenanceEnd) {
            super("Estimated end: " + maintenanceEnd);
            this.maintenanceEnd = maintenanceEnd;
        }
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

    public static class OverloadedServerException extends OverloadedException {
        public static final String REASON_MISMATCHED_DEVICES = "mismatchedDevices";
        public static final String REASON_STALE_DEVICES = "staleDevices";
        public static final String REASON_NOT_REGISTERED = "notRegistered";
        public static final String REASON_DEVICE_CONFLICT = "deviceConflict";
        public static final String REASON_DO_NOT_OWN_DECK = "doNotOwnDeck";
        public static final String REASON_NO_SUCH_DECK = "noSuchDeck";
        public static final String REASON_NO_SUCH_USER = "noSuchUser";
        public static final String REASON_EXPIRED_TOKEN = "expiredToken";
        public static final String REASON_INVALID_AUTH = "invalidAuth";
        public static final String REASON_NSFW_DETECTED = "nsfwDetected";
        public final int httpCode;
        public final String reason;
        public final JSONObject details;

        private OverloadedServerException(@NotNull String msg, int httpCode, @NonNull String reason, @Nullable JSONObject details) {
            super(msg);
            this.httpCode = httpCode;
            this.reason = reason;
            this.details = details;
        }

        @NotNull
        @Contract("_, _ -> new")
        @SuppressLint("DefaultLocale")
        private static OverloadedServerException create(@NonNull Response resp, @NonNull JSONObject obj) {
            String reason = obj.optString("reason");
            JSONObject details = obj.optJSONObject("details");
            return new OverloadedServerException(String.format("%s -> %s (%d)", resp.request(), reason, resp.code()), resp.code(), reason, details);
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
        private final String firebaseToken;
        private final long firebaseExpiration;
        private String serverToken;

        private OverloadedToken(@NonNull String firebaseToken, long firebaseExpiration) {
            this.firebaseToken = firebaseToken;
            this.firebaseExpiration = firebaseExpiration;
            this.serverToken = Prefs.getString(OverloadedPK.LAST_SERVER_TOKEN, null);
        }

        @NonNull
        static OverloadedToken from(@NonNull GetTokenResult result) {
            if (result.getToken() == null) throw new IllegalArgumentException();
            return new OverloadedToken(result.getToken(), result.getExpirationTimestamp() * 1000);
        }

        boolean expired() {
            return firebaseExpiration <= now();
        }

        void updateServerToken(@Nullable String token) {
            serverToken = token;
            Prefs.putString(OverloadedPK.LAST_SERVER_TOKEN, token);
        }

        @NonNull
        String authHeader() {
            if (serverToken != null) {
                return "ServerToken " + serverToken;
            } else {
                if (expired()) throw new IllegalStateException();
                return "FirebaseToken " + firebaseToken;
            }
        }
    }

    private static class GzipRequestInterceptor implements Interceptor {
        @NotNull
        @Override
        public Response intercept(@NonNull Chain chain) throws IOException {
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

    private class WebSocketHolder extends WebSocketListener {
        final Set<EventListener> listeners = new HashSet<>();
        private final Handler handler = new Handler(Looper.getMainLooper());
        public WebSocket client;
        private int tries = 0;

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
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            Log.d(TAG, "Opened WebSocket connection.");
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

            tries = 0;

            if (type == Event.Type.PING) {
                webSocket.send("_");
                return;
            }

            try {
                dispatchEvent(new Event(type, data == null ? new JSONObject() : data, null));
            } catch (Exception ex) {
                Log.e(TAG, "Failed dispatching event.", ex);
            }
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @org.jetbrains.annotations.Nullable Response response) {
            if (client == null) return;

            Log.e(TAG, "Failure in WebSocket connection.", t);
            scheduler.schedule(OverloadedApi.this::openWebSocket, (tries++ + 1) * 500, TimeUnit.MILLISECONDS);
        }

        void close() {
            if (client != null) client.close(1000, null);
        }
    }
}
