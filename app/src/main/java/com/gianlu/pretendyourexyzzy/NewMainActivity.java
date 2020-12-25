package com.gianlu.pretendyourexyzzy;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;

import androidx.annotation.CallSuper;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.dialogs.FragmentWithDialog;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.pretendyourexyzzy.api.FirstLoadedPyx;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxChatHelper;
import com.gianlu.pretendyourexyzzy.api.PyxDiscoveryApi;
import com.gianlu.pretendyourexyzzy.api.PyxException;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PlayGamesAuthProvider;

import org.jetbrains.annotations.NotNull;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Objects;

import javax.net.ssl.SSLException;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.OverloadedChatApi;

public class NewMainActivity extends ActivityWithDialog implements OverloadedChatApi.UnreadCountListener, PyxChatHelper.UnreadCountListener, Pyx.OnPollingPyxErrorListener {
    private static final String TAG = NewMainActivity.class.getSimpleName();
    private ActivityNewMainBinding binding;
    private RegisteredPyx pyx;
    private Task<RegisteredPyx> prepareTask;
    private PagerAdapter adapter;
    private BottomNavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener;

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

        binding.mainPager.setOffscreenPageLimit(3);
        binding.mainPager.setAdapter(adapter = new PagerAdapter(this));
        binding.mainPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }

            @Override
            public void onPageSelected(int position) {
                try {
                    binding.mainNavigation.setOnNavigationItemSelectedListener(null);
                    updateBadges();
                    switch (position) {
                        case 0:
                            checkReloadNeeded();
                            binding.mainNavigation.setSelectedItemId(R.id.mainNavigation_settings);
                            break;
                        case 1:
                            checkReloadNeeded();
                            binding.mainNavigation.setSelectedItemId(R.id.mainNavigation_home);
                            break;
                        case 2:
                            binding.mainNavigation.setSelectedItemId(R.id.mainNavigation_profile);
                            break;
                        default:
                            throw new IllegalArgumentException("Unknown position: " + position);
                    }
                } finally {
                    binding.mainNavigation.setOnNavigationItemSelectedListener(navigationItemSelectedListener);
                }
            }
        });

        binding.mainNavigation.setOnNavigationItemReselectedListener(item -> {
            // Do nothing
        });
        binding.mainNavigation.setOnNavigationItemSelectedListener(navigationItemSelectedListener = item -> {
            if (item.getItemId() == R.id.mainNavigation_settings) {
                binding.mainPager.setCurrentItem(0, true);
                return true;
            } else if (item.getItemId() == R.id.mainNavigation_home) {
                binding.mainPager.setCurrentItem(1, true);
                return true;
            } else if (item.getItemId() == R.id.mainNavigation_profile) {
                binding.mainPager.setCurrentItem(2, true);
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
                    if (!signedId) {
                        ThisApplication.setUserProperty("overloaded", false);
                        return;
                    }

                    ThisApplication.setUserProperty("overloaded", true);
                    ThisApplication.setUserProperty("overloaded_uid", FirebaseAuth.getInstance().getUid());

                    OverloadedUtils.doInitChecks(this);

                    SyncUtils.syncStarredCards(this, null);
                    SyncUtils.syncCustomDecks(this, null);
                    SyncUtils.syncStarredCustomDecks(this, null);

                    OverloadedApi.chat(this).addUnreadCountListener(this);

                    OverloadedSignInHelper.signInSilently(this, PlayGamesAuthProvider.PROVIDER_ID)
                            .addOnSuccessListener(account -> {
                                String authCode = account.getServerAuthCode();
                                if (authCode != null) OverloadedApi.get().linkGames(authCode);
                            });
                });
    }

    public void checkReloadNeeded() {
        if (adapter == null || prepareTask == null || !(prepareTask.isComplete() || prepareTask.isCanceled()))
            return;

        String newUsername = adapter.profileFragment.getUsername();
        String newIdCode = adapter.profileFragment.getIdCode();
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

        pyx.chat().addUnreadCountListener(this);
        pyx.polling().addErrorListener(this);

        runOnUiThread(() -> {
            if (adapter != null) adapter.callPyxReady(pyx);

            if (pyx.firstLoad().nextOperation == FirstLoad.NextOp.GAME)
                startActivity(GameActivity.gameIntent(this, pyx.firstLoad().game));
        });
    }

    private void pyxInvalid(@Nullable Exception ex) {
        if (this.pyx != null) {
            pyx.chat().removeUnreadCountListener(this);
            pyx.polling().removeErrorListener(this);
        }

        this.pyx = null;

        runOnUiThread(() -> {
            if (adapter != null) adapter.callPyxInvalid(ex);
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
            else return pyx.register(username, idCode);
        };

        pyxInvalid(null);
        Prefs.putString(PK.LAST_NICKNAME, username);
        Prefs.putString(PK.LAST_ID_CODE, idCode);

        Task<RegisteredPyx> task;
        try {
            Pyx local = Pyx.get();
            if (local instanceof RegisteredPyx)
                return prepareTask = Tasks.forResult((RegisteredPyx) local);
            else if (local instanceof FirstLoadedPyx)
                task = ((FirstLoadedPyx) local).register(username, idCode);
            else
                task = local.doFirstLoad().continueWithTask(firstLoadContinuation);
        } catch (LevelMismatchException ex) {
            task = PyxDiscoveryApi.get().firstLoad(this).continueWithTask(firstLoadContinuation);
        }

        return prepareTask = task.continueWithTask(task1 -> {
            RegisteredPyx pyx;
            try {
                pyx = task1.getResult(PyxException.class);
            } catch (PyxException ex) {
                if (ex.errorCode.equals("niu") && (ex.hadException(SocketTimeoutException.class) || ex.hadException(SSLException.class) || ex.hadException(ConnectException.class))) {
                    return PyxDiscoveryApi.get().firstLoad(this).continueWithTask(task2 -> {
                        FirstLoadedPyx flp = task2.getResult();
                        FirstLoad fl = flp.firstLoad();
                        if (fl.inProgress && fl.user != null) return flp.upgrade(fl.user);
                        else throw ex;
                    });
                } else {
                    throw ex;
                }
            }

            return Tasks.forResult(pyx);
        });
    }

    @Override
    public void onPollPyxError(@NonNull PyxException ex) {
        if (ex.errorCode.equals("nr") || ex.errorCode.equals("se")) {
            if (pyx != null) pyx.logout(ex.errorCode.equals("nr"));
            preparePyxInstance()
                    .addOnSuccessListener(this::pyxReady)
                    .addOnFailureListener(this::pyxError);
        }
    }

    @Override
    public void onBackPressed() {
        ChildFragment visible = null;
        if (adapter != null) {
            int itemId = binding.mainNavigation.getSelectedItemId();
            if (itemId == R.id.mainNavigation_settings) visible = adapter.settingsFragment;
            else if (itemId == R.id.mainNavigation_home) visible = adapter.gamesFragment;
            else if (itemId == R.id.mainNavigation_profile) visible = adapter.profileFragment;
        }

        if (visible == null || !visible.goBack()) {
            MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this);
            dialog.setTitle(R.string.logout).setMessage(Html.fromHtml(getString(R.string.logout_confirmation)))
                    .setNegativeButton(R.string.no, null)
                    .setPositiveButton(R.string.yes, (d, which) -> {
                        if (pyx != null) pyx.logout();
                        super.onBackPressed();
                    });

            showDialog(dialog);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.pyx != null) {
            pyx.polling().removeErrorListener(this);
            pyx.chat().removeUnreadCountListener(this);
        }

        OverloadedApi.chat(this).removeUnreadCountListener(this);
    }

    @Override
    public void overloadedUnreadCountUpdated(int unread) {
        setBadge(R.id.mainNavigation_profile, unread);
    }

    @Override
    public void pyxUnreadCountUpdated(int globalUnread, int gameUnread) {
        setBadge(R.id.mainNavigation_home, globalUnread);
    }

    private void setBadge(@IdRes int item, int count) {
        if (count <= 0) binding.mainNavigation.removeBadge(item);
        else binding.mainNavigation.getOrCreateBadge(item).setNumber(count);
    }

    private void updateBadges() {
        setBadge(R.id.mainNavigation_home, pyx == null ? 0 : pyx.chat().getGlobalUnread());
        setBadge(R.id.mainNavigation_profile, OverloadedApi.chat(this).countTotalUnread());
    }

    private static class PagerAdapter extends FragmentStatePagerAdapter {
        private final NewSettingsFragment settingsFragment;
        private final NewGamesFragment gamesFragment;
        private final NewProfileFragment profileFragment;

        PagerAdapter(@NonNull FragmentActivity activity) {
            super(activity.getSupportFragmentManager(), BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
            this.settingsFragment = NewSettingsFragment.get();
            this.gamesFragment = NewGamesFragment.get();
            this.profileFragment = NewProfileFragment.get();
        }

        void callPyxReady(RegisteredPyx pyx) {
            settingsFragment.callPyxReady(pyx);
            gamesFragment.callPyxReady(pyx);
            profileFragment.callPyxReady(pyx);
        }

        void callPyxInvalid(Exception ex) {
            settingsFragment.callPyxInvalid(ex);
            gamesFragment.callPyxInvalid(ex);
            profileFragment.callPyxInvalid(ex);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return settingsFragment;
                case 1:
                    return gamesFragment;
                case 2:
                    return profileFragment;
                default:
                    throw new IllegalArgumentException("Unknown position: " + position);
            }
        }

        @Override
        public int getCount() {
            return 3;
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
