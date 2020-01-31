package com.gianlu.pretendyourexyzzy.overloaded;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.logging.Logging;
import com.gianlu.pretendyourexyzzy.main.chats.ChatController;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class OverloadedApi {
    private static OverloadedApi instance = null;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ChatModule chat;
    private FirebaseUser user;

    private OverloadedApi() {
        FirebaseAuth.getInstance().addAuthStateListener(fa -> {
            user = fa.getCurrentUser();
            if (user == null) {
                chat = null;
            } else {
                chat = new ChatModule(user, db);
            }
        });
    }

    @NonNull
    public static OverloadedApi get() {
        if (instance == null) instance = new OverloadedApi();
        return instance;
    }

    private static void log(@NonNull Task<?> task) {
        if (task.getException() == null) return;
        Logging.log("Failed executing task!", task.getException());
    }

    @Nullable
    public ChatModule chat() {
        return chat;
    }

    public void purchaseStatus(@NonNull PurchaseStatusCallback callback) {
        if (user == null) {
            callback.onFailed(new IllegalStateException("No signed in user!"));
            return;
        }

        db.document("users/" + user.getUid()).get().addOnCompleteListener(task -> {
            DocumentSnapshot snap;
            if ((snap = task.getResult()) != null) {
                Purchase.Status status = Purchase.Status.parse((String) snap.get("purchase_status"));
                String token = (String) snap.get("purchase_token");
                callback.onPurchaseStatus(new Purchase(status, token));
            } else if (task.getException() != null) {
                callback.onFailed(task.getException());
            } else {
                callback.onFailed(new IllegalStateException("What's that?"));
            }
        });
    }

    public interface PurchaseStatusCallback {
        void onPurchaseStatus(@NonNull Purchase status);

        void onFailed(@NonNull Exception ex);
    }

    public static final class Purchase {
        public final Status status;
        public final String purchase_token;

        Purchase(@NonNull Status status, @Nullable String purchase_token) {
            this.status = status;
            this.purchase_token = purchase_token;
        }

        public enum Status {
            NONE("none"), OK("ok"); //, PENDING, EXPIRED

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
        }
    }

    public static class ChatModule {
        private final FirebaseUser user;
        private final FirebaseFirestore db;

        private ChatModule(@NonNull FirebaseUser user, @NonNull FirebaseFirestore db) {
            this.user = user;
            this.db = db;
        }

        @Nullable
        private static ChatController.ChatMessage processSnapshot(@NonNull DocumentSnapshot doc) {
            System.out.println(doc);
            System.out.println(doc.getReference().getPath());
            return null; // FIXME: Parse chat message properly
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

        @NonNull
        public Task<ListenerRegistration> addListener(@NonNull ChatController.Listener listener) {
            return getChatIds().continueWith(task -> {
                log(task);

                List<String> list = task.getResult();
                if (list == null) return null;

                return db.collection("chats").whereIn("__name__", list).addSnapshotListener((snap, e) -> {
                    if (snap != null) {
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            ChatController.ChatMessage msg = processSnapshot(doc);
                            if (msg != null) listener.onChatMessage(msg);
                        }
                    } else if (e != null) {
                        Logging.log("Failed receiving snapshot!", e); // FIXME: Listener is now dead
                    }
                });
            });
        }
    }
}
