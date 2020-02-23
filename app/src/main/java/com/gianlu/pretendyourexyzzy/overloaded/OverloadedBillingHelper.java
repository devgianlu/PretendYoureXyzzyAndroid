package com.gianlu.pretendyourexyzzy.overloaded;

import android.app.Activity;
import android.content.Context;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.BuildConfig;
import com.gianlu.pretendyourexyzzy.PK;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.overloaded.api.OverloadedApi;
import com.gianlu.pretendyourexyzzy.overloaded.api.OverloadedUtils;
import com.gianlu.pretendyourexyzzy.overloaded.api.UserDataCallback;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public final class OverloadedBillingHelper implements PurchasesUpdatedListener, UserDataCallback {
    private final Context context;
    private final Listener listener;
    public boolean wasBuying = false;
    private BillingClient billingClient;
    private volatile SkuDetails infiniteSku;
    private volatile OverloadedApi.UserData userData;
    private volatile Exception exception;
    private Status lastStatus;

    public OverloadedBillingHelper(@NonNull Context context, @NonNull Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void onStart() {
        billingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build();
        billingClient.startConnection(new BillingClientStateListener() {
            private boolean retried = false;

            @Override
            public void onBillingSetupFinished(BillingResult br) {
                if (br.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    getSkuDetails();
                    exception = null;
                } else {
                    exception = new IOException(br.getResponseCode() + ": " + br.getDebugMessage());
                    Logging.log(exception);
                }

                checkUpdateUi();
            }

            @Override
            public void onBillingServiceDisconnected() {
                if (!retried) {
                    retried = true;
                    billingClient.startConnection(this);
                } else {
                    listener.showToast(Toaster.build().message(R.string.failedBillingConnection));
                    exception = new IOException("Failed connecting to the billing service.");
                    checkUpdateUi();
                }
            }
        });

        if (OverloadedUtils.isSignedIn()) {
            if (!Prefs.getBoolean(PK.OVERLOADED_FINISHED_SETUP, false))
                listener.showProgress(R.string.verifyingPurchase);

            OverloadedApi.get().userData(null, new UserDataCallback() {
                @Override
                public void onUserData(@NonNull OverloadedApi.UserData data) {
                    userData = data;
                    exception = null;
                    checkUpdateUi();

                    if (data.purchaseStatus == OverloadedApi.UserData.PurchaseStatus.NONE && !data.purchaseToken.isEmpty()) {
                        OverloadedApi.get().verifyPurchase(data.purchaseToken, null, OverloadedBillingHelper.this);
                        return;
                    }

                    if (data.purchaseStatus == OverloadedApi.UserData.PurchaseStatus.PENDING) {
                        OverloadedApi.get().verifyPurchase(data.purchaseToken, null, OverloadedBillingHelper.this);
                    } else if (data.purchaseStatus == OverloadedApi.UserData.PurchaseStatus.OK) {
                        listener.dismissDialog();
                        if (data.hasUsername()) {
                            Prefs.putBoolean(PK.OVERLOADED_FINISHED_SETUP, true);
                            OverloadedApi.get().openWebSocket();
                        } else {
                            listener.showDialog(AskUsernameDialog.get());
                        }
                    } else {
                        List<Purchase> purchases = billingClient.queryPurchases(BillingClient.SkuType.INAPP).getPurchasesList();
                        if (purchases == null || purchases.isEmpty()) {
                            listener.dismissDialog();
                            return;
                        }

                        Purchase latestPurchase = null;
                        for (Purchase p : purchases) {
                            if (!p.getSku().equals("overloaded.infinite") || !p.getPackageName().equals(BuildConfig.APPLICATION_ID))
                                continue;

                            if (latestPurchase == null)
                                latestPurchase = p;
                            else if (latestPurchase.getPurchaseTime() < p.getPurchaseTime())
                                latestPurchase = p;
                        }

                        if (latestPurchase == null) {
                            listener.dismissDialog();
                            return;
                        }

                        OverloadedApi.get().verifyPurchase(latestPurchase.getPurchaseToken(), null, OverloadedBillingHelper.this);
                    }
                }

                @Override
                public void onFailed(@NonNull Exception ex) {
                    listener.dismissDialog();
                    Logging.log(ex);
                    userData = null;
                    exception = ex;
                    checkUpdateUi();
                }
            });
        }

        checkUpdateUi();
    }

    private void getSkuDetails() {
        billingClient.querySkuDetailsAsync(SkuDetailsParams.newBuilder()
                .setSkusList(Collections.singletonList("overloaded.infinite"))
                .setType(BillingClient.SkuType.INAPP).build(), (br, list) -> {
            if (br.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                infiniteSku = list.get(0);
                exception = null;
            } else {
                exception = new IOException(br.getResponseCode() + ": " + br.getDebugMessage());
                Logging.log(exception);
            }

            checkUpdateUi();
        });
    }

    public void onResume() {
        if (!OverloadedUtils.isSignedIn()) {
            userData = null;
            exception = null;
            checkUpdateUi();
        }
    }

    private synchronized void checkUpdateUi() {
        if (exception != null) {
            listener.updateOverloadedStatus(lastStatus = Status.ERROR, null);
            return;
        }

        if (OverloadedUtils.isSignedIn()) {
            if (userData == null) {
                listener.updateOverloadedStatus(lastStatus = Status.LOADING, null);
                return;
            }

            switch (userData.purchaseStatus) {
                case OK:
                    if (userData.hasUsername())
                        listener.updateOverloadedStatus(lastStatus = Status.SIGNED_IN, userData);
                    else
                        listener.updateOverloadedStatus(lastStatus = Status.LOADING, userData);
                    break;
                case NONE:
                    if (infiniteSku == null) {
                        listener.updateOverloadedStatus(lastStatus = Status.LOADING, null);
                        getSkuDetails();
                        return;
                    }

                    listener.updateOverloadedStatus(lastStatus = Status.NOT_BOUGHT, userData);
                    break;
                case PENDING:
                    listener.updateOverloadedStatus(lastStatus = Status.PURCHASE_PENDING, userData);
                    break;
                default:
                    listener.updateOverloadedStatus(lastStatus = Status.LOADING, null);
                    break;
            }
        } else {
            if (billingClient == null || !billingClient.isReady()) {
                listener.updateOverloadedStatus(lastStatus = Status.LOADING, null);
            } else {
                if (infiniteSku == null) {
                    listener.updateOverloadedStatus(lastStatus = Status.LOADING, null);
                    getSkuDetails();
                    return;
                }

                listener.updateOverloadedStatus(lastStatus = Status.NOT_SIGNED_IN, null);
            }
        }
    }

    private void handleBillingErrors(@BillingClient.BillingResponseCode int code) {
        switch (code) {
            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
            case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
            case BillingClient.BillingResponseCode.SERVICE_TIMEOUT:
                listener.showToast(Toaster.build().message(R.string.failedBillingConnection).extra(code));
                break;
            case BillingClient.BillingResponseCode.USER_CANCELED:
                listener.showToast(Toaster.build().message(R.string.userCancelled).extra(code));
                break;
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
            case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
            case BillingClient.BillingResponseCode.ITEM_NOT_OWNED:
            case BillingClient.BillingResponseCode.ERROR:
                listener.showToast(Toaster.build().message(R.string.failedBuying).extra(code));
                break;
            default:
            case BillingClient.BillingResponseCode.OK:
                break;
        }
    }

    private void startBillingFlow(@NonNull Activity activity, @NonNull SkuDetails product) {
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(product)
                .build();

        BillingResult result = billingClient.launchBillingFlow(activity, flowParams);
        if (result.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            listener.showProgress(R.string.verifyingPurchase);
            OverloadedApi.get().userData(activity, new UserDataCallback() {
                @Override
                public void onUserData(@NonNull OverloadedApi.UserData status) {
                    userData = status;
                    exception = null;
                    checkUpdateUi();
                    listener.dismissDialog();
                }

                @Override
                public void onFailed(@NonNull Exception ex) {
                    Logging.log(ex);
                    listener.dismissDialog();
                }
            });
        } else if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            handleBillingErrors(result.getResponseCode());
        }
    }

    public void startBillingFlow(@NonNull Activity activity) {
        if (infiniteSku != null && billingClient != null && billingClient.isReady())
            startBillingFlow(activity, infiniteSku);
    }

    @NonNull
    public CompoundButton.OnCheckedChangeListener toggleOverloaded(@NonNull Activity activity) {
        return (btn, isChecked) -> {
            // infiniteSku and userData have already been loaded
            if (isChecked) {
                if (OverloadedUtils.isSignedIn() && userData != null) {
                    switch (userData.purchaseStatus) {
                        case NONE:
                            btn.setChecked(false);
                            startBillingFlow(activity, infiniteSku);
                            break;
                        case OK:
                            listener.updateOverloadedMode(true, userData);
                            break;
                        default:
                        case PENDING: // Shouldn't be allowed to click when in this state
                            btn.setChecked(false);
                            break;
                    }
                } else {
                    btn.setChecked(false);
                    wasBuying = true;
                    listener.showDialog(OverloadedChooseProviderDialog.getSignInInstance());
                }
            } else {
                listener.updateOverloadedMode(false, null);
            }
        };
    }

    @Override
    public void onPurchasesUpdated(BillingResult br, @Nullable List<Purchase> list) {
        if (br.getResponseCode() != BillingClient.BillingResponseCode.OK &&
                br.getResponseCode() != BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            handleBillingErrors(br.getResponseCode());
            return;
        }

        if (list == null || list.isEmpty()) return;

        listener.showProgress(R.string.verifyingPurchase);
        OverloadedApi.get().verifyPurchase(list.get(0).getPurchaseToken(), null, this);
    }

    @Override
    public void onUserData(@NonNull OverloadedApi.UserData status) {
        userData = status;
        exception = null;
        checkUpdateUi();
        listener.dismissDialog();

        if (status.purchaseStatus == OverloadedApi.UserData.PurchaseStatus.OK) {
            listener.showToast(Toaster.build().message(R.string.purchaseVerified));
            if (status.hasUsername()) Prefs.putBoolean(PK.OVERLOADED_FINISHED_SETUP, true);
            else listener.showDialog(AskUsernameDialog.get());
        } else {
            listener.showToast(Toaster.build().message(R.string.failedVerifyingPurchase).extra(status));
        }
    }

    @Override
    public void onFailed(@NonNull Exception ex) {
        userData = null;
        exception = ex;
        checkUpdateUi();
        listener.dismissDialog();
        listener.showToast(Toaster.build().message(R.string.failedBuying).ex(ex));
    }

    public void onDestroy() {
        if (billingClient != null) billingClient.endConnection();
    }

    public synchronized void updatePurchase(@NonNull OverloadedApi.UserData status) {
        userData = status;
        exception = null;
        checkUpdateUi();
    }

    @Nullable
    public Status lastStatus() {
        return lastStatus;
    }

    public enum Status {
        LOADING, NOT_BOUGHT, PURCHASE_PENDING, SIGNED_IN, NOT_SIGNED_IN, ERROR
    }

    public interface Listener extends DialogUtils.ShowStuffInterface {
        void updateOverloadedStatus(@NonNull Status status, OverloadedApi.UserData data);

        void updateOverloadedMode(boolean enabled, OverloadedApi.UserData data);
    }
}
