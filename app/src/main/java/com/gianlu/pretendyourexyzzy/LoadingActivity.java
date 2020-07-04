package com.gianlu.pretendyourexyzzy;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.recyclerview.widget.RecyclerView;

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
import com.gianlu.pretendyourexyzzy.tutorial.Discovery;
import com.gianlu.pretendyourexyzzy.tutorial.LoginTutorial;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;

import me.toptas.fancyshowcase.FocusShape;


public class LoadingActivity extends ActivityWithDialog implements Pyx.OnResult<FirstLoadedPyx>, TutorialManager.Listener {
    private static final int GOOGLE_SIGN_IN_CODE = 3;
    private static final String TAG = LoadingActivity.class.getSimpleName();
    private Intent goTo;
    private boolean finished = false;
    private ProgressBar loading;
    private LinearLayout register;
    private TextInputLayout registerNickname;
    private Button registerSubmit;
    private GamePermalink launchGame = null;
    private String launchGamePassword;
    private Button changeServer;
    private boolean launchGameShouldRequest;
    private TextInputLayout registerIdCode;
    private TutorialManager tutorialManager;
    private SuperTextView welcomeMessage;
    private PyxDiscoveryApi discoveryApi;
    private TextView currentServer;

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

        new Handler().postDelayed(() -> {
            finished = true;
            if (goTo != null) startActivity(goTo);
        }, 1000);

        if (Prefs.getBoolean(PK.FIRST_RUN, true)) {
            startActivity(new Intent(this, TutorialActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            return;
        }

        Button preferences = findViewById(R.id.loading_preferences);
        preferences.setOnClickListener(v -> startActivity(new Intent(LoadingActivity.this, PreferenceActivity.class)));

        tutorialManager = new TutorialManager(this, Discovery.LOGIN);

        loading = findViewById(R.id.loading_loading);
        loading.getIndeterminateDrawable().setColorFilter(CommonUtils.resolveAttrAsColor(this, android.R.attr.textColorPrimary), PorterDuff.Mode.SRC_IN);
        currentServer = findViewById(R.id.loading_currentServer);
        register = findViewById(R.id.loading_register);
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

        signInSilently();
    }

    private void googleSignedIn(GoogleSignInAccount account) {
        if (account == null) return;

        Log.i(TAG, "Successfully logged in Google Play as " + Utils.getAccountName(account));
    }

    private void signInSilently() {
        GoogleSignInOptions signInOptions = GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN;
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (GoogleSignIn.hasPermissions(account, signInOptions.getScopeArray())) {
            googleSignedIn(account);
        } else {
            GoogleSignInClient signInClient = GoogleSignIn.getClient(this, signInOptions);
            signInClient.silentSignIn().addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    googleSignedIn(task.getResult());
                } else {
                    if (Prefs.getBoolean(PK.SHOULD_PROMPT_GOOGLE_PLAY, true)) {
                        Intent intent = signInClient.getSignInIntent();
                        startActivityForResult(intent, GOOGLE_SIGN_IN_CODE);
                    }
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == GOOGLE_SIGN_IN_CODE) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result == null) return;

            if (result.isSuccess()) {
                googleSignedIn(result.getSignInAccount());
            } else {
                if (result.getStatus().getStatusCode() == GoogleSignInStatusCodes.SIGN_IN_CANCELLED)
                    Prefs.putBoolean(PK.SHOULD_PROMPT_GOOGLE_PLAY, false);

                String msg = result.getStatus().getStatusMessage();
                if (msg != null && !msg.isEmpty())
                    Toaster.with(this).message(msg).show();
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
                loading.setVisibility(View.VISIBLE);
                register.setVisibility(View.GONE);
                dismissDialog();

                discoveryApi.firstLoad(LoadingActivity.this, null, LoadingActivity.this);
            }
        }));

        showDialog(builder);
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

    private void showRegisterUI(final FirstLoadedPyx pyx) {
        loading.setVisibility(View.GONE);
        register.setVisibility(View.VISIBLE);
        registerNickname.setErrorEnabled(false);
        registerIdCode.setErrorEnabled(false);

        String lastNickname = Prefs.getString(PK.LAST_NICKNAME, null);
        if (lastNickname != null) CommonUtils.setText(registerNickname, lastNickname);

        String lastIdCode = Prefs.getString(PK.LAST_ID_CODE, null);
        if (lastIdCode != null) CommonUtils.setText(registerIdCode, lastIdCode);

        if (!pyx.isServerSecure() && !pyx.config().insecureIdAllowed())
            registerIdCode.setEnabled(false);
        else
            registerIdCode.setEnabled(true);

        registerSubmit.setOnClickListener(v -> {
            loading.setVisibility(View.VISIBLE);
            register.setVisibility(View.GONE);

            String idCode = getIdCode();
            String nick = CommonUtils.getText(registerNickname);
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

                    loading.setVisibility(View.GONE);
                    register.setVisibility(View.VISIBLE);

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
            showRegisterUI(result);
            tutorialManager.tryShowingTutorials(this);
        }
    }

    @Override
    public void onException(@NonNull Exception ex) {
        if (ex instanceof PyxException) {
            if (Objects.equals(((PyxException) ex).errorCode, "se")) {
                loading.setVisibility(View.GONE);
                register.setVisibility(View.VISIBLE);

                return;
            }
        }

        Log.e(TAG, "Failed loading server.", ex);
        Toaster.with(this).message(R.string.failedLoading).show();
        changeServerDialog(false);
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("shouldRequest", launchGameShouldRequest);
        if (launchGame != null) intent.putExtra("game", launchGame);
        if (launchGamePassword != null) intent.putExtra("password", launchGamePassword);
        if (finished) startActivity(intent);
        else this.goTo = intent;
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
        t.add(t.forView(registerSubmit, R.string.tutorial_joinTheServer)
                .roundRectRadius(8)
                .enableAutoTextPosition()
                .focusShape(FocusShape.ROUNDED_RECTANGLE));
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (register != null)
            GPGamesHelper.setPopupView(this, (View) register.getParent(), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
    }
}
