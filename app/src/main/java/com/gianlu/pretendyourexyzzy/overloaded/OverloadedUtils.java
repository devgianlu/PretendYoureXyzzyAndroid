package com.gianlu.pretendyourexyzzy.overloaded;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.gianlu.pretendyourexyzzy.BuildConfig;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.google.android.gms.games.event.Event;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Pattern;

import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.Utils;
import xyz.gianlu.pyxoverloaded.model.UserData;

public final class OverloadedUtils {
    public static final String ACTION_OPEN_CHAT = "overloaded_open_chat";
    public static final String ACTION_SEND_CHAT = "overloaded_send_chat";
    public static final String ACTION_SHOW_PROFILE = "overloaded_show_profile";
    public static final String ACTION_ADD_FRIEND = "overloaded_add_friend";
    public static final String ACTION_REMOVE_FRIEND = "overloaded_remove_friend";
    public static final Sku ACTIVE_SKU = Sku.OVERLOADED_MONTHLY_SKU;
    private static final String TAG = OverloadedUtils.class.getSimpleName();

    private OverloadedUtils() {
    }

    @Nullable
    public static Event findEvent(@NonNull Iterable<Event> events, @NonNull String id) {
        for (Event ev : events)
            if (ev.getEventId().equals(id))
                return ev;

        return null;
    }

    public static boolean checkUsernameValid(@NonNull String str) {
        return Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{2,29}$").matcher(str).matches();
    }

    /**
     * @return Whether the user is signed in and fully registered.
     * @see OverloadedApi#isFullyRegistered()
     */
    public static boolean isSignedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null && OverloadedApi.get().isFullyRegistered();
    }

    /**
     * @return A task which will resolve to whether the user is signed in and fully registered or fail.
     */
    @NonNull
    public static Task<Boolean> waitReady() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null)
            return Tasks.forResult(false);

        return OverloadedApi.get().userData(true).continueWith(task -> {
            UserData data = task.getResult();
            return data != null && data.purchaseStatus.ok;
        });
    }

    @NonNull
    public static String getServerId(@NonNull Pyx.Server server) {
        return server.url.host() + ":" + server.url.port() + server.url.encodedPath();
    }

    @NonNull
    public static String getServeCustomDeckUrl(@NonNull String shareCode) {
        return Utils.overloadedServerUrl("ServeCustomDeck") + "?shareCode=" + shareCode;
    }

    @NonNull
    public static String getCardImageUrl(@NonNull String id) {
        return Utils.overloadedServerUrl("Images/GetCardImage") + "?id=" + id;
    }

    /**
     * Get the latest purchase of Overloaded (cached).
     *
     * @return The {@link Purchase} or {@code null} if none was present
     */
    @Nullable
    public static Purchase getLatestPurchase(BillingClient billingClient) {
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
     * Do initialization checks (assuming user data is recent and we are actually signed in)
     */
    public static void doInitChecks(@NonNull Context context) {
        BillingClient billingClient = BillingClient.newBuilder(context)
                .enablePendingPurchases()
                .setListener((billingResult, list) -> {
                })
                .build();

        TaskCompletionSource<BillingResult> bcTaskSource = new TaskCompletionSource<>();
        Task<BillingResult> bcTask = bcTaskSource.getTask();
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult br) {
                if (br.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    bcTaskSource.trySetResult(br);
                } else {
                    bcTaskSource.trySetResult(null);
                    billingClient.endConnection();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                bcTaskSource.trySetResult(null);
                billingClient.endConnection();
            }
        });

        OverloadedApi.get().userData(true)
                .addOnSuccessListener(data -> {
                    if (data.purchaseStatus.ok) {
                        OverloadedApi.get().openWebSocket();
                        OverloadedApi.chat(context).shareKeysIfNeeded();
                        Log.d(TAG, "Ok, init startup done.");
                    }

                    bcTask.addOnSuccessListener(br -> {
                        if (br == null) return;

                        Purchase purchase;
                        if ((purchase = getLatestPurchase(billingClient)) != null && (!data.purchaseStatus.ok || (data.expireTime != null && data.expireTime <= System.currentTimeMillis()))) {
                            OverloadedApi.get().registerUser(null, purchase.getSku(), purchase.getPurchaseToken())
                                    .addOnSuccessListener(updatedData -> {
                                        if (updatedData.purchaseStatus.ok) {
                                            OverloadedApi.get().openWebSocket();
                                            OverloadedApi.chat(context).shareKeysIfNeeded();
                                        }

                                        Log.i(TAG, "Updated purchase token.");
                                    })
                                    .addOnFailureListener(ex -> Log.e(TAG, "Failed updating purchase token.", ex));
                        }

                        billingClient.endConnection();
                    });
                })
                .addOnFailureListener(ex -> {
                    Log.e(TAG, "Failed getting user data.", ex);
                    billingClient.endConnection();
                });
    }

    public enum Sku {
        OVERLOADED_INFINITE_SKU("overloaded.infinite", BillingClient.SkuType.INAPP),
        OVERLOADED_MONTHLY_SKU("overloaded.monthly", BillingClient.SkuType.SUBS);

        public final String sku;
        public final String skuType;

        Sku(@NotNull String sku, @NotNull String skuType) {
            this.sku = sku;
            this.skuType = skuType;
        }
    }
}
