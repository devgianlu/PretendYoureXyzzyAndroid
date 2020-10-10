package com.gianlu.pretendyourexyzzy;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;

import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.pretendyourexyzzy.api.FirstLoadedPyx;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxDiscoveryApi;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.FirstLoad;
import com.gianlu.pretendyourexyzzy.databinding.ActivityNewMainBinding;
import com.gianlu.pretendyourexyzzy.main.NewGamesFragment;
import com.gianlu.pretendyourexyzzy.main.NewProfileFragment;
import com.gianlu.pretendyourexyzzy.main.NewSettingsFragment;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import org.jetbrains.annotations.NotNull;

public class NewMainActivity extends ActivityWithDialog {
    private static final String SETTINGS_FRAGMENT_TAG = "settings";
    private static final String GAMES_FRAGMENT_TAG = "games";
    private static final String PROFILE_FRAGMENT_TAG = "profile";
    private static final String TAG = NewMainActivity.class.getSimpleName();
    private ActivityNewMainBinding binding;
    private NewSettingsFragment settingsFragment;
    private NewGamesFragment gamesFragment;
    private NewProfileFragment profileFragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Prefs.getBoolean(PK.ONE_TIME_LOGIN_SHOWN)) {
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
            switch (item.getItemId()) {
                case R.id.mainNavigation_settings:
                    getSupportFragmentManager().beginTransaction()
                            .hide(gamesFragment).hide(profileFragment)
                            .show(settingsFragment)
                            .commitNow();
                    return true;
                case R.id.mainNavigation_home:
                    getSupportFragmentManager().beginTransaction()
                            .hide(settingsFragment).hide(profileFragment)
                            .show(gamesFragment)
                            .commitNow();
                    return true;
                case R.id.mainNavigation_profile:
                    getSupportFragmentManager().beginTransaction()
                            .hide(gamesFragment).hide(settingsFragment)
                            .show(profileFragment)
                            .commitNow();
                    return true;
                default:
                    return false;
            }
        });
        binding.mainNavigation.setSelectedItemId(R.id.mainNavigation_home);

        preparePyxInstance("test12366", null) // TODO
                .addOnSuccessListener(this, this::pyxReady)
                .addOnFailureListener(this, ex -> {
                    Log.e(TAG, "Failed loading Pyx instance.", ex);
                    pyxInvalid();
                });
    }

    private void pyxReady(@NotNull RegisteredPyx pyx) {
        if (settingsFragment != null) settingsFragment.onPyxReady(pyx);
        if (gamesFragment != null) gamesFragment.onPyxReady(pyx);
        if (profileFragment != null) profileFragment.onPyxReady(pyx);
    }

    private void pyxInvalid() {
        if (settingsFragment != null) settingsFragment.onPyxInvalid();
        if (gamesFragment != null) gamesFragment.onPyxInvalid();
        if (profileFragment != null) profileFragment.onPyxInvalid();
    }

    @NotNull
    private Task<RegisteredPyx> preparePyxInstance(@NotNull String username, @Nullable String idCode) {
        Continuation<FirstLoadedPyx, Task<RegisteredPyx>> firstLoadContinuation = task -> {
            FirstLoadedPyx pyx = task.getResult();
            FirstLoad fl = pyx.firstLoad();
            if (fl.inProgress && fl.user != null) {
                if (fl.nextOperation == FirstLoad.NextOp.GAME) {
                    // TODO
                }

                return Tasks.forResult(pyx.upgrade(fl.user));
            } else {
                return task.getResult().register(username, idCode);
            }
        };

        try {
            Pyx local = Pyx.get();
            if (local instanceof RegisteredPyx) {
                return Tasks.forResult((RegisteredPyx) local);
            } else if (local instanceof FirstLoadedPyx) {
                return ((FirstLoadedPyx) local).register(username, idCode);
            } else {
                return local.doFirstLoad().continueWithTask(firstLoadContinuation);
            }
        } catch (LevelMismatchException ex) {
            return PyxDiscoveryApi.get().firstLoad(this).continueWithTask(firstLoadContinuation);
        }
    }

    public interface MainFragment {
        void onPyxReady(@NotNull RegisteredPyx pyx);

        void onPyxInvalid();
    }
}
