package com.gianlu.pretendyourexyzzy.api.overloaded;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.logging.Logging;
import com.gianlu.pretendyourexyzzy.main.chats.ChatController;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PlayGamesAuthProvider;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OverloadedApi {
    private static final String CLIENT_ID = "596428580538-idv220mduj2clinjoq11sd0n60dgbjr4.apps.googleusercontent.com";
    private static OverloadedApi instance = null;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private ChatModule chat;

    private OverloadedApi() {
    }

    @NonNull
    public static GoogleSignInOptions googleSignInOptions() {
        return new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                .requestServerAuthCode(CLIENT_ID)
                .build();
    }

    public static void authenticateFirebase(@NonNull GoogleSignInAccount account) {
        if (get().auth.getCurrentUser() != null) {
            Logging.log("Already logged in Firebase as " + get().auth.getCurrentUser().getUid(), false);
            return;
        }

        if (account.getServerAuthCode() == null) return;

        AuthCredential credential = PlayGamesAuthProvider.getCredential(account.getServerAuthCode());
        get().auth.signInWithCredential(credential).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                AuthResult result = task.getResult();
                FirebaseUser user;
                if (result != null && (user = result.getUser()) != null) {
                    Logging.log("Successfully logged in Firebase as " + user.getUid(), false);
                    return;
                }
            }

            Logging.log("Failed logging in Firebase!", task.getException());
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
        if (auth.getCurrentUser() == null) {
            chat = null;
            return null;
        }

        if (chat != null && !chat.user.getUid().equals(auth.getCurrentUser().getUid())) {
            chat = null;
            return chat();
        }

        if (chat == null && auth.getCurrentUser() != null)
            chat = new ChatModule(auth.getCurrentUser(), db);

        return chat;
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
            return null; // FIXME
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
