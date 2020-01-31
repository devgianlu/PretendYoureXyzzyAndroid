package com.gianlu.pretendyourexyzzy.overloaded;

import android.app.Activity;
import android.content.Intent;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.logging.Logging;
import com.gianlu.pretendyourexyzzy.R;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PlayGamesAuthProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class OverloadedSignInHelper {
    public static final List<SignInProvider> SIGN_IN_PROVIDERS;
    private static final String CLIENT_ID = "596428580538-idv220mduj2clinjoq11sd0n60dgbjr4.apps.googleusercontent.com";

    static {
        SIGN_IN_PROVIDERS = new ArrayList<>();
        SIGN_IN_PROVIDERS.add(new SignInProvider(GoogleAuthProvider.PROVIDER_ID, R.drawable.ic_google_auth, R.string.google) {
            @NonNull
            @Override
            GoogleSignInOptions googleSignInOptions() {
                return new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(CLIENT_ID)
                        .requestEmail()
                        .build();
            }

            @Nullable
            @Override
            AuthCredential extractCredential(@NonNull Intent data) {
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    if (account == null) {
                        Logging.log("Failed authenticating with Google!", true);
                        return null;
                    }

                    return GoogleAuthProvider.getCredential(account.getIdToken(), null);
                } catch (ApiException ex) {
                    Logging.log("Failed authenticating with Google!", ex);
                    return null;
                }
            }
        });
        SIGN_IN_PROVIDERS.add(new SignInProvider(PlayGamesAuthProvider.PROVIDER_ID, R.drawable.ic_play_games_auth, R.string.googlePlayGames) {
            @NonNull
            @Override
            GoogleSignInOptions googleSignInOptions() {
                return new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN)
                        .requestServerAuthCode(CLIENT_ID)
                        .build();
            }

            @Nullable
            @Override
            AuthCredential extractCredential(@NonNull Intent data) {
                GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
                if (result.isSuccess()) {
                    String serverCode;
                    GoogleSignInAccount account = result.getSignInAccount();
                    if (account != null && (serverCode = account.getServerAuthCode()) != null)
                        return PlayGamesAuthProvider.getCredential(serverCode);
                }

                Logging.log("Failed authenticating with Google Play Games! " + result.getStatus(), true);
                return null;
            }
        });
    }

    private Flow currentFlow = null;

    public OverloadedSignInHelper() {
    }

    @NonNull
    public static List<String> providerIds() {
        List<String> list = new ArrayList<>(SIGN_IN_PROVIDERS.size());
        for (SignInProvider provider : SIGN_IN_PROVIDERS) list.add(provider.id);
        return Collections.unmodifiableList(list);
    }

    public static boolean isSignedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    @NonNull
    public Intent startFlow(@NonNull Activity activity, @NonNull SignInProvider provider) {
        currentFlow = new Flow();
        currentFlow.provider = provider;
        currentFlow.client = GoogleSignIn.getClient(activity, provider.googleSignInOptions());
        return currentFlow.client.getSignInIntent();
    }

    @Nullable
    public AuthCredential extractCredential(@NonNull Intent data) {
        if (currentFlow == null) throw new IllegalStateException();
        return currentFlow.provider.extractCredential(data);
    }

    public void processSignInData(@NonNull Intent data, @NonNull SignInCallback callback) {
        if (currentFlow == null) throw new IllegalStateException();

        AuthCredential credential = currentFlow.provider.extractCredential(data);
        if (credential == null) {
            callback.onSignInFailed();
            return;
        }

        FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        AuthResult result = task.getResult();
                        FirebaseUser loggedUser;
                        if (result != null && (loggedUser = result.getUser()) != null) {
                            Logging.log("Successfully logged in Firebase as " + loggedUser.getUid(), false);
                            callback.onSignInSuccessful();
                            return;
                        }
                    }

                    Logging.log("Failed logging in Firebase!", task.getException());
                    callback.onSignInFailed();
                });
    }

    public interface SignInCallback {
        void onSignInSuccessful();

        void onSignInFailed();
    }

    public static abstract class SignInProvider {
        public final int iconRes;
        public final int nameRes;
        public final String id;

        SignInProvider(@NonNull String id, @DrawableRes int iconRes, int nameRes) {
            this.iconRes = iconRes;
            this.nameRes = nameRes;
            this.id = id;
        }

        @NonNull
        abstract GoogleSignInOptions googleSignInOptions();

        @Nullable
        abstract AuthCredential extractCredential(@NonNull Intent data);
    }

    private static class Flow {
        SignInProvider provider;
        GoogleSignInClient client;
    }
}
