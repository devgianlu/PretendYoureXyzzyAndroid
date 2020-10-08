package com.gianlu.pretendyourexyzzy;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutItem;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.preferences.BasePreferenceActivity;
import com.gianlu.commonutils.preferences.BasePreferenceFragment;
import com.gianlu.commonutils.preferences.CommonPK;
import com.gianlu.commonutils.preferences.MaterialAboutPreferenceItem;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.activities.TutorialActivity;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedChooseProviderDialog;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedSignInHelper;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedSignInHelper.SignInProvider;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.yarolegovich.mp.MaterialCheckboxPreference;
import com.yarolegovich.mp.MaterialStandardPreference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.callback.SuccessCallback;
import xyz.gianlu.pyxoverloaded.callback.UserDataCallback;
import xyz.gianlu.pyxoverloaded.model.UserData;

public class PreferenceActivity extends BasePreferenceActivity implements OverloadedChooseProviderDialog.Listener {
    private static final String TAG = PreferenceActivity.class.getSimpleName();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GPGamesHelper.setPopupView(this, Gravity.CENTER_HORIZONTAL | Gravity.TOP);

        if (disableUsageStatisticsToggle()) {
            Prefs.putBoolean(CommonPK.CRASH_REPORT_ENABLED, true);
            Prefs.putBoolean(CommonPK.TRACKING_ENABLED, true);
        }
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
    protected boolean disableUsageStatisticsToggle() {
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

    public static class OverloadedFragment extends BasePreferenceFragment implements OverloadedChooseProviderDialog.Listener {
        private static final int RC_SIGN_IN = 3;
        private final OverloadedSignInHelper signInHelper = new OverloadedSignInHelper();
        private boolean link;

        @Override
        public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            if (requestCode == RC_SIGN_IN && data != null) {
                if (link) {
                    AuthCredential credential = signInHelper.extractCredential(data);
                    if (credential == null) {
                        Log.w(TAG, "Couldn't extract credentials: " + data);
                        showToast(Toaster.build().message(R.string.failedSigningIn));
                        return;
                    }

                    OverloadedApi.get().link(credential, task -> {
                        if (task.isSuccessful()) {
                            showToast(Toaster.build().message(R.string.accountLinked));
                        } else {
                            showToast(Toaster.build().message(R.string.failedLinkingAccount));
                            Log.e(TAG, "Failed linking account.", task.getException());
                        }

                        onBackPressed();
                    });
                } else {
                    signInHelper.processSignInData(data, new OverloadedSignInHelper.SignInCallback() {
                        @Override
                        public void onSignInSuccessful(@NonNull FirebaseUser user) {
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

        private boolean canLink() {
            FirebaseUser user = OverloadedApi.get().firebaseUser();
            if (user == null) return false;

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

        @NonNull
        private List<String> linkableProviderNames(@NonNull Context context) {
            List<String> names = new ArrayList<>();
            for (SignInProvider provider : OverloadedSignInHelper.SIGN_IN_PROVIDERS) {
                if (!OverloadedApi.get().hasLinkedProvider(provider.id))
                    names.add(context.getString(provider.nameRes));
            }

            return names;
        }

        @NonNull
        private List<String> linkableProviderIds() {
            List<String> ids = new ArrayList<>();
            for (SignInProvider provider : OverloadedSignInHelper.SIGN_IN_PROVIDERS) {
                if (!OverloadedApi.get().hasLinkedProvider(provider.id))
                    ids.add(provider.id);
            }

            return ids;
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

                if (canLink()) {
                    MaterialStandardPreference linkAccount = new MaterialStandardPreference(context);
                    linkAccount.setTitle(R.string.linkAccount);
                    linkAccount.setSummary(CommonUtils.join(linkableProviderNames(context), ", "));
                    linkAccount.setOnClickListener(v -> {
                        link = true;
                        DialogUtils.showDialog(getActivity(),
                                OverloadedChooseProviderDialog.getLinkInstance(linkableProviderIds()),
                                null);
                    });
                    addPreference(linkAccount);
                }

                MaterialStandardPreference purchaseStatus = new MaterialStandardPreference(context);
                purchaseStatus.setTitle(R.string.purchaseStatus);
                purchaseStatus.setClickable(false);
                purchaseStatus.setLoading(true);
                addPreference(purchaseStatus);
                OverloadedApi.get().userData(getActivity(), new UserDataCallback() {
                    @Override
                    public void onUserData(@NonNull UserData userData) {
                        purchaseStatus.setSummary(String.format("%s (%s)", userData.purchaseStatus.toString(context), userData.username));
                        purchaseStatus.setLoading(false);
                    }

                    @Override
                    public void onFailed(@NonNull Exception ex) {
                        Log.e(TAG, "Failed getting user data.", ex);
                        purchaseStatus.setSummary("<error>");
                        purchaseStatus.setLoading(false);
                    }
                });

                MaterialStandardPreference logout = new MaterialStandardPreference(context);
                logout.setTitle(R.string.logout);
                logout.setIcon(R.drawable.outline_exit_to_app_24);
                logout.setOnClickListener(v -> {
                    OverloadedApi.get().logout();
                    onBackPressed();
                });
                addPreference(logout);

                MaterialStandardPreference delete = new MaterialStandardPreference(context);
                delete.setTitle(R.string.deleteAccount);
                delete.setIcon(R.drawable.baseline_delete_forever_24);
                delete.setOnClickListener(v -> {
                    MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(requireContext());
                    dialog.setTitle(R.string.deleteAccount).setMessage(Html.fromHtml(getString(R.string.deleteAccount_confirmation)))
                            .setNegativeButton(android.R.string.no, null)
                            .setPositiveButton(android.R.string.yes, (d, which) -> {
                                showProgress(R.string.loading);
                                OverloadedApi.get().deleteAccount(getActivity(), new SuccessCallback() {
                                    @Override
                                    public void onSuccessful() {
                                        dismissDialog();
                                        showToast(Toaster.build().message(R.string.accountDeleted));
                                        onBackPressed();
                                    }

                                    @Override
                                    public void onFailed(@NonNull Exception ex) {
                                        Log.e(TAG, "Failed deleting account.", ex);
                                        dismissDialog();
                                        showToast(Toaster.build().message(R.string.failedDeletingAccount));
                                    }
                                });
                            });

                    showDialog(dialog);
                });
                addPreference(delete);
            } else {
                MaterialStandardPreference login = new MaterialStandardPreference(context);
                login.setTitle(R.string.login);
                login.setSummary(R.string.overloadedLogin_please);
                login.setOnClickListener(v -> {
                    link = false;
                    DialogUtils.showDialog(getActivity(), OverloadedChooseProviderDialog.getSignInInstance(), null);
                });
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
