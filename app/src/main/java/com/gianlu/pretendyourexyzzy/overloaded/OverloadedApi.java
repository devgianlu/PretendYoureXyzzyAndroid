package com.gianlu.pretendyourexyzzy.overloaded;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.logging.Logging;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.models.User;
import com.gianlu.pretendyourexyzzy.main.chats.ChatController;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedSignInHelper.SignInProvider;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.RuntimeExecutionException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.functions.FirebaseFunctions;
import com.google.firebase.functions.HttpsCallableResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OverloadedApi {
    private final static OverloadedApi instance = new OverloadedApi();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ChatModule chat;
    private FirebaseUser user;
    private UserData latestUserData;

    private OverloadedApi() {
        FirebaseAuth.getInstance().addAuthStateListener(fa -> {
            user = fa.getCurrentUser();
            Logging.log(String.format("Auth state updated! {user: %s}", user), false);

            chat = null;
        });
    }

    public static boolean checkUsernameValid(@NonNull String str) {
        return Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{2,29}$").matcher(str).matches();
    }

    @NonNull
    public static OverloadedApi get() {
        return instance;
    }

    private static void log(@NonNull Task<?> task) {
        if (task.getException() == null) return;
        Logging.log("Failed executing task!", task.getException());
    }

    @NonNull
    public static Task<Boolean> isUsernameUnique(@NonNull String username) {
        return FirebaseFunctions.getInstance()
                .getHttpsCallable("checkUsername")
                .call(Collections.singletonMap("username", username))
                .continueWith(task -> {
                    HttpsCallableResult result = task.getResult();
                    if (result == null) return false;

                    // noinspection unchecked
                    Map<String, Object> map = (Map<String, Object>) result.getData();
                    if (map == null) return false;

                    return Boolean.parseBoolean(String.valueOf(map.get("unique")));
                });
    }

    @NonNull
    private static String serverUrlIdentifier(@NonNull Pyx.Server server) {
        return server.url.host() + ":" + server.url.port() + server.url.encodedPath();
    }

    public void loggedIntoPyxServer(@NonNull User user, @NonNull Pyx.Server server) {
        Map<String, String> map = new HashMap<>();
        map.put("nickname", user.nickname);
        map.put("idCode", user.idCode == null ? "" : user.idCode);
        map.put("serverUrl", serverUrlIdentifier(server));
        FirebaseFunctions.getInstance()
                .getHttpsCallable("pyxServerLoggedIn")
                .call(map).addOnCompleteListener(OverloadedApi::log);
    }

    public void loggedOutOfPyxServer(@NonNull Pyx.Server server) { // FIXME: We will not receive this call if the user closes the app
        FirebaseFunctions.getInstance()
                .getHttpsCallable("pyxServerLoggedOut")
                .call(Collections.singletonMap("serverUrl", serverUrlIdentifier(server)))
                .addOnCompleteListener(OverloadedApi::log);
    }

    @NonNull
    public Task<List<String>> listUsers(@NonNull Pyx.Server server) {
        return FirebaseFunctions.getInstance()
                .getHttpsCallable("listUsersForServer")
                .call(Collections.singletonMap("serverUrl", serverUrlIdentifier(server)))
                .continueWith(task -> {
                    OverloadedApi.log(task);

                    HttpsCallableResult result = task.getResult();
                    if (result == null) return Collections.emptyList();

                    // noinspection unchecked
                    List<String> list = (List<String>) result.getData();
                    if (list == null) return Collections.emptyList();
                    else return list;
                });
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
    public ChatModule chat() {
        if (user == null && updateUser())
            return null;

        if (chat == null) chat = new ChatModule(user, db);
        return chat;
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

    private boolean hasLinkedProvider(@NonNull String id) {
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

    public void userData(@NonNull UserDataCallback callback) {
        if (user == null && updateUser()) {
            callback.onFailed(new IllegalStateException("No signed in user!"));
            return;
        }

        userData().addOnCompleteListener(task -> {
            UserData userData;
            if (task.getException() != null) callback.onFailed(task.getException());
            else if ((userData = task.getResult()) != null) callback.onUserData(userData);
            else throw new IllegalStateException();
        });
    }

    @Nullable
    public UserData userDataCache() {
        return latestUserData;
    }

    @NonNull
    private Task<UserData> userData() {
        return db.document("users/" + user.getUid()).get().continueWith(task -> {
            DocumentSnapshot snap;
            if ((snap = task.getResult()) != null) {
                UserData.PurchaseStatus status = UserData.PurchaseStatus.parse((String) snap.get("purchase_status"));
                String token = (String) snap.get("purchase_token");
                if (token == null) token = "";
                return latestUserData = new UserData(snap.getString("username"), status, token);
            } else if (task.getException() != null) {
                throw new RuntimeExecutionException(task.getException());
            } else {
                throw new IllegalStateException();
            }
        });
    }

    public void link(@NonNull AuthCredential credential, @NonNull OnCompleteListener<Void> listener) {
        if (user == null && updateUser())
            return;

        user.linkWithCredential(credential)
                .continueWithTask(task -> user.reload())
                .addOnCompleteListener(listener);
    }

    public void verifyPurchase(@NonNull String purchaseToken, @NonNull UserDataCallback callback) {
        FirebaseFunctions.getInstance()
                .getHttpsCallable("verifyPayment")
                .call(Collections.singletonMap("purchase_token", purchaseToken))
                .continueWithTask(task -> userData())
                .addOnCompleteListener(task -> {
                    UserData userData;
                    if (task.getException() != null) callback.onFailed(task.getException());
                    else if ((userData = task.getResult()) != null)
                        callback.onUserData(userData);
                    else throw new IllegalStateException();
                });
    }

    public void setUsername(@NonNull String username, @NonNull UserDataCallback callback) {
        FirebaseFunctions.getInstance()
                .getHttpsCallable("setUsername")
                .call(Collections.singletonMap("username", username))
                .continueWithTask(task -> {
                    task.getResult();
                    return userData();
                })
                .addOnCompleteListener(task -> {
                    UserData userData;
                    if (task.getException() != null) callback.onFailed(task.getException());
                    else if ((userData = task.getResult()) != null) callback.onUserData(userData);
                    else throw new IllegalStateException();
                });
    }

    public interface UserDataCallback {
        void onUserData(@NonNull UserData status);

        void onFailed(@NonNull Exception ex);
    }

    public static final class UserData {
        public final PurchaseStatus purchaseStatus;
        public final String purchaseToken;
        public final String username;

        UserData(@Nullable String username, @NonNull PurchaseStatus purchaseStatus, @NonNull String purchaseToken) {
            this.username = username;
            this.purchaseStatus = purchaseStatus;
            this.purchaseToken = purchaseToken;
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

    public static class ChatModule {
        private static final Pattern CHAT_MESSAGE_PATH_PATTERN = Pattern.compile("chats/(.*)/thread/(.*)");
        private final FirebaseUser user;
        private final FirebaseFirestore db;

        private ChatModule(@NonNull FirebaseUser user, @NonNull FirebaseFirestore db) {
            this.user = user;
            this.db = db;
        }

        @Nullable
        private static ChatController.ChatMessage processSnapshot(@NonNull DocumentSnapshot doc) {
            Matcher matcher = CHAT_MESSAGE_PATH_PATTERN.matcher(doc.getReference().getPath());
            if (!matcher.find()) return null;

            String chatId = matcher.group(1);
            if (chatId == null || chatId.isEmpty()) return null;

            String msgId = matcher.group(2);
            if (msgId == null || msgId.isEmpty()) return null;

            String sender = doc.getString("sender");
            if (sender == null) return null;

            String text = doc.getString("text");
            if (text == null) return null;

            Timestamp ts = doc.getTimestamp("timestamp");
            if (ts == null) return null;

            return new ChatMessage(chatId, msgId, sender, text, false, false, ts.toDate().getTime());
        }

        @NonNull
        public Task<List<ListenerRegistration>> addListener(@NonNull ChatController.Listener listener) {
            return getChatIds().continueWith(task -> {
                log(task);

                List<String> ids = task.getResult();
                if (ids == null) return null;

                EventListener<QuerySnapshot> snapListener = (snap, e) -> {
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            ChatController.ChatMessage msg = processSnapshot(doc);
                            if (msg != null) listener.onChatMessage(msg);
                        }
                    } else if (e != null) {
                        Logging.log("Failed receiving snapshot!", e);
                    }
                };

                List<ListenerRegistration> registrations = new ArrayList<>(ids.size());
                for (String id : ids)
                    registrations.add(db.collection("chats/" + id + "/thread")
                            .addSnapshotListener(snapListener));

                return registrations;
            });
        }

        @NonNull
        private Task<List<String>> getChatIds() {
            return db.document("users/" + user.getUid()).get().continueWith(task -> {
                log(task);

                DocumentSnapshot snap = task.getResult();
                if (snap == null) return Collections.emptyList();

                List list = (List) snap.get("chats");
                if (list == null) return Collections.emptyList();

                // noinspection unchecked
                return new ArrayList<String>(list);
            });
        }

        @NonNull
        public Task<List<Map<String, Object>>> getAllChats() {
            return getChatIds().continueWithTask(task -> {
                log(task);

                List<String> list = task.getResult();
                if (list == null || list.isEmpty()) return Tasks.forResult(null);

                return db.collection("chats").whereIn("__name__", list).get();
            }).continueWith(task -> {
                log(task);

                QuerySnapshot snap = task.getResult();
                if (snap == null) return Collections.emptyList();

                List<Map<String, Object>> list = new ArrayList<>();
                for (DocumentSnapshot doc : snap.getDocuments())
                    list.add(doc.getData());

                return list;
            });
        }

        @NonNull
        public Task<Void> send(@NonNull String chatId, @NonNull String msg) { // TODO: We should probably do some server side validation
            Map<String, Object> data = new HashMap<>();
            data.put("sender", user.getUid());
            data.put("text", msg);
            data.put("timestamp", Timestamp.now());
            return db.collection("chats/" + chatId + "/thread").document().set(data);
        }

        @NonNull
        public String uid() {
            return user.getUid();
        }

        public void startChatWith(@NonNull String uid) {
            db.collection("chats").document().set(Collections.singletonMap("users", Arrays.asList(uid, user.getUid()))).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    log(task);
                    System.out.println("HEIIIIIIIIIIIIIIIIIIIIIIIIIII");
                }
            });
        }

        private static class ChatMessage extends ChatController.ChatMessage {
            private final String chatId;
            private final String msgId;

            ChatMessage(@NonNull String chatId, @NonNull String msgId, @NonNull String sender, @NonNull String text,
                        boolean wall, boolean emote, long timestamp) {
                super(sender, text, wall, emote, timestamp);
                this.chatId = chatId;
                this.msgId = msgId;
            }
        }
    }
}
