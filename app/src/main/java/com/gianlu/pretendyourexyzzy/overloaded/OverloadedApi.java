package com.gianlu.pretendyourexyzzy.overloaded;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.logging.Logging;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.main.chats.ChatController;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedSignInHelper.SignInProvider;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.RuntimeExecutionException;
import com.google.android.gms.tasks.Task;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OverloadedApi {
    private final static OverloadedApi instance = new OverloadedApi();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ChatModule chat;
    private FirebaseUser user;

    private OverloadedApi() {
        FirebaseAuth.getInstance().addAuthStateListener(fa -> {
            user = fa.getCurrentUser();
            Logging.log(String.format("Auth state updated! {user: %s}", user), false);

            if (user == null) {
                chat = null;
            } else {
                chat = new ChatModule(user, db);
            }
        });
    }

    @NonNull
    public static OverloadedApi get() {
        return instance;
    }

    private static void log(@NonNull Task<?> task) {
        if (task.getException() == null) return;
        Logging.log("Failed executing task!", task.getException());
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

    public void purchaseStatus(@NonNull PurchaseStatusCallback callback) {
        if (user == null && updateUser()) {
            callback.onFailed(new IllegalStateException("No signed in user!"));
            return;
        }

        purchaseStatus().addOnCompleteListener(task -> {
            Purchase purchase;
            if ((purchase = task.getResult()) != null) callback.onPurchaseStatus(purchase);
            else if (task.getException() != null) callback.onFailed(task.getException());
            else throw new IllegalStateException();
        });
    }

    @NonNull
    private Task<Purchase> purchaseStatus() {
        return db.document("users/" + user.getUid()).get().continueWith(task -> {
            DocumentSnapshot snap;
            if ((snap = task.getResult()) != null) {
                Purchase.Status status = Purchase.Status.parse((String) snap.get("purchase_status"));
                String token = (String) snap.get("purchase_token");
                if (token == null) token = "";
                return new Purchase(status, token);
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

    public void verifyPurchase(@NonNull String purchaseToken, @NonNull PurchaseStatusCallback callback) {
        FirebaseFunctions.getInstance()
                .getHttpsCallable("verifyPayment")
                .call(Collections.singletonMap("purchase_token", purchaseToken))
                .continueWithTask(task -> purchaseStatus())
                .addOnCompleteListener(task -> {
                    Purchase purchase;
                    if ((purchase = task.getResult()) != null) callback.onPurchaseStatus(purchase);
                    else if (task.getException() != null) callback.onFailed(task.getException());
                    else throw new IllegalStateException();
                });
    }

    public interface PurchaseStatusCallback {
        void onPurchaseStatus(@NonNull Purchase status);

        void onFailed(@NonNull Exception ex);
    }

    public static final class Purchase {
        public final Status status;
        public final String purchaseToken;

        Purchase(@NonNull Status status, @NonNull String purchaseToken) {
            this.status = status;
            this.purchaseToken = purchaseToken;
        }

        public enum Status {
            NONE("none"), OK("ok"), PENDING("pending");

            private final String val;

            Status(String val) {
                this.val = val;
            }

            @NonNull
            private static Status parse(@Nullable String val) {
                if (val == null) throw new IllegalArgumentException("Can't parse null value.");

                for (Status status : values()) {
                    if (Objects.equals(status.val, val))
                        return status;
                }

                throw new IllegalArgumentException("Unknown status: " + val);
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
        public Task<QuerySnapshot> getAllChats() {
            return getChatIds().continueWithTask(task -> {
                log(task);

                List<String> list = task.getResult();
                if (list == null) return null;

                return db.collection("chats").whereIn("__name__", list).get();
            });
        }

        private static class ChatMessage extends ChatController.ChatMessage {
            private final String chatId;
            private final String msgId;

            ChatMessage(@NonNull String chatId, @NonNull String msgId, @NonNull String sender, @NonNull String text, boolean wall, boolean emote, long timestamp) {
                super(sender, text, wall, emote, timestamp);
                this.chatId = chatId;
                this.msgId = msgId;
            }
        }
    }
}
