package com.gianlu.pretendyourexyzzy.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;
import com.gianlu.pretendyourexyzzy.databinding.DialogOverloadedSubBinding;
import com.gianlu.pretendyourexyzzy.databinding.ItemOverloadedSignInProviderBinding;
import com.gianlu.pretendyourexyzzy.main.NewProfileFragment;
import com.gianlu.pretendyourexyzzy.main.NewSettingsFragment;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedSignInHelper;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PlayGamesAuthProvider;
import com.google.firebase.auth.UserInfo;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import xyz.gianlu.pyxoverloaded.OverloadedApi;

import static com.gianlu.pretendyourexyzzy.overloaded.OverloadedUtils.ACTIVE_SKU;

public final class OverloadedSubDialog extends DialogFragment implements PurchasesUpdatedListener {
    private static final String TAG = OverloadedSubDialog.class.getSimpleName();
    private static final int RC_SIGN_IN = 56;
    private final OverloadedSignInHelper signInHelper = new OverloadedSignInHelper();
    private BillingClient billingClient;
    private SkuDetails skuDetails;
    private DialogOverloadedSubBinding binding;
    private boolean usernameLocked = false;

    @NonNull
    public static OverloadedSubDialog get() {
        return new OverloadedSubDialog();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        Window window = dialog.getWindow();
        if (window != null) window.requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (billingClient != null) billingClient.endConnection();
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        Window window;
        if (dialog != null && (window = dialog.getWindow()) != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.WHITE));
        }

        billingClient = BillingClient.newBuilder(requireContext()).enablePendingPurchases().setListener(this).build();
        billingClient.startConnection(new BillingClientStateListener() {
            private boolean retried = false;

            @Override
            public void onBillingSetupFinished(@NotNull BillingResult br) {
                if (br.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    getSkuDetails(new SkuDetailsCallback() {
                        @Override
                        public void onSuccess(@NonNull SkuDetails details) {
                            if (binding == null)
                                return;

                            binding.overloadedSubDialogPrice.setText(String.format("%s\u00A0", details.getPrice()));
                        }

                        @Override
                        public void onFailed(int code) {
                            Log.e(TAG, "Failed loading SKU details: " + code);
                            dismissAllowingStateLoss();
                        }
                    });
                } else {
                    Log.e(TAG, "Failed setting up billing service: " + br.getResponseCode());
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                if (!retried) {
                    retried = true;
                    billingClient.startConnection(this);
                } else {
                    Log.e(TAG, "Billing service disconnected.");
                }
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DialogOverloadedSubBinding.inflate(inflater, container, false);
        binding.overloadedSubDialogBack.setOnClickListener(v -> dismissAllowingStateLoss());

        if (OverloadedUtils.isSignedIn()) {
            dismissAllowingStateLoss();
            return null;
        }

        CommonUtils.getEditText(binding.overloadedSubDialogUsername).addTextChangedListener(new TextWatcher() {
            private final Timer timer = new Timer();
            private TimerTask lastTask;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (lastTask != null) lastTask.cancel();
                timer.schedule(lastTask = new TimerTask() {
                    private final Handler handler = new Handler(Looper.getMainLooper());

                    @Override
                    public void run() {
                        String username = CommonUtils.getText(binding.overloadedSubDialogUsername);
                        if (username.trim().isEmpty() || usernameLocked) return;

                        if (OverloadedUtils.checkUsernameValid(username)) {
                            OverloadedApi.get().isUsernameUnique(username)
                                    .addOnSuccessListener(result -> {
                                        if (result)
                                            binding.overloadedSubDialogUsername.setErrorEnabled(false);
                                        else
                                            binding.overloadedSubDialogUsername.setError(getString(R.string.overloaded_usernameAlreadyInUse));
                                    })
                                    .addOnFailureListener(ex -> Log.w(TAG, "Failed checking username unique.", ex));
                        } else {
                            handler.post(() -> binding.overloadedSubDialogUsername.setError(getString(R.string.overloaded_invalidUsername)));
                        }
                    }
                }, 500);
            }
        });

        updateStatus(Status.FEATURES);

        return binding.getRoot();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RC_SIGN_IN && data != null && resultCode == Activity.RESULT_OK) {
            signInHelper.processSignInData(data, new OverloadedSignInHelper.SignInCallback() {
                @Override
                public void onSignInSuccessful(@NonNull FirebaseUser user) {
                    binding.overloadedSubDialogProvidersLoading.hideShimmer();
                    binding.overloadedSubDialogProviders.setEnabled(true);
                    DialogUtils.showToast(getContext(), Toaster.build().message(R.string.signInSuccessful));
                    updateStatus(Status.USERNAME);
                }

                @Override
                public void onSignInFailed() {
                    binding.overloadedSubDialogProvidersLoading.hideShimmer();
                    binding.overloadedSubDialogProviders.setEnabled(true);
                    DialogUtils.showToast(getContext(), Toaster.build().message(R.string.failedSigningIn));
                }
            });
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void createProviderItem(@NonNull OverloadedSignInHelper.SignInProvider provider, OnProviderClickListener listener) {
        ItemOverloadedSignInProviderBinding binding = ItemOverloadedSignInProviderBinding.inflate(getLayoutInflater(), this.binding.overloadedSubDialogProviders, true);
        binding.overloadedSignInProviderIcon.setImageResource(provider.iconRes);
        binding.overloadedSignInProviderName.setText(provider.nameRes);
        binding.getRoot().setOnClickListener(v -> listener.onClick(provider));
    }

    private void updateStatus(@NonNull Status status) {
        switch (status) {
            default:
            case FEATURES:
                binding.overloadedSubDialogSignIn.setVisibility(View.GONE);
                binding.overloadedSubDialogRegister.setVisibility(View.GONE);
                binding.overloadedSubDialogFeatures.setVisibility(View.VISIBLE);
                binding.overloadedSubDialogButton.setVisibility(View.VISIBLE);
                binding.overloadedSubDialogButton.setText(R.string.getOverloadedNow);
                binding.overloadedSubDialogButton.setOnClickListener(v -> updateStatus(Status.SIGN_IN));
                break;
            case SIGN_IN:
                binding.overloadedSubDialogSignIn.setVisibility(View.VISIBLE);
                binding.overloadedSubDialogRegister.setVisibility(View.GONE);
                binding.overloadedSubDialogFeatures.setVisibility(View.GONE);
                binding.overloadedSubDialogButton.setVisibility(View.GONE);

                OnProviderClickListener listener = provider -> {
                    if (getActivity() == null) return;

                    binding.overloadedSubDialogProvidersLoading.showShimmer(true);
                    binding.overloadedSubDialogProviders.setEnabled(false);

                    FirebaseAuth.getInstance().signOut();
                    startActivityForResult(signInHelper.startFlow(requireActivity(), provider), RC_SIGN_IN);
                };

                binding.overloadedSubDialogProvidersLoading.hideShimmer();
                binding.overloadedSubDialogProviders.removeAllViews();
                for (OverloadedSignInHelper.SignInProvider provider : OverloadedSignInHelper.SIGN_IN_PROVIDERS)
                    createProviderItem(provider, listener);
                break;
            case USERNAME:
                binding.overloadedSubDialogSignIn.setVisibility(View.GONE);
                binding.overloadedSubDialogRegister.setVisibility(View.VISIBLE);
                binding.overloadedSubDialogFeatures.setVisibility(View.GONE);
                binding.overloadedSubDialogButton.setVisibility(View.VISIBLE);
                binding.overloadedSubDialogButton.setText(R.string.subscribe);
                binding.overloadedSubDialogButton.setOnClickListener(v -> {
                    String username = CommonUtils.getText(binding.overloadedSubDialogUsername);
                    if (OverloadedUtils.checkUsernameValid(username)) {
                        setRegisterLoading(true);
                        Tasks.forResult(usernameLocked)
                                .continueWithTask(task -> {
                                    if (task.getResult()) return Tasks.forResult(true);
                                    else return OverloadedApi.get().isUsernameUnique(username);
                                })
                                .addOnSuccessListener(result -> {
                                    if (result) {
                                        registerUser(username);
                                    } else {
                                        setRegisterLoading(false);
                                        binding.overloadedSubDialogUsername.setError(getString(R.string.overloaded_usernameAlreadyInUse));
                                    }
                                })
                                .addOnFailureListener(ex -> {
                                    binding.overloadedSubDialogUsername.setError(getString(R.string.failedCheckingUsername));
                                    setRegisterLoading(false);
                                    Log.e(TAG, "Failed checking username unique.", ex);
                                });
                    } else {
                        binding.overloadedSubDialogUsername.setError(getString(R.string.overloaded_invalidUsername));
                    }
                });

                binding.overloadedSubDialogUsernameLoading.showShimmer(true);
                OverloadedApi.get().userData()
                        .addOnSuccessListener(userData -> {
                            if (!isAdded()) return;

                            binding.overloadedSubDialogUsernameLoading.hideShimmer();
                            usernameLocked = true;
                            CommonUtils.setText(binding.overloadedSubDialogUsername, userData.username);
                            binding.overloadedSubDialogUsername.setEnabled(false);
                            binding.overloadedSubDialogUsername.setHelperText(getString(R.string.overloadedSub_usernameLocked));

                            if (userData.purchaseStatus.ok) {
                                Prefs.putString(PK.LAST_NICKNAME, userData.username);
                                subscriptionCompleteOk();
                            }
                        })
                        .addOnFailureListener(ex -> {
                            if (!isAdded()) return;

                            usernameLocked = false;
                            binding.overloadedSubDialogUsernameLoading.hideShimmer();
                            binding.overloadedSubDialogUsername.setHelperText(getString(R.string.overloadedSub_usernameInfo));

                            UserInfo playGamesInfo;
                            if ((playGamesInfo = OverloadedApi.get().getProviderUserInfo(PlayGamesAuthProvider.PROVIDER_ID)) != null)
                                CommonUtils.setText(binding.overloadedSubDialogUsername, playGamesInfo.getDisplayName());
                        });
                break;
        }
    }

    private void setRegisterLoading(boolean loading) {
        binding.overloadedSubDialogButton.setEnabled(!loading);
        binding.overloadedSubDialogUsername.setEnabled(!usernameLocked && !loading);

        if (loading) binding.overloadedSubDialogUsernameLoading.showShimmer(true);
        else binding.overloadedSubDialogUsernameLoading.hideShimmer();
    }

    private void setRegisterError() {
        if (!isAdded()) return;

        setRegisterLoading(false);
        binding.overloadedSubDialogUsername.setError(getString(R.string.failedRegistering_tryAgain));
    }

    private void subscriptionCompleteOk() {
        OverloadedApi.get().openWebSocket();
        if (getContext() != null) OverloadedApi.chat(requireContext()).shareKeysIfNeeded();
        DialogUtils.showToast(getContext(), Toaster.build().message(R.string.overloadedSubscribed));

        if (getParentFragment() instanceof NewSettingsFragment.PrefsChildFragment)
            ((NewSettingsFragment.PrefsChildFragment) getParentFragment()).rebuildPreferences();
        else if (getParentFragment() instanceof NewProfileFragment)
            ((NewProfileFragment) getParentFragment()).refreshOverloaded(true);

        if (isAdded()) dismissAllowingStateLoss();
    }

    /**
     * Registers a new user.
     *
     * @param username The username chosen
     */
    private void registerUser(@NonNull String username) {
        Purchase purchase = OverloadedUtils.getLatestPurchase(billingClient);

        setRegisterLoading(true);
        OverloadedApi.get().registerUser(username, purchase != null ? purchase.getSku() : null, purchase != null ? purchase.getPurchaseToken() : null)
                .addOnSuccessListener(data -> {
                    if (data.purchaseStatus.ok) {
                        Prefs.putString(PK.LAST_NICKNAME, username);
                        subscriptionCompleteOk();
                        return;
                    }

                    if (skuDetails != null) {
                        startBillingFlow(skuDetails);
                    } else {
                        getSkuDetails(new SkuDetailsCallback() {
                            @Override
                            public void onSuccess(@NonNull SkuDetails details) {
                                startBillingFlow(details);
                            }

                            @Override
                            public void onFailed(int code) {
                                setRegisterError();
                            }
                        });
                    }
                })
                .addOnFailureListener(ex -> {
                    Log.e(TAG, "Failed registering user.", ex);
                    setRegisterError();
                });
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult br, @Nullable List<Purchase> list) {
        if (br.getResponseCode() != BillingClient.BillingResponseCode.OK && br.getResponseCode() != BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            setRegisterError();
            return;
        }

        if (list == null || list.isEmpty())
            return;

        purchaseComplete(list.get(0));
    }

    /**
     * Gets the SKU details for the standard purchase.
     *
     * @param callback The callback
     */
    private void getSkuDetails(@NonNull SkuDetailsCallback callback) {
        SkuDetailsParams params = SkuDetailsParams.newBuilder().setSkusList(Collections.singletonList(ACTIVE_SKU.sku)).setType(ACTIVE_SKU.skuType).build();
        billingClient.querySkuDetailsAsync(params, new SkuDetailsResponseListener() {
            boolean retried = false;

            @Override
            public void onSkuDetailsResponse(@NonNull BillingResult br, @Nullable List<SkuDetails> list) {
                int code = br.getResponseCode();
                if (code == BillingClient.BillingResponseCode.OK && list != null && !list.isEmpty()) {
                    callback.onSuccess(skuDetails = list.get(0));
                } else if (!retried) {
                    retried = true;
                    billingClient.querySkuDetailsAsync(params, this);
                } else {
                    Log.e(TAG, "Failed loading SKU details: " + code);
                    callback.onFailed(code);
                }
            }
        });
    }

    /**
     * Starts the billing flow for the given SKU.
     *
     * @param product The {@link SkuDetails} for this flow
     */
    private void startBillingFlow(@NonNull SkuDetails product) {
        if (getActivity() == null) return;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) throw new IllegalStateException();

        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setObfuscatedAccountId(Utils.sha1(user.getUid()))
                .setSkuDetails(product)
                .build();

        BillingResult result = billingClient.launchBillingFlow(requireActivity(), flowParams);
        if (result.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            Purchase purchase = OverloadedUtils.getLatestPurchase(billingClient);
            if (purchase == null) {
                Log.e(TAG, "Couldn't find latest purchase.");
                return;
            }

            purchaseComplete(purchase);
        } else if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            setRegisterError();
        }
    }

    /**
     * Checks whether we need to:
     * - Register the user after asking for the username, or
     * - Update the purchase token, or
     * - Nothing
     *
     * @param purchase The latest Overloaded purchase, may be used to update the token
     */
    private void purchaseComplete(@NonNull Purchase purchase) {
        setRegisterLoading(true);
        OverloadedApi.get().userData()
                .addOnSuccessListener(data -> {
                    if (data.purchaseStatus.ok) {
                        subscriptionCompleteOk();
                        return;
                    }

                    OverloadedApi.get().registerUser(null, purchase.getSku(), purchase.getPurchaseToken())
                            .addOnSuccessListener(data1 -> {
                                if (data1.purchaseStatus.ok) subscriptionCompleteOk();
                                else setRegisterError();
                            })
                            .addOnFailureListener(ex -> {
                                Log.e(TAG, "Failed updating purchase token.", ex);
                                setRegisterError();
                            });
                })
                .addOnFailureListener(ex -> {
                    Log.e(TAG, "Failed getting user data.", ex);
                    setRegisterError();
                });
    }


    private enum Status {
        FEATURES, SIGN_IN, USERNAME
    }

    private interface OnProviderClickListener {
        void onClick(@NonNull OverloadedSignInHelper.SignInProvider provider);
    }

    private interface SkuDetailsCallback {
        void onSuccess(@NonNull SkuDetails details);

        void onFailed(@BillingClient.BillingResponseCode int code);
    }
}
