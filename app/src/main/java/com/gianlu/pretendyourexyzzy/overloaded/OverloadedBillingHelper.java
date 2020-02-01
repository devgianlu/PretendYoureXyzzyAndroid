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
import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;

import java.util.Collections;
import java.util.List;

public final class OverloadedBillingHelper implements PurchasesUpdatedListener, OverloadedApi.PurchaseStatusCallback {
    private final Context context;
    private final Listener listener;
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
            OverloadedApi.get().purchaseStatus(new OverloadedApi.PurchaseStatusCallback() {
                @Override
                public void onPurchaseStatus(@NonNull OverloadedApi.Purchase status) {
                    purchase = status;
                    checkUpdateUi();

                    if (status.status == OverloadedApi.Purchase.Status.PENDING)
                        OverloadedApi.get().verifyPurchase(status.purchaseToken, OverloadedBillingHelper.this);
                }

                @Override
                public void onFailed(@NonNull Exception ex) {
                    Logging.log(ex);
                    purchase = null;
                }
            });
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
            } else {
                listener.toggleBuyOverloadedVisibility(true);
                listener.updateOverloadedStatusText(null);
            }
        } else {
            switch (purchase.status) {
                case OK:
                    listener.toggleBuyOverloadedVisibility(false);
                    listener.updateOverloadedStatusText(context.getString(R.string.overloadedStatus_ok));
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

    private void startBillingFlow(@NonNull Activity activity, @NonNull SkuDetails product) {
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(product)
                .build();

        BillingResult result = billingClient.launchBillingFlow(activity, flowParams);
        if (result.getResponseCode() != BillingClient.BillingResponseCode.OK)
            listener.handleBillingErrors(result.getResponseCode());
    }

    @NonNull
    public View.OnClickListener buyOverloadedOnClick(@NonNull ActivityWithDialog activity) {
        return v -> {
            if (OverloadedSignInHelper.isSignedIn()) {
                if (infiniteSku != null && billingClient != null && billingClient.isReady())
                    startBillingFlow(activity, infiniteSku);
            } else {
                activity.showDialog(OverloadedChooseProviderDialog.getSignInInstance());
            }
        };
    }

    @Override
    public void onPurchasesUpdated(BillingResult br, @Nullable List<Purchase> list) {
        if (br.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            listener.handleBillingErrors(br.getResponseCode());
            return;
        }

        if (list == null || list.isEmpty()) return;

        OverloadedApi.get().verifyPurchase(list.get(0).getPurchaseToken(), this);
    }

    @Override
    public void onPurchaseStatus(@NonNull OverloadedApi.Purchase status) {
        purchase = status;
        checkUpdateUi();
        listener.showToast(Toaster.build().message(R.string.purchaseVerified));
    }

    @Override
    public void onFailed(@NonNull Exception ex) {
        purchase = null;
        checkUpdateUi();
        listener.showToast(Toaster.build().message(R.string.failedBuying).ex(ex));
    }

    public void onDestroy() {
        if (billingClient != null) billingClient.endConnection();
    }

    public interface Listener {
        void handleBillingErrors(@BillingClient.BillingResponseCode int code);

        void showToast(@NonNull Toaster toaster);

        void toggleBuyOverloadedVisibility(boolean visible);

        void updateOverloadedStatusText(@Nullable String text);
    }
}
