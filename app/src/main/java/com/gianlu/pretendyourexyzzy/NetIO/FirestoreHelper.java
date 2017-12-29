package com.gianlu.pretendyourexyzzy.NetIO;


import android.support.annotation.NonNull;

import com.gianlu.commonutils.Logging;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FirestoreHelper {
    private static FirestoreHelper instance;
    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private final ExecutorService executorService;

    private FirestoreHelper() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        executorService = Executors.newSingleThreadExecutor();
    }

    public static FirestoreHelper getInstance() {
        if (instance == null) instance = new FirestoreHelper();
        return instance;
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    private FirebaseUser getCurrentUserSync() throws Exception {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            return user;
        } else {
            final Object waitTaskEnd = new Object();
            Task<AuthResult> task = auth.signInAnonymously().addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    synchronized (waitTaskEnd) {
                        waitTaskEnd.notify();
                    }
                }
            });

            synchronized (waitTaskEnd) {
                waitTaskEnd.wait();
            }

            if (task.isSuccessful()) return task.getResult().getUser();
            else throw task.getException();
        }
    }

    public void setNickname(final PYX.Server server, final String nickname) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Map<String, Object> data = new HashMap<>();
                data.put("nickname", nickname);
                data.put("server", server.uri);

                try {
                    FirebaseUser user = getCurrentUserSync();
                    db.document("/users/" + user.getUid())
                            .set(data)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {
                                    if (!task.isSuccessful()) Logging.logMe(task.getException());
                                }
                            });
                } catch (Exception ex) {
                    Logging.logMe(ex);
                }
            }
        });
    }

    public void loggedOut() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    FirebaseUser user = getCurrentUserSync();
                    db.document("/users/" + user.getUid()).delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (!task.isSuccessful()) Logging.logMe(task.getException());
                        }
                    });
                } catch (Exception ex) {
                    Logging.logMe(ex);
                }
            }
        });
    }

    public Task<QuerySnapshot> getMobileNicknames() {
        return db.collection("/users").get();
    }
}
