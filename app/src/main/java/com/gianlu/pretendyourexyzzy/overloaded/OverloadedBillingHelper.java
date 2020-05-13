package com.gianlu.pretendyourexyzzy.overloaded;

import android.app.Activity;
import android.content.Context;
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
import com.google.firebase.auth.FirebaseAuth;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.callback.UserDataCallback;
import xyz.gianlu.pyxoverloaded.model.UserData;

public final class OverloadedBillingHelper implements PurchasesUpdatedListener, AskUsernameDialog.Listener {
    private static final String TAG = OverloadedBillingHelper.class.getSimpleName();
    private static final String OVERLOADED_INFINITE_SKU = "overloaded.infinite";
    private final Context context;
    private final Listener listener;
    private BillingClient billingClient;
    private Exception latestException = null;
    private UserData latestData = null;

    public OverloadedBillingHelper(@NonNull Context context, @NonNull Listener listener) {
        this.context = context;
        this.listener = listener;
    }

    ///////////////////////////////
    ////////// Activity ///////////
    ///////////////////////////////

    public void onStart() {
        billingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build();
        billingClient.startConnection(new BillingClientStateListener() {
            private boolean retried = false;

            @Override
            public void onBillingSetupFinished(BillingResult br) {
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
                    if (data.purchaseStatus == UserData.PurchaseStatus.OK) {
                        OverloadedApi.get().openWebSocket();
                    } else if ((purchase = getLatestPurchase()) != null) {
                        OverloadedApi.get().registerUser(null, purchase.getPurchaseToken(), null, new UserDataCallback() {
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
                    if (ex instanceof OverloadedApi.OverloadedServerException) {
                        if (((OverloadedApi.OverloadedServerException) ex).code == 403) {
                            FirebaseAuth.getInstance().signOut();
                            updateStatus(null, null);
                            return;
                        }
                    }

                    updateStatus(null, ex);
                }
            });
        }
    }

    public void onResume() {
        if (!isFirebaseLoggedIn()) updateStatus(null, null);
    }


    ///////////////////////////////
    /////////// Billing ///////////
    ///////////////////////////////

    public void onDestroy() {
        if (billingClient != null) billingClient.endConnection();
    }

    /**
     * Get the latest purchase of Overloaded (cached).
     *
     * @return The {@link Purchase} or {@code null} if none was present
     */
    @Nullable
    private Purchase getLatestPurchase() {
        List<Purchase> purchases = billingClient.queryPurchases(BillingClient.SkuType.INAPP).getPurchasesList();
        if (purchases == null || purchases.isEmpty()) return null;

        Purchase latestPurchase = null;
        for (Purchase p : purchases) {
            if (!OVERLOADED_INFINITE_SKU.equals(p.getSku()) || !BuildConfig.APPLICATION_ID.equals(p.getPackageName()))
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
                .setSkusList(Collections.singletonList(OVERLOADED_INFINITE_SKU))
                .setType(BillingClient.SkuType.INAPP).build(), (br, list) -> {
            int code = br.getResponseCode();
            if (code == BillingClient.BillingResponseCode.OK) callback.onSuccess(list.get(0));
            else callback.onFailed(code);
        });
    }

    /**
     * Starts the billing flow for the given SKU.
     *
     * @param product  The {@link SkuDetails} for this flow
     * @param activity The caller {@link Activity}
     */
    private void startBillingFlow(@NonNull Activity activity, @NonNull SkuDetails product) {
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
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
     *
     * @param activity The caller {@link Activity}
     */
    public void startBillingFlow(@NonNull Activity activity) {
        getSkuDetails(new SkuDetailsCallback() {
            @Override
            public void onSuccess(@NonNull SkuDetails details) {
                startBillingFlow(activity, details);
            }

            @Override
            public void onFailed(int code) {
                handleBillingErrors(code);
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
                if (data.purchaseStatus == UserData.PurchaseStatus.OK) {
                    listener.dismissDialog();
                    updateStatus(data, null);
                    return;
                }

                OverloadedApi.get().registerUser(null, purchase.getPurchaseToken(), null, new UserDataCallback() {
                    @Override
                    public void onUserData(@NonNull UserData data) {
                        listener.dismissDialog();
                        updateStatus(data, null);
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
                if (ex instanceof OverloadedApi.OverloadedServerException) {
                    if (((OverloadedApi.OverloadedServerException) ex).code == 403) {
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


    ///////////////////////////////
    ///////// Overloaded //////////
    ///////////////////////////////

    private boolean isFirebaseLoggedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    @Override
    public void onUsernameSelected(@NonNull String username) {
        Purchase purchase = getLatestPurchase();
        if (purchase == null) {
            Log.e(TAG, "Couldn't find latest purchase.");
            return;
        }

        listener.showProgress(R.string.loading);
        OverloadedApi.get().registerUser(username, purchase.getPurchaseToken(), null, new UserDataCallback() {
            @Override
            public void onUserData(@NonNull UserData data) {
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
                    case PURCHASE_PENDING:
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
        if (latestException instanceof OverloadedApi.MaintenanceException)
            return Status.MAINTENANCE;

        if (!isFirebaseLoggedIn()) {
            if (!billingClient.isReady())
                return Status.LOADING;

            return Status.NOT_SIGNED_IN;
        }

        if (latestData == null && latestException == null)
            return Status.LOADING;

        if (latestException instanceof OverloadedApi.TwoDevicesException)
            return Status.TWO_CLIENTS_ERROR;
        else if (latestException != null)
            return Status.ERROR;

        if (latestData.purchaseStatus == UserData.PurchaseStatus.OK)
            return Status.SIGNED_IN;
        else
            return Status.PURCHASE_PENDING;
    }

    @Contract(pure = true)
    public long maintenanceEstimatedEnd() {
        return latestException instanceof OverloadedApi.MaintenanceException ? ((OverloadedApi.MaintenanceException) latestException).estimatedEnd : -1;
    }

    public enum Status {
        LOADING, PURCHASE_PENDING, SIGNED_IN, NOT_SIGNED_IN, ERROR, MAINTENANCE, TWO_CLIENTS_ERROR
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
