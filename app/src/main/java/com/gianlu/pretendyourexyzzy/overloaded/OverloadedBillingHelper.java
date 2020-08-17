package com.gianlu.pretendyourexyzzy.overloaded;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
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
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.BuildConfig;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.Utils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.OverloadedApi.OverloadedServerException;
import xyz.gianlu.pyxoverloaded.callback.UserDataCallback;
import xyz.gianlu.pyxoverloaded.model.UserData;

public final class OverloadedBillingHelper implements PurchasesUpdatedListener, AskUsernameDialog.Listener {
    public static final Sku ACTIVE_SKU = Sku.OVERLOADED_MONTHLY_SKU;
    private static final String TAG = OverloadedBillingHelper.class.getSimpleName();
    private final Activity activity;
    private final Listener listener;
    private BillingClient billingClient;
    private Exception latestException = null;
    private UserData latestData = null;

    public OverloadedBillingHelper(@NonNull Activity activity, @NonNull Listener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    public static void launchSubscriptions(@NotNull Context context, @Nullable String sku) {
        String url;
        if (sku == null)
            url = "https://play.google.com/store/account/subscriptions";
        else
            url = String.format("https://play.google.com/store/account/subscriptions?sku=%s&package=%s", sku, context.getApplicationContext().getPackageName());

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        context.startActivity(intent);
    }

    //region Activity
    public void onStart() {
        billingClient = BillingClient.newBuilder(activity).enablePendingPurchases().setListener(this).build();
        billingClient.startConnection(new BillingClientStateListener() {
            private boolean retried = false;

            @Override
            public void onBillingSetupFinished(@NotNull BillingResult br) {
                if (br.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    updateStatusFromBilling();
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

        updateStatus(null, null);
        if (isFirebaseLoggedIn()) {
            OverloadedApi.get().userData(null, new UserDataCallback() {
                @Override
                public void onUserData(@NonNull UserData data) {
                    updateStatus(data, null);

                    Purchase purchase;
                    if (data.purchaseStatus.ok) {
                        doOkStuff();
                    } else if ((purchase = getLatestPurchase()) != null) {
                        OverloadedApi.get().registerUser(null, purchase.getSku(), purchase.getPurchaseToken(), null, new UserDataCallback() {
                            @Override
                            public void onUserData(@NonNull UserData data) {
                                updateStatus(data, null);
                            }

                            @Override
                            public void onFailed(@NonNull Exception ex) {
                                Log.e(TAG, "Failed updating purchase token.", ex);
                                updateStatus(null, ex);
                            }
                        });
                    }
                }

                @Override
                public void onFailed(@NonNull Exception ex) {
                    if (ex instanceof OverloadedServerException) {
                        if (((OverloadedServerException) ex).reason.equals(OverloadedServerException.REASON_NOT_REGISTERED)) {
                            FirebaseAuth.getInstance().signOut();
                            updateStatus(null, null);
                            return;
                        }
                    }

                    Log.e(TAG, "Failed getting user data on start.", ex);
                    updateStatus(null, ex);
                }
            });
        }
    }

    public void onResume() {
        if (!isFirebaseLoggedIn()) updateStatus(null, null);
    }

    public void onDestroy() {
        if (billingClient != null) billingClient.endConnection();
    }
    //endregion

    //region Billing
    /**
     * Get the latest purchase of Overloaded (cached).
     *
     * @return The {@link Purchase} or {@code null} if none was present
     */
    @Nullable
    private Purchase getLatestPurchase() {
        if (billingClient == null || !billingClient.isReady())
            return null;

        List<Purchase> purchases = billingClient.queryPurchases(ACTIVE_SKU.skuType).getPurchasesList();
        if (purchases == null || purchases.isEmpty()) return null;

        Purchase latestPurchase = null;
        for (Purchase p : purchases) {
            if (!ACTIVE_SKU.sku.equals(p.getSku()) || !BuildConfig.APPLICATION_ID.equals(p.getPackageName()))
                continue;

            if (latestPurchase == null || latestPurchase.getPurchaseTime() < p.getPurchaseTime())
                latestPurchase = p;
        }

        return latestPurchase;
    }

    /**
     * Handles all billing failure response codes by showing a message.
     *
     * @param code The {@link com.android.billingclient.api.BillingClient.BillingResponseCode} integer
     */
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

    /**
     * Gets the SKU details for the standard purchase.
     *
     * @param callback The callback
     */
    private void getSkuDetails(@NonNull SkuDetailsCallback callback) {
        billingClient.querySkuDetailsAsync(SkuDetailsParams.newBuilder()
                .setSkusList(Collections.singletonList(ACTIVE_SKU.sku))
                .setType(ACTIVE_SKU.skuType).build(), (br, list) -> {
            int code = br.getResponseCode();
            if (code == BillingClient.BillingResponseCode.OK && list != null)
                callback.onSuccess(list.get(0));
            else
                callback.onFailed(code);
        });
    }

    /**
     * Starts the billing flow for the given SKU.
     *
     * @param product The {@link SkuDetails} for this flow
     */
    private void startBillingFlow(@NonNull SkuDetails product) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) throw new IllegalStateException();

        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setObfuscatedAccountId(Utils.sha1(user.getUid()))
                .setSkuDetails(product)
                .build();

        BillingResult result = billingClient.launchBillingFlow(activity, flowParams);
        if (result.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            Purchase purchase = getLatestPurchase();
            if (purchase == null) {
                Log.e(TAG, "Couldn't find latest purchase.");
                return;
            }

            purchaseComplete(purchase);
        } else if (result.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            handleBillingErrors(result.getResponseCode());
        }
    }

    /**
     * Starts the billing flow after getting the SKU details.
     */
    private void startBillingFlow() {
        getSkuDetails(new SkuDetailsCallback() {
            @Override
            public void onSuccess(@NonNull SkuDetails details) {
                listener.dismissDialog();
                startBillingFlow(details);
            }

            @Override
            public void onFailed(int code) {
                listener.dismissDialog();
                handleBillingErrors(code);
            }
        });
    }

    /**
     * Starts the general flow for registering.
     */
    public void startFlow() {
        listener.showProgress(R.string.loading);
        OverloadedApi.get().userData(activity, new UserDataCallback() {
            @Override
            public void onUserData(@NonNull UserData data) {
                if (data.purchaseStatus.ok) {
                    listener.dismissDialog();
                    updateStatus(data, null);
                    doOkStuff();
                } else {
                    startBillingFlow();
                }
            }

            @Override
            public void onFailed(@NonNull Exception ex) {
                listener.dismissDialog();
                if (ex instanceof OverloadedServerException) {
                    if (((OverloadedServerException) ex).reason.equals(OverloadedServerException.REASON_NOT_REGISTERED)) {
                        listener.showDialog(AskUsernameDialog.get());
                        // Follows in #onUsernameSelected(String)
                        return;
                    }
                }

                Log.e(TAG, "Failed getting user data, continuing flow.", ex);
                startBillingFlow();
            }
        });
    }

    @Override
    public void onPurchasesUpdated(@NotNull BillingResult br, @Nullable List<Purchase> list) {
        if (br.getResponseCode() != BillingClient.BillingResponseCode.OK &&
                br.getResponseCode() != BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            handleBillingErrors(br.getResponseCode());
            return;
        }

        if (list == null || list.isEmpty()) return;

        purchaseComplete(list.get(0));
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
        listener.showProgress(R.string.loading);
        OverloadedApi.get().userData(null, new UserDataCallback() {
            @Override
            public void onUserData(@NonNull UserData data) {
                if (data.purchaseStatus.ok) {
                    listener.dismissDialog();
                    updateStatus(data, null);
                    doOkStuff();
                    return;
                }

                OverloadedApi.get().registerUser(null, purchase.getSku(), purchase.getPurchaseToken(), null, new UserDataCallback() {
                    @Override
                    public void onUserData(@NonNull UserData data) {
                        listener.dismissDialog();
                        updateStatus(data, null);

                        if (data.purchaseStatus.ok)
                            doOkStuff();
                    }

                    @Override
                    public void onFailed(@NonNull Exception ex) {
                        listener.dismissDialog();
                        Log.e(TAG, "Failed updating purchase token.", ex);
                        updateStatus(null, ex);
                    }
                });
            }

            @Override
            public void onFailed(@NonNull Exception ex) {
                listener.dismissDialog();
                if (ex instanceof OverloadedServerException) {
                    if (((OverloadedServerException) ex).reason.equals(OverloadedServerException.REASON_NOT_REGISTERED)) {
                        listener.showDialog(AskUsernameDialog.get());
                        // Follows in #onUsernameSelected(String)
                        return;
                    }
                }

                Log.e(TAG, "Failed getting user data.", ex);
                updateStatus(null, ex);
            }
        });
    }
    //endregion

    private void doOkStuff() {
        OverloadedApi.get().openWebSocket();
        OverloadedApi.chat(activity).shareKeysIfNeeded();
    }

    private boolean isFirebaseLoggedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    @Override
    public void onUsernameSelected(@NonNull String username) {
        Purchase purchase = getLatestPurchase();

        listener.showProgress(R.string.loading);
        OverloadedApi.get().registerUser(username, purchase != null ? purchase.getSku() : null, purchase != null ? purchase.getPurchaseToken() : null, null, new UserDataCallback() {
            @Override
            public void onUserData(@NonNull UserData data) {
                if (data.purchaseStatus.ok) doOkStuff();
                else startBillingFlow();

                listener.dismissDialog();
                updateStatus(data, null);
            }

            @Override
            public void onFailed(@NonNull Exception ex) {
                listener.dismissDialog();
                Log.e(TAG, "Failed registering user.", ex);
                updateStatus(null, ex);
            }
        });
    }

    @NonNull
    public CompoundButton.OnCheckedChangeListener toggleOverloaded() {
        return (btn, isChecked) -> {
            if (isChecked) {
                Status status = getStatus();
                switch (status) {
                    case LOADING:
                    case ERROR:
                    case MAINTENANCE:
                    case TWO_CLIENTS_ERROR:
                        // Shouldn't even be pressable
                        btn.setChecked(false);
                        break;
                    case SIGNED_IN:
                        listener.updateOverloadedMode(true, latestData);
                        break;
                    case NOT_SIGNED_IN:
                        btn.setChecked(false);
                        listener.showDialog(OverloadedChooseProviderDialog.getSignInInstance());
                        break;
                }
            } else {
                listener.updateOverloadedMode(false, null);
            }
        };
    }

    @Contract("!null, !null -> fail")
    private void updateStatus(UserData data, Exception ex) {
        if (data != null && ex != null)
            throw new IllegalArgumentException();

        latestException = ex;
        latestData = data;

        listener.updateOverloadedStatus(getStatus(), data);
    }

    private void updateStatusFromBilling() {
        listener.updateOverloadedStatus(getStatus(), latestData);
    }

    /**
     * @return The {@link Status} of the client
     */
    @NonNull
    public Status getStatus() {
        if (OverloadedApi.get().isUnderMaintenance())
            return Status.MAINTENANCE;

        if (!isFirebaseLoggedIn()) {
            if (!billingClient.isReady())
                return Status.LOADING;

            return Status.NOT_SIGNED_IN;
        }

        if (latestData == null && latestException == null)
            return Status.LOADING;

        if (latestException instanceof OverloadedServerException && ((OverloadedServerException) latestException).reason.equals(OverloadedServerException.REASON_DEVICE_CONFLICT))
            return Status.TWO_CLIENTS_ERROR;
        else if (latestException != null)
            return Status.ERROR;

        if (latestData.purchaseStatus.ok)
            return Status.SIGNED_IN;
        else
            return Status.NOT_SIGNED_IN;
    }

    @Contract(pure = true)
    public long maintenanceEstimatedEnd() {
        return OverloadedApi.get().isUnderMaintenance() ? OverloadedApi.get().maintenanceEnd() : -1;
    }

    public enum Sku {
        OVERLOADED_INFINITE_SKU("overloaded.infinite", BillingClient.SkuType.INAPP),
        OVERLOADED_MONTHLY_SKU("overloaded.monthly", BillingClient.SkuType.SUBS);

        public final String sku;
        final String skuType;

        Sku(@NotNull String sku, @NotNull String skuType) {
            this.sku = sku;
            this.skuType = skuType;
        }
    }

    public enum Status {
        LOADING, SIGNED_IN, NOT_SIGNED_IN, ERROR, MAINTENANCE, TWO_CLIENTS_ERROR
    }

    public interface Listener extends DialogUtils.ShowStuffInterface {
        void updateOverloadedStatus(@NonNull Status status, UserData data);

        void updateOverloadedMode(boolean enabled, UserData data);
    }

    private interface SkuDetailsCallback {
        void onSuccess(@NonNull SkuDetails details);

        void onFailed(@BillingClient.BillingResponseCode int code);
    }
}
