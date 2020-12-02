package com.gianlu.pretendyourexyzzy;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.pretendyourexyzzy.api.FirstLoadedPyx;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxDiscoveryApi;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.FirstLoad;
import com.gianlu.pretendyourexyzzy.databinding.ActivityNewMainBinding;
import com.gianlu.pretendyourexyzzy.game.GameActivity;
import com.gianlu.pretendyourexyzzy.main.NewGamesFragment;
import com.gianlu.pretendyourexyzzy.main.NewProfileFragment;
import com.gianlu.pretendyourexyzzy.main.NewSettingsFragment;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedSignInHelper;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;
import com.gianlu.pretendyourexyzzy.overloaded.SyncUtils;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.PlayGamesAuthProvider;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import xyz.gianlu.pyxoverloaded.OverloadedApi;

public class NewMainActivity extends ActivityWithDialog {
    private static final String SETTINGS_FRAGMENT_TAG = "settings";
    private static final String GAMES_FRAGMENT_TAG = "games";
    private static final String PROFILE_FRAGMENT_TAG = "profile";
    private static final String TAG = NewMainActivity.class.getSimpleName();
    private ActivityNewMainBinding binding;
    private NewSettingsFragment settingsFragment;
    private NewGamesFragment gamesFragment;
    private NewProfileFragment profileFragment;
    private RegisteredPyx pyx;
    private Task<RegisteredPyx> prepareTask;

    @Override
    protected void onStart() {
        super.onStart();
        GPGamesHelper.setPopupView(this, Gravity.TOP);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Prefs.getBoolean(PK.ONE_TIME_LOGIN_SHOWN) && Prefs.getString(PK.LAST_NICKNAME, null) == null) {
            startActivity(new Intent(this, OneTimeLoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            return;
        }

        binding = ActivityNewMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getSupportFragmentManager().beginTransaction()
                .add(R.id.main_container, settingsFragment = NewSettingsFragment.get(), SETTINGS_FRAGMENT_TAG)
                .add(R.id.main_container, gamesFragment = NewGamesFragment.get(), GAMES_FRAGMENT_TAG)
                .add(R.id.main_container, profileFragment = NewProfileFragment.get(), PROFILE_FRAGMENT_TAG)
                .commitNow();

        binding.mainNavigation.setOnNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.mainNavigation_settings) {
                checkReloadNeeded();
                getSupportFragmentManager().beginTransaction()
                        .hide(gamesFragment).hide(profileFragment)
                        .show(settingsFragment)
                        .commitNow();
                return true;
            } else if (item.getItemId() == R.id.mainNavigation_home) {
                checkReloadNeeded();
                getSupportFragmentManager().beginTransaction()
                        .hide(settingsFragment).hide(profileFragment)
                        .show(gamesFragment)
                        .commitNow();
                return true;
            } else if (item.getItemId() == R.id.mainNavigation_profile) {
                getSupportFragmentManager().beginTransaction()
                        .hide(gamesFragment).hide(settingsFragment)
                        .show(profileFragment)
                        .runOnCommit(() -> profileFragment.onResume())
                        .commitNow();
                return true;
            } else {
                return false;
            }
        });
        binding.mainNavigation.setSelectedItemId(R.id.mainNavigation_home);

        preparePyxInstance()
                .addOnSuccessListener(this::pyxReady)
                .addOnFailureListener(this::pyxError);

        OverloadedUtils.waitReady()
                .addOnSuccessListener(signedId -> {
                    if (!signedId) return;

                    OverloadedUtils.doInitChecks(this);

                    SyncUtils.syncStarredCards(this, null);
                    SyncUtils.syncCustomDecks(this, null);
                    SyncUtils.syncStarredCustomDecks(this, null);

                    OverloadedSignInHelper.signInSilently(this, PlayGamesAuthProvider.PROVIDER_ID)
                            .addOnSuccessListener(account -> {
                                String authCode = account.getServerAuthCode();
                                if (authCode != null) OverloadedApi.get().linkGames(authCode);
                            });
                });
    }

    public void checkReloadNeeded() {
        if (profileFragment == null || prepareTask == null || !(prepareTask.isComplete() || prepareTask.isCanceled()))
            return;

        String newUsername = profileFragment.getUsername();
        String newIdCode = profileFragment.getIdCode();
        if (pyx != null && newUsername.equals(pyx.user().nickname) && Objects.equals(Prefs.getString(PK.LAST_ID_CODE, null), newIdCode))
            return;

        if (pyx != null) pyx.logout();
        preparePyxInstance(newUsername, newIdCode)
                .addOnSuccessListener(this, this::pyxReady)
                .addOnFailureListener(this, this::pyxError);
    }

    public void changeServer(@NotNull Pyx.Server server) {
        if (pyx != null) pyx.logout();

        Pyx.Server.setLastServer(server);
        preparePyxInstance()
                .addOnSuccessListener(this, this::pyxReady)
                .addOnFailureListener(this, this::pyxError);
    }

    private void pyxReady(@NotNull RegisteredPyx pyx) {
        this.pyx = pyx;

        runOnUiThread(() -> {
            if (settingsFragment != null) settingsFragment.callPyxReady(pyx);
            if (gamesFragment != null) gamesFragment.callPyxReady(pyx);
            if (profileFragment != null) profileFragment.callPyxReady(pyx);

            if (pyx.firstLoad().nextOperation == FirstLoad.NextOp.GAME)
                startActivity(GameActivity.gameIntent(this, pyx.firstLoad().game));
        });
    }

    private void pyxInvalid(@Nullable Exception ex) {
        this.pyx = null;

        runOnUiThread(() -> {
            if (settingsFragment != null) settingsFragment.callPyxInvalid(ex);
            if (gamesFragment != null) gamesFragment.callPyxInvalid(ex);
            if (profileFragment != null) profileFragment.callPyxInvalid(ex);
        });
    }

    private void pyxError(@NotNull Exception ex) {
        Log.e(TAG, "Failed loading Pyx instance.", ex);
        pyxInvalid(ex);
    }

    @NotNull
    private Task<RegisteredPyx> preparePyxInstance() {
        String username = Prefs.getString(PK.LAST_NICKNAME, null);
        if (username == null) {
            startActivity(new Intent(this, OneTimeLoginActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            return Tasks.forException(new IllegalStateException("No username set."));
        }

        return preparePyxInstance(username, Prefs.getString(PK.LAST_ID_CODE, null));
    }

    @NotNull
    private Task<RegisteredPyx> preparePyxInstance(@NotNull String username, @Nullable String idCode) {
        Continuation<FirstLoadedPyx, Task<RegisteredPyx>> firstLoadContinuation = task -> {
            FirstLoadedPyx pyx = task.getResult();
            FirstLoad fl = pyx.firstLoad();
            if (fl.inProgress && fl.user != null) return pyx.upgrade(fl.user);
            else return task.getResult().register(username, idCode);
        };

        pyxInvalid(null);
        Prefs.putString(PK.LAST_NICKNAME, username);
        Prefs.putString(PK.LAST_ID_CODE, idCode);

        try {
            Pyx local = Pyx.get();
            if (local instanceof RegisteredPyx)
                return prepareTask = Tasks.forResult((RegisteredPyx) local);
            else if (local instanceof FirstLoadedPyx)
                return prepareTask = ((FirstLoadedPyx) local).register(username, idCode);
            else
                return prepareTask = local.doFirstLoad().continueWithTask(firstLoadContinuation);
        } catch (LevelMismatchException ex) {
            return prepareTask = PyxDiscoveryApi.get().firstLoad(this).continueWithTask(firstLoadContinuation);
        }
    }

    @Override
    public void onBackPressed() {
        ChildFragment visible;
        int itemId = binding.mainNavigation.getSelectedItemId();
        if (itemId == R.id.mainNavigation_settings) {
            visible = settingsFragment;
        } else if (itemId == R.id.mainNavigation_home) {
            visible = gamesFragment;
        } else if (itemId == R.id.mainNavigation_profile) {
            visible = profileFragment;
        } else {
            visible = null;
        }

        if (visible == null || !visible.goBack()) {
            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
            dialog.setTitle(R.string.logout).setMessage(Html.fromHtml(getString(R.string.logout_confirmation)))
                    .setNegativeButton(R.string.no, null)
                    .setPositiveButton(R.string.yes, (d, which) -> {
                        pyx.logout();
                        super.onBackPressed();
                    });

            showDialog(dialog);
        }
    }

    public static abstract class ChildFragment extends FragmentWithDialog {
        private boolean mStarted = false;
        private boolean callReady = false;
        private boolean callInvalid = false;
        private RegisteredPyx pyx = null;
        private Exception ex = null;

        @CallSuper
        @Override
        public void onStart() {
            super.onStart();
            mStarted = true;

            if (callReady && pyx != null) onPyxReady(pyx);
            else if (callInvalid) onPyxInvalid(ex);

            callReady = false;
            callInvalid = false;
            pyx = null;
        }

        public void callPyxReady(@NotNull RegisteredPyx pyx) {
            if (mStarted && isAdded()) {
                onPyxReady(pyx);
            } else {
                callInvalid = false;
                callReady = true;
                this.pyx = pyx;
            }
        }

        public void callPyxInvalid(@Nullable Exception ex) {
            if (mStarted && isAdded()) {
                onPyxInvalid(ex);
            } else {
                callInvalid = true;
                callReady = false;
                this.pyx = null;
                this.ex = null;
            }
        }

        protected void onPyxReady(@NotNull RegisteredPyx pyx) {
        }

        protected void onPyxInvalid(@Nullable Exception ex) {
        }

        /**
         * @return Whether it has consumed the event.
         */
        protected abstract boolean goBack();
    }
}
