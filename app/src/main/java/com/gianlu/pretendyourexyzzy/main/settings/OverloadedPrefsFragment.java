package com.gianlu.pretendyourexyzzy.main.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.Html;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.customdecks.CustomDecksDatabase;
import com.gianlu.pretendyourexyzzy.dialogs.OverloadedSubDialog;
import com.gianlu.pretendyourexyzzy.main.NewSettingsFragment;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedChooseProviderDialog;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedSignInHelper;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.yarolegovich.mp.MaterialStandardPreference;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import xyz.gianlu.pyxoverloaded.OverloadedApi;

public class OverloadedPrefsFragment extends NewSettingsFragment.PrefsChildFragment {
    private static final String TAG = OverloadedPrefsFragment.class.getSimpleName();
    private static final int RC_UPLOAD_PROFILE_IMAGE = 88;

    private boolean hasLinked() {
        FirebaseUser user = OverloadedApi.get().firebaseUser();
        return user != null && user.getProviderData().size() > 0;
    }

    @NonNull
    private List<String> linkedProviderNames(@NonNull Context context) {
        List<String> names = new ArrayList<>();
        for (OverloadedSignInHelper.SignInProvider provider : OverloadedSignInHelper.SIGN_IN_PROVIDERS) {
            if (OverloadedApi.get().hasLinkedProvider(provider.id))
                names.add(context.getString(provider.nameRes));
        }

        return names;
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
        for (OverloadedSignInHelper.SignInProvider provider : OverloadedSignInHelper.SIGN_IN_PROVIDERS) {
            if (!OverloadedApi.get().hasLinkedProvider(provider.id))
                names.add(context.getString(provider.nameRes));
        }

        return names;
    }

    @NonNull
    private List<String> linkableProviderIds() {
        List<String> ids = new ArrayList<>();
        for (OverloadedSignInHelper.SignInProvider provider : OverloadedSignInHelper.SIGN_IN_PROVIDERS) {
            if (!OverloadedApi.get().hasLinkedProvider(provider.id))
                ids.add(provider.id);
        }

        return ids;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RC_UPLOAD_PROFILE_IMAGE) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                try {
                    InputStream in = requireContext().getContentResolver().openInputStream(data.getData());
                    if (in == null) return;

                    OverloadedApi.get().uploadProfileImage(in)
                            .addOnSuccessListener(imageId -> {
                                showToast(Toaster.build().message(R.string.profileImageChanged));
                                rebuildPreferences();
                            })
                            .addOnFailureListener(ex -> {
                                Log.e(TAG, "Failed uploading profile image.", ex);

                                if (ex instanceof OverloadedApi.OverloadedServerException && ((OverloadedApi.OverloadedServerException) ex).reason.equals(OverloadedApi.OverloadedServerException.REASON_NSFW_DETECTED))
                                    showToast(Toaster.build().message(R.string.nsfwDetectedMessage));
                                else
                                    showToast(Toaster.build().message(R.string.failedUploadingImage));
                            });
                } catch (IOException ex) {
                    Log.e(TAG, "Failed reading image data: " + data, ex);
                    showToast(Toaster.build().message(R.string.failedUploadingImage));
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void buildPreferences(@NonNull Context context) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            MaterialStandardPreference login = new MaterialStandardPreference(context);
            login.setTitle(R.string.subscribe);
            login.setSummary(R.string.getOverloadedNow_desc);
            login.setOnClickListener(v -> OverloadedSubDialog.get().show(getChildFragmentManager(), null));
            addPreference(login);
            return;
        }

        //region Profile
        addCategory(R.string.profile);

        MaterialStandardPreference loggedInAs = new MaterialStandardPreference(context);
        loggedInAs.setTitle(R.string.loggedIn);
        loggedInAs.setSummary(Utils.getDisplayableName(currentUser));
        loggedInAs.setClickable(false);
        addPreference(loggedInAs);

        if (hasLinked()) {
            MaterialStandardPreference linkedAccounts = new MaterialStandardPreference(context);
            linkedAccounts.setTitle(R.string.linkedAccounts);
            linkedAccounts.setSummary(CommonUtils.join(linkedProviderNames(context), ", "));
            linkedAccounts.setClickable(false);
            addPreference(linkedAccounts);
        }

        if (canLink()) {
            MaterialStandardPreference linkAccount = new MaterialStandardPreference(context);
            linkAccount.setTitle(R.string.linkAccount);
            linkAccount.setSummary(CommonUtils.join(linkableProviderNames(context), ", "));
            linkAccount.setOnClickListener(v -> OverloadedChooseProviderDialog.getLinkInstance(linkableProviderIds()).show(getChildFragmentManager(), null));
            addPreference(linkAccount);
        }

        MaterialStandardPreference purchaseStatus = new MaterialStandardPreference(context);
        purchaseStatus.setTitle(R.string.purchaseStatus);
        purchaseStatus.setClickable(false);
        purchaseStatus.setLoading(true);
        addPreference(purchaseStatus);

        MaterialStandardPreference logout = new MaterialStandardPreference(context);
        logout.setTitle(R.string.logout);
        logout.setIcon(R.drawable.outline_exit_to_app_24);
        logout.setOnClickListener(v -> {
            OverloadedApi.get().logout();
            CustomDecksDatabase.get(requireContext()).clearStarredDecks();
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
                        OverloadedApi.get().deleteAccount()
                                .addOnSuccessListener(aVoid -> {
                                    dismissDialog();
                                    showToast(Toaster.build().message(R.string.accountDeleted));
                                    onBackPressed();
                                })
                                .addOnFailureListener(ex -> {
                                    Log.e(TAG, "Failed deleting account.", ex);
                                    dismissDialog();
                                    showToast(Toaster.build().message(R.string.failedDeletingAccount));
                                });
                    });

            showDialog(dialog);
        });
        addPreference(delete);
        //endregion

        //region Profile image
        addCategory(R.string.profileImage);

        MaterialStandardPreference uploadProfileImage = new MaterialStandardPreference(context);
        uploadProfileImage.setTitle(R.string.uploadProfileImage);
        uploadProfileImage.setSummary(R.string.profileImage_message);
        uploadProfileImage.setEnabled(false);
        addPreference(uploadProfileImage);

        MaterialStandardPreference removeProfileImage = new MaterialStandardPreference(context);
        removeProfileImage.setTitle(R.string.removeProfileImage);
        removeProfileImage.setEnabled(false);
        addPreference(removeProfileImage);
        //endregion

        OverloadedApi.get().userData()
                .addOnSuccessListener(userData -> {
                    purchaseStatus.setSummary(String.format("%s (%s)", getString(userData.purchaseStatusGranular.getName()), userData.username));
                    purchaseStatus.setLoading(false);

                    if (userData.purchaseStatus.ok) {
                        if (userData.profileImageId == null) {
                            uploadProfileImage.setEnabled(true);
                            uploadProfileImage.setTitle(R.string.uploadProfileImage);

                            removePreference(removeProfileImage);
                        } else {
                            uploadProfileImage.setEnabled(true);
                            uploadProfileImage.setTitle(R.string.changeProfileImage);

                            removeProfileImage.setEnabled(true);
                            removeProfileImage.setOnClickListener(v -> OverloadedApi.get().removeProfileImage()
                                    .addOnSuccessListener(aVoid -> {
                                        showToast(Toaster.build().message(R.string.profileImageRemoved));
                                        rebuildPreferences();
                                    })
                                    .addOnFailureListener(ex -> {
                                        Log.e(TAG, "Failed removing profile image.", ex);
                                        showToast(Toaster.build().message(R.string.failedRemovingProfileImage));
                                    }));
                        }

                        uploadProfileImage.setOnClickListener(v -> {
                            Intent intent = OverloadedUtils.getImageUploadIntent();
                            startActivityForResult(Intent.createChooser(intent, "Pick an image to upload..."), RC_UPLOAD_PROFILE_IMAGE);
                        });
                    } else {
                        removeCategory(R.string.profileImage);
                    }
                })
                .addOnFailureListener(ex -> {
                    Log.e(TAG, "Failed getting user data.", ex);
                    purchaseStatus.setSummary("<error>");
                    purchaseStatus.setLoading(false);

                    removeCategory(R.string.profileImage);
                });
    }

    @Override
    public int getTitleRes() {
        return R.string.overloaded;
    }
}
