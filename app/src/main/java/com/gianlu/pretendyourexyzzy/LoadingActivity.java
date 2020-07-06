package com.gianlu.pretendyourexyzzy;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.tutorial.BaseTutorial;
import com.gianlu.commonutils.tutorial.TutorialManager;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.activities.ManageServersActivity;
import com.gianlu.pretendyourexyzzy.activities.TutorialActivity;
import com.gianlu.pretendyourexyzzy.adapters.ServersAdapter;
import com.gianlu.pretendyourexyzzy.api.FirstLoadedPyx;
import com.gianlu.pretendyourexyzzy.api.NameValuePair;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.PyxDiscoveryApi;
import com.gianlu.pretendyourexyzzy.api.PyxException;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.FirstLoad;
import com.gianlu.pretendyourexyzzy.api.models.GamePermalink;
import com.gianlu.pretendyourexyzzy.overloaded.AskUsernameDialog;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedBillingHelper;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedBillingHelper.Status;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedChooseProviderDialog;
import com.gianlu.pretendyourexyzzy.overloaded.OverloadedSignInHelper;
import com.gianlu.pretendyourexyzzy.tutorial.Discovery;
import com.gianlu.pretendyourexyzzy.tutorial.LoginTutorial;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseUser;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import me.toptas.fancyshowcase.FocusShape;
import xyz.gianlu.pyxoverloaded.OverloadedApi;
import xyz.gianlu.pyxoverloaded.model.UserData;
import xyz.gianlu.pyxoverloaded.model.UserData.PurchaseStatusGranular;


public class LoadingActivity extends ActivityWithDialog implements Pyx.OnResult<FirstLoadedPyx>, TutorialManager.Listener, OverloadedChooseProviderDialog.Listener, OverloadedBillingHelper.Listener, AskUsernameDialog.Listener {
    private static final int RC_SIGN_IN = 3;
    private static final String TAG = LoadingActivity.class.getSimpleName();
    private final OverloadedSignInHelper signInHelper = new OverloadedSignInHelper();
    private final OverloadedBillingHelper billingHelper = new OverloadedBillingHelper(this, this);
    private TextInputLayout registerNickname;
    private Button registerSubmit;
    private GamePermalink launchGame = null;
    private String launchGamePassword;
    private ImageButton changeServer;
    private boolean launchGameShouldRequest;
    private ShimmerFrameLayout serverLoading;
    private TextInputLayout registerIdCode;
    private TutorialManager tutorialManager;
    private SuperTextView welcomeMessage;
    private PyxDiscoveryApi discoveryApi;
    private TextView currentServer;
    private ShimmerFrameLayout overloadedLoading;
    private ShimmerFrameLayout inputLoading;
    private TextView overloadedStatus;
    private ImageButton overloadedWarning;
    private SwitchMaterial overloadedToggle;
    private boolean waitingOverloaded = false;

    @Override
    protected void onStart() {
        super.onStart();
        billingHelper.onStart();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_loading);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) actionBar.hide();

        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);

        if (Prefs.getBoolean(PK.FIRST_RUN, true)) {
            startActivity(new Intent(this, TutorialActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            return;
        }

        Button preferences = findViewById(R.id.loading_preferences);
        preferences.setOnClickListener(v -> startActivity(new Intent(LoadingActivity.this, PreferenceActivity.class)));

        tutorialManager = new TutorialManager(this, Discovery.LOGIN);

        inputLoading = findViewById(R.id.loading_inputLoading);
        serverLoading = findViewById(R.id.loading_serverLoading);
        overloadedLoading = findViewById(R.id.loading_overloadedLoading);
        overloadedToggle = findViewById(R.id.loading_overloadedToggle);
        overloadedToggle.setOnCheckedChangeListener(billingHelper.toggleOverloaded());
        overloadedStatus = findViewById(R.id.loading_overloadedStatus);
        overloadedWarning = findViewById(R.id.loading_overloadedWarning);
        currentServer = findViewById(R.id.loading_currentServer);
        registerNickname = findViewById(R.id.loading_registerNickname);
        registerSubmit = findViewById(R.id.loading_registerSubmit);
        registerIdCode = findViewById(R.id.loading_registerIdCode);
        welcomeMessage = findViewById(R.id.loading_welcomeMsg);

        changeServer = findViewById(R.id.loading_changeServer);
        changeServer.setOnClickListener(v -> changeServerDialog(true));

        registerIdCode.setEndIconOnClickListener(v -> CommonUtils.setText(registerIdCode, CommonUtils.randomString(100, new SecureRandom())));
        CommonUtils.clearErrorOnEdit(registerIdCode);

        if (Objects.equals(getIntent().getAction(), Intent.ACTION_VIEW) || Objects.equals(getIntent().getAction(), Intent.ACTION_SEND)) {
            Uri url = getIntent().getData();
            if (url != null) {
                Pyx.Server server = Pyx.Server.fromUrl(url);
                if (server != null) setServer(server);

                String fragment = url.getFragment();
                if (fragment != null) {
                    List<NameValuePair> params = Utils.splitQuery(fragment);
                    for (NameValuePair pair : params) {
                        if (Objects.equals(pair.key(), "game")) {
                            try {
                                launchGame = new GamePermalink(Integer.parseInt(pair.value("")), new JSONObject()); // A bit hacky
                            } catch (NumberFormatException | JSONException ignored) {
                            }
                        } else if (Objects.equals(pair.key(), "password")) {
                            launchGamePassword = pair.value("");
                        }
                    }

                    launchGameShouldRequest = true;
                }
            }
        }

        String lastNickname = Prefs.getString(PK.LAST_NICKNAME, null);
        if (lastNickname != null) CommonUtils.setText(registerNickname, lastNickname);

        String lastIdCode = Prefs.getString(PK.LAST_ID_CODE, null);
        if (lastIdCode != null) CommonUtils.setText(registerIdCode, lastIdCode);

        Pyx.Server lastServer = Pyx.Server.lastServerNoThrow();
        if (lastServer != null) currentServer.setText(lastServer.name);

        discoveryApi = PyxDiscoveryApi.get();
        discoveryApi.getWelcomeMessage(this, new Pyx.OnResult<String>() {
            @Override
            public void onDone(@NonNull String result) {
                if (result.isEmpty()) {
                    welcomeMessage.setVisibility(View.GONE);
                } else {
                    welcomeMessage.setVisibility(View.VISIBLE);
                    welcomeMessage.setHtml(result);
                }
            }

            @Override
            public void onException(@NonNull Exception ex) {
                Log.e(TAG, "Failed loading welcome message.", ex);
                welcomeMessage.setVisibility(View.GONE);
            }
        });
        discoveryApi.firstLoad(this, null, this);
    }

    @Override
    public void onSelectedSignInProvider(@NonNull OverloadedSignInHelper.SignInProvider provider) {
        startActivityForResult(signInHelper.startFlow(this, provider), RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == RC_SIGN_IN) {
            if (data != null) {
                showProgress(R.string.loading);
                signInHelper.processSignInData(data, new OverloadedSignInHelper.SignInCallback() {
                    @Override
                    public void onSignInSuccessful(@NonNull FirebaseUser user) {
                        dismissDialog();
                        showToast(Toaster.build().message(R.string.signInSuccessful));
                        billingHelper.startBillingFlow(LoadingActivity.this, user.getUid());
                    }

                    @Override
                    public void onSignInFailed() {
                        dismissDialog();
                        showToast(Toaster.build().message(R.string.failedSigningIn));
                    }
                });
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void changeServerDialog(boolean dismissible) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.changeServer)
                .setCancelable(dismissible)
                .setNeutralButton(R.string.manage, (dialog, which) -> {
                    startActivity(new Intent(this, ManageServersActivity.class));
                    dialog.dismiss();
                });

        if (dismissible)
            builder.setNegativeButton(android.R.string.cancel, null);

        LinearLayout layout = (LinearLayout) getLayoutInflater().inflate(R.layout.activity_dialog_manage_servers, null, false);
        builder.setView(layout);

        RecyclerMessageView rmv = layout.findViewById(R.id.manageServers_list);
        rmv.disableSwipeRefresh();
        rmv.linearLayoutManager(RecyclerView.VERTICAL, false);
        rmv.dividerDecoration(RecyclerView.VERTICAL);
        rmv.loadListData(new ServersAdapter(this, Pyx.Server.loadAllServers(), new ServersAdapter.Listener() {
            @Override
            public void shouldUpdateItemCount(int count) {
            }

            @Override
            public void serverSelected(@NonNull Pyx.Server server) {
                setServer(server);
                toggleLoading(true);
                dismissDialog();

                discoveryApi.firstLoad(LoadingActivity.this, null, LoadingActivity.this);
            }
        }));

        showDialog(builder);
    }

    private void toggleLoading(boolean val) {
        registerSubmit.setEnabled(!val);
        registerSubmit.setClickable(!val);
        if (val) {
            inputLoading.showShimmer(true);
            serverLoading.showShimmer(true);
            overloadedLoading.showShimmer(true);
        } else {
            inputLoading.hideShimmer();
            serverLoading.hideShimmer();
            if (billingHelper.getStatus() != Status.LOADING)
                overloadedLoading.hideShimmer();
        }
    }

    private void setServer(@NonNull Pyx.Server server) {
        Pyx.invalidate();
        Prefs.putString(PK.LAST_SERVER, server.name);

        currentServer.setText(server.name);
    }

    @Nullable
    private String getIdCode() {
        String id = CommonUtils.getText(registerIdCode).trim();
        return id.isEmpty() ? null : id;
    }

    private void showRegisterUi(@NotNull FirstLoadedPyx pyx) {
        toggleLoading(false);
        registerNickname.setErrorEnabled(false);
        registerIdCode.setErrorEnabled(false);

        if (!pyx.isServerSecure() && !pyx.config().insecureIdAllowed())
            registerIdCode.setEnabled(false);
        else
            registerIdCode.setEnabled(true);

        registerSubmit.setOnClickListener(v -> {
            toggleLoading(true);

            String idCode = getIdCode();
            String nick = CommonUtils.getText(registerNickname);

            if (billingHelper.getStatus() == Status.SIGNED_IN && !overloadedToggle.isChecked() && nick.equals(OverloadedApi.get().username()))
                overloadedToggle.setChecked(true);

            pyx.register(nick, idCode, this, new Pyx.OnResult<RegisteredPyx>() {
                @Override
                public void onDone(@NonNull RegisteredPyx result) {
                    Prefs.putString(PK.LAST_NICKNAME, result.user().nickname);
                    Prefs.putString(PK.LAST_ID_CODE, idCode);
                    goToMain();
                }

                @Override
                public void onException(@NonNull Exception ex) {
                    Log.e(TAG, "Failed registering on server.", ex);

                    toggleLoading(false);

                    if (ex instanceof PyxException) {
                        switch (((PyxException) ex).errorCode) {
                            case "rn":
                                registerNickname.setError(getString(R.string.reservedNickname));
                                return;
                            case "in":
                                registerNickname.setError(getString(R.string.invalidNickname));
                                return;
                            case "niu":
                                registerNickname.setError(getString(R.string.alreadyUsedNickname));
                                return;
                            case "tmu":
                                registerNickname.setError(getString(R.string.tooManyUsers));
                                return;
                            case "iid":
                                registerIdCode.setError(getString(R.string.invalidIdCode));
                                return;
                        }
                    }

                    Log.e(TAG, "Failed registering user on server.", ex);
                    Toaster.with(LoadingActivity.this).message(R.string.failedLoading).show();
                }
            });
        });
    }

    @Override
    public void onDone(@NonNull FirstLoadedPyx result) {
        FirstLoad fl = result.firstLoad();
        if (fl.inProgress && fl.user != null) {
            if (fl.nextOperation == FirstLoad.NextOp.GAME) {
                launchGame = fl.game;
                launchGameShouldRequest = false;
            }

            result.upgrade(fl.user);
            goToMain();
        } else {
            currentServer.setText(result.server.name);
            showRegisterUi(result);
            tutorialManager.tryShowingTutorials(this);
        }
    }

    @Override
    public void onException(@NonNull Exception ex) {
        if (ex instanceof PyxException) {
            if (Objects.equals(((PyxException) ex).errorCode, "se")) {
                toggleLoading(false);
                return;
            }
        }

        Log.e(TAG, "Failed loading server.", ex);
        Toaster.with(this).message(R.string.failedLoading).show();
        changeServerDialog(false);
    }

    private void goToMain() {
        if (billingHelper.getStatus() == Status.LOADING) {
            if (waitingOverloaded) {
                goToMain();
                waitingOverloaded = false;
            } else {
                waitingOverloaded = true;
            }
            return;
        }

        waitingOverloaded = false;

        Intent intent = new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("shouldRequest", launchGameShouldRequest);
        if (launchGame != null) intent.putExtra("game", launchGame);
        if (launchGamePassword != null) intent.putExtra("password", launchGamePassword);
        startActivity(intent);
        waitingOverloaded = false;
    }

    @Override
    public boolean canShow(@NonNull BaseTutorial tutorial) {
        return tutorial instanceof LoginTutorial;
    }

    @Override
    public boolean buildSequence(@NonNull BaseTutorial t) {
        t.add(t.forView(registerNickname, R.string.tutorial_chooseNickname)
                .enableAutoTextPosition()
                .roundRectRadius(8)
                .focusShape(FocusShape.ROUNDED_RECTANGLE));
        t.add(t.forView(registerIdCode, R.string.tutorial_chooseIdCode)
                .enableAutoTextPosition()
                .roundRectRadius(8)
                .focusShape(FocusShape.ROUNDED_RECTANGLE));
        t.add(t.forView(changeServer, R.string.tutorial_changeServer)
                .roundRectRadius(8)
                .enableAutoTextPosition()
                .focusShape(FocusShape.ROUNDED_RECTANGLE));
        t.add(t.forView(overloadedLoading, R.string.tutorial_overloaded)
                .roundRectRadius(8)
                .enableAutoTextPosition()
                .focusShape(FocusShape.ROUNDED_RECTANGLE));
        t.add(t.forView(registerSubmit, R.string.tutorial_joinTheServer)
                .roundRectRadius(8)
                .enableAutoTextPosition()
                .focusShape(FocusShape.ROUNDED_RECTANGLE));
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        billingHelper.onResume();
        GPGamesHelper.setPopupView(this, Gravity.CENTER_HORIZONTAL | Gravity.TOP);
    }

    @Override
    public void updateOverloadedStatus(@NonNull Status status, UserData data) {
        if (overloadedLoading == null || overloadedStatus == null || overloadedToggle == null || overloadedWarning == null)
            return;

        if (status != Status.LOADING && waitingOverloaded) {
            goToMain();
            return;
        }

        if (data != null && data.purchaseStatusGranular.message) {
            overloadedWarning.setVisibility(View.VISIBLE);
            overloadedWarning.setOnClickListener(v -> showOverloadedWarningDialog(OverloadedBillingHelper.ACTIVE_SKU.sku, data.purchaseStatusGranular, data.expireTime));
        } else {
            overloadedWarning.setVisibility(View.GONE);
        }

        switch (status) {
            case MAINTENANCE:
                overloadedLoading.hideShimmer();
                overloadedStatus.setText(getString(R.string.overloadedStatus_maintenance, new SimpleDateFormat("HH:mm", Locale.getDefault()).format(billingHelper.maintenanceEstimatedEnd())));
                overloadedToggle.setEnabled(false);
                overloadedToggle.setChecked(false);
                break;
            case TWO_CLIENTS_ERROR:
                overloadedLoading.hideShimmer();
                overloadedStatus.setText(R.string.overloadedStatus_twoDevices);
                overloadedToggle.setEnabled(false);
                overloadedToggle.setChecked(false);
                break;
            case ERROR:
                overloadedLoading.hideShimmer();
                overloadedStatus.setText(R.string.overloadedStatus_error);
                overloadedToggle.setEnabled(false);
                overloadedToggle.setChecked(false);
                break;
            case LOADING:
                overloadedLoading.showShimmer(true);
                break;
            case SIGNED_IN:
                overloadedLoading.hideShimmer();
                overloadedStatus.setText(getString(R.string.loggedInAs, data.username));
                overloadedToggle.setEnabled(true);
                overloadedToggle.setChecked(Prefs.getBoolean(PK.OVERLOADED_LAST_ENABLED, false));
                break;
            case NOT_SIGNED_IN:
                overloadedLoading.hideShimmer();
                overloadedStatus.setText(R.string.overloaded_notSignedIn);
                overloadedToggle.setEnabled(true);
                overloadedToggle.setChecked(false);
                break;
            default:
                throw new IllegalStateException("Unknown status: " + status);
        }

        overloadedStatus.setVisibility(status == Status.LOADING ? View.GONE : View.VISIBLE);
        overloadedStatus.setTextColor(status == Status.ERROR ? ContextCompat.getColor(this, R.color.red) : CommonUtils.resolveAttrAsColor(this, android.R.attr.textColorSecondary));
    }

    @Override
    public void updateOverloadedMode(boolean enabled, UserData data) {
        if (registerNickname == null || overloadedToggle == null) return;

        if (enabled) {
            if (data == null) {
                overloadedToggle.setChecked(false);
                return;
            }

            String nickname = CommonUtils.getText(registerNickname);
            if (Objects.equals(nickname, data.username)) {
                registerNickname.setEnabled(false);
                Prefs.putBoolean(PK.OVERLOADED_LAST_ENABLED, true);
                return;
            }

            overloadedToggle.setChecked(false);
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
            builder.setTitle(R.string.overloadedDifferentUsername_title)
                    .setMessage(R.string.overloadedDifferentUsername_message)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.change, (dialog, which) -> {
                        CommonUtils.setText(registerNickname, data.username);
                        overloadedToggle.setChecked(true);
                    });

            showDialog(builder);
        } else {
            Prefs.putBoolean(PK.OVERLOADED_LAST_ENABLED, false);
            registerNickname.setEnabled(true);
        }
    }

    private void showOverloadedWarningDialog(@NotNull String sku, @NotNull PurchaseStatusGranular status, @Nullable Long expireTimeMillis) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(status.getName()).setMessage(status.getMessage(this, expireTimeMillis));

        if (status == PurchaseStatusGranular.PAUSED) {
            builder.setNeutralButton(R.string.resume, (dialog, which) -> OverloadedBillingHelper.launchSubscriptions(this, sku));
        } else if (status == PurchaseStatusGranular.ACCOUNT_HOLD || status == PurchaseStatusGranular.GRACE_PERIOD) {
            builder.setNeutralButton(R.string.fixPayment, (dialog, which) -> OverloadedBillingHelper.launchSubscriptions(this, sku));
        } else {
            builder.setNeutralButton(R.string.subscriptions, (dialog, which) -> OverloadedBillingHelper.launchSubscriptions(this, null));
        }

        showDialog(builder);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        billingHelper.onDestroy();
    }

    @Override
    public void onUsernameSelected(@NonNull String username) {
        billingHelper.onUsernameSelected(username);
    }
}
