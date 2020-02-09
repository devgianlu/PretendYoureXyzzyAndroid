package com.gianlu.pretendyourexyzzy.overloaded;

import android.app.Activity;
import android.content.Context;
import android.view.View;

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

import java.util.Collections;
import java.util.List;

public final class OverloadedBillingHelper implements PurchasesUpdatedListener, OverloadedApi.PurchaseStatusCallback {
    private final Context context;
    private final Listener listener;
    public boolean wasBuying = false;
    private BillingClient billingClient;
    private volatile SkuDetails infiniteSku;
    private volatile OverloadedApi.Purchase purchase;

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
                    checkUpdateUi();
                } else {
                    Logging.log(br.getResponseCode() + ": " + br.getDebugMessage(), true);
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                if (!retried) {
                    retried = true;
                    billingClient.startConnection(this);
                } else {
                    listener.showToast(Toaster.build().message(R.string.failedBillingConnection));
                    checkUpdateUi();
                }
            }
        });

        if (OverloadedSignInHelper.isSignedIn()) {
            if (!Prefs.getBoolean(PK.OVERLOADED_FINISHED_SETUP, false))
                listener.showProgress(R.string.verifyingPurchase);

            OverloadedApi.get().purchaseStatus(new OverloadedApi.PurchaseStatusCallback() {
                @Override
                public void onPurchaseStatus(@NonNull OverloadedApi.Purchase status) {
                    purchase = status;
                    checkUpdateUi();

                    if (status.status == OverloadedApi.Purchase.Status.NONE && !status.purchaseToken.isEmpty()) {
                        OverloadedApi.get().verifyPurchase(status.purchaseToken, OverloadedBillingHelper.this);
                        return;
                    }

                    if (status.status == OverloadedApi.Purchase.Status.PENDING) {
                        OverloadedApi.get().verifyPurchase(status.purchaseToken, OverloadedBillingHelper.this);
                    } else if (status.status == OverloadedApi.Purchase.Status.OK) {
                        listener.dismissDialog();
                        if (status.hasUsername())
                            Prefs.putBoolean(PK.OVERLOADED_FINISHED_SETUP, true);
                        else
                            listener.showDialog(AskUsernameDialog.get());
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

                        OverloadedApi.get().verifyPurchase(latestPurchase.getPurchaseToken(), OverloadedBillingHelper.this);
                    }
                }

                @Override
                public void onFailed(@NonNull Exception ex) {
                    listener.dismissDialog();
                    Logging.log(ex);
                    purchase = null;
                }
            });
        }
    }

    private void getSkuDetails() {
        billingClient.querySkuDetailsAsync(SkuDetailsParams.newBuilder()
                .setSkusList(Collections.singletonList("overloaded.infinite"))
                .setType(BillingClient.SkuType.INAPP).build(), (br1, list) -> {
            if (br1.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                if (!list.isEmpty()) {
                    infiniteSku = list.get(0);
                    checkUpdateUi();
                }
            } else {
                Logging.log(br1.getResponseCode() + ": " + br1.getDebugMessage(), true);
            }
        });
    }

    public void onResume() {
        if (!OverloadedSignInHelper.isSignedIn()) {
            purchase = null;
            checkUpdateUi();
        }
    }

    private synchronized void checkUpdateUi() {
        if (purchase == null) {
            if (billingClient == null || !billingClient.isReady()) {
                listener.toggleBuyOverloadedVisibility(false);
                listener.updateOverloadedStatusText(null);
                return;
            }

            if (infiniteSku == null) {
                getSkuDetails();
            } else {
                listener.toggleBuyOverloadedVisibility(true);
                listener.updateOverloadedStatusText(null);
            }
        } else {
            switch (purchase.status) {
                case OK:
                    if (purchase.hasUsername()) {
                        listener.toggleBuyOverloadedVisibility(false);
                        listener.updateOverloadedStatusText(context.getString(R.string.overloadedStatus_ok) + ": " + purchase.username);
                    } else {
                        listener.toggleBuyOverloadedVisibility(false);
                        listener.updateOverloadedStatusText(null);
                    }
                    break;
                case NONE:
                    listener.toggleBuyOverloadedVisibility(true);
                    listener.updateOverloadedStatusText(null);
                    break;
                case PENDING:
                    listener.toggleBuyOverloadedVisibility(false);
                    listener.updateOverloadedStatusText(context.getString(R.string.overloadedStatus_purchasePending));
                    break;
                default:
                    listener.toggleBuyOverloadedVisibility(false);
                    listener.updateOverloadedStatusText(null);
                    break;
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
            OverloadedApi.get().purchaseStatus(new OverloadedApi.PurchaseStatusCallback() {
                @Override
                public void onPurchaseStatus(@NonNull OverloadedApi.Purchase status) {
                    purchase = status;
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
    public View.OnClickListener buyOverloadedOnClick(@NonNull Activity activity) {
        return v -> {
            if (OverloadedSignInHelper.isSignedIn()) {
                if (infiniteSku == null) {
                    getSkuDetails();
                } else if (billingClient != null && billingClient.isReady()) {
                    startBillingFlow(activity, infiniteSku);
                }
            } else {
                wasBuying = true;
                listener.showDialog(OverloadedChooseProviderDialog.getSignInInstance());
            }
        };
    }

    @Override
    public void onPurchasesUpdated(BillingResult br, @Nullable List<Purchase> list) {
        if (br.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            handleBillingErrors(br.getResponseCode());
            return;
        }

        if (list == null || list.isEmpty()) return;

        listener.showProgress(R.string.verifyingPurchase);
        OverloadedApi.get().verifyPurchase(list.get(0).getPurchaseToken(), this);
    }

    @Override
    public void onPurchaseStatus(@NonNull OverloadedApi.Purchase status) {
        purchase = status;
        checkUpdateUi();
        listener.dismissDialog();

        if (status.status == OverloadedApi.Purchase.Status.OK) {
            listener.showToast(Toaster.build().message(R.string.purchaseVerified));
            if (status.hasUsername()) Prefs.putBoolean(PK.OVERLOADED_FINISHED_SETUP, true);
            else listener.showDialog(AskUsernameDialog.get());
        } else {
            listener.showToast(Toaster.build().message(R.string.failedVerifyingPurchase).extra(status));
        }
    }

    @Override
    public void onFailed(@NonNull Exception ex) {
        purchase = null;
        checkUpdateUi();
        listener.dismissDialog();
        listener.showToast(Toaster.build().message(R.string.failedBuying).ex(ex));
    }

    public void onDestroy() {
        if (billingClient != null) billingClient.endConnection();
    }

    public synchronized void updatePurchase(@NonNull OverloadedApi.Purchase status) {
        purchase = status;
        checkUpdateUi();
    }

    public interface Listener extends DialogUtils.ShowStuffInterface {
        void toggleBuyOverloadedVisibility(boolean visible);

        void updateOverloadedStatusText(@Nullable String text);
    }
}
