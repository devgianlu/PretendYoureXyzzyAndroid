package com.gianlu.pretendyourexyzzy;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutItem;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.preferences.BasePreferenceActivity;
import com.gianlu.commonutils.preferences.BasePreferenceFragment;
import com.gianlu.commonutils.preferences.MaterialAboutPreferenceItem;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.activities.TutorialActivity;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedSignInDialog;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedSignInHelper;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedSignInHelper.SignInProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.yarolegovich.mp.MaterialCheckboxPreference;
import com.yarolegovich.mp.MaterialStandardPreference;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class PreferenceActivity extends BasePreferenceActivity implements OverloadedSignInDialog.Listener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        View root = getWindow().getDecorView().findViewById(android.R.id.content);
        if (root != null)
            GPGamesHelper.setPopupView(this, root, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
    }

    @NonNull
    @Override
    protected List<MaterialAboutPreferenceItem> getPreferencesItems() {
        return Arrays.asList(new MaterialAboutPreferenceItem(R.string.general, R.drawable.baseline_settings_24, GeneralFragment.class),
                new MaterialAboutPreferenceItem(R.string.overloaded, R.drawable.baseline_videogame_asset_24, OverloadedFragment.class));
    }

    @Override
    protected int getAppIconRes() {
        return R.mipmap.ic_launcher;
    }

    @NonNull
    @Override
    protected List<MaterialAboutItem> customizeTutorialCard() {
        return Collections.singletonList(new MaterialAboutActionItem(R.string.showBeginnerTutorial, R.string.showBeginnerTutorial_summary, 0, () -> startActivity(new Intent(PreferenceActivity.this, TutorialActivity.class))));
    }

    @Override
    protected boolean hasTutorial() {
        return true;
    }

    @Nullable
    @Override
    protected String getOpenSourceUrl() {
        return "https://github.com/devgianlu/PretendYoureXyzzyAndroid";
    }

    @Override
    protected boolean disableOtherDonationsOnGooglePlay() {
        return true;
    }

    @Override
    public void onSelectedSignInProvider(@NonNull SignInProvider provider) {
        OverloadedFragment fragment = (OverloadedFragment) getSupportFragmentManager().findFragmentByTag(OverloadedFragment.class.getName());
        if (fragment != null) fragment.onSelectedSignInProvider(provider);
    }

    public static class GeneralFragment extends BasePreferenceFragment {

        private void showUnblockDialog(@NonNull Context context) {
            String[] entries = Prefs.getSet(PK.BLOCKED_USERS, new HashSet<>()).toArray(new String[0]);
            boolean[] checked = new boolean[entries.length];
            for (int i = 0; i < checked.length; i++) checked[i] = false;

            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context);
            builder.setTitle(R.string.unblockUser)
                    .setMultiChoiceItems(entries, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
                    .setPositiveButton(R.string.unblock, (dialog, which) -> {
                        for (int i = 0; i < checked.length; i++) {
                            if (checked[i]) BlockedUsers.unblock(entries[i]);
                        }

                        if (Prefs.isSetEmpty(PK.BLOCKED_USERS)) onBackPressed();
                    }).setNegativeButton(android.R.string.cancel, null);

            showDialog(builder);
        }

        @Override
        protected void buildPreferences(@NonNull Context context) {
            MaterialCheckboxPreference nightMode = new MaterialCheckboxPreference.Builder(context)
                    .defaultValue(PK.NIGHT_MODE.fallback())
                    .key(PK.NIGHT_MODE.key())
                    .build();
            nightMode.setTitle(R.string.prefs_nightMode);
            nightMode.setSummary(R.string.prefs_nightMode_summary);
            addPreference(nightMode);

            if (!Prefs.isSetEmpty(PK.BLOCKED_USERS)) {
                MaterialStandardPreference unblock = new MaterialStandardPreference(context);
                unblock.setTitle(R.string.unblockUser);
                unblock.setSummary(R.string.unblockUser_summary);
                unblock.setOnClickListener(v -> showUnblockDialog(context));
                addPreference(unblock);
            }
        }

        @Override
        public int getTitleRes() {
            return R.string.general;
        }
    }

    public static class OverloadedFragment extends BasePreferenceFragment implements OverloadedSignInDialog.Listener {
        private static final int RC_SIGN_IN = 3;
        private final OverloadedSignInHelper signInHelper = new OverloadedSignInHelper();

        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            if (requestCode == RC_SIGN_IN) {
                if (data != null) {
                    signInHelper.processSignInData(data, new OverloadedSignInHelper.SignInCallback() {
                        @Override
                        public void onSignInSuccessful() {
                            showToast(Toaster.build().message(R.string.signInSuccessful));
                            onBackPressed();
                        }

                        @Override
                        public void onSignInFailed() {
                            showToast(Toaster.build().message(R.string.failedSigningIn));
                            onBackPressed();
                        }
                    });
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }

        @Override
        protected void buildPreferences(@NonNull Context context) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                MaterialStandardPreference loggedInAs = new MaterialStandardPreference(context);
                loggedInAs.setTitle(R.string.loggedIn);
                loggedInAs.setSummary(Utils.getDisplayableName(currentUser));
                loggedInAs.setClickable(false);
                addPreference(loggedInAs);

                // TODO: Overloaded payment status

                MaterialStandardPreference logout = new MaterialStandardPreference(context);
                logout.setTitle(R.string.logout);
                logout.setIcon(R.drawable.outline_exit_to_app_24);
                logout.setOnClickListener(v -> {
                    FirebaseAuth.getInstance().signOut();
                    onBackPressed();
                });
                addPreference(logout);
            } else {
                MaterialStandardPreference login = new MaterialStandardPreference(context);
                login.setTitle(R.string.login);
                login.setSummary(R.string.overloadedLogin_please);
                login.setOnClickListener(v -> DialogUtils.showDialog(getActivity(), OverloadedSignInDialog.getInstance(), null));
                addPreference(login);
            }
        }

        @Override
        public int getTitleRes() {
            return R.string.overloaded;
        }

        @Override
        public void onSelectedSignInProvider(@NonNull SignInProvider provider) {
            if (getActivity() == null) return;
            startActivityForResult(signInHelper.startFlow(getActivity(), provider), RC_SIGN_IN);
        }
    }
}
