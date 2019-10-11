package com.gianlu.pretendyourexyzzy;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
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
import com.gianlu.commonutils.logging.Logging;
import com.gianlu.commonutils.misc.RecyclerMessageView;
import com.gianlu.commonutils.misc.SuperTextView;
import com.gianlu.commonutils.preferences.Prefs;
import com.gianlu.commonutils.tutorial.BaseTutorial;
import com.gianlu.commonutils.tutorial.TutorialManager;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.Adapters.ServersAdapter;
import com.gianlu.pretendyourexyzzy.NetIO.FirstLoadedPyx;
import com.gianlu.pretendyourexyzzy.NetIO.Models.FirstLoad;
import com.gianlu.pretendyourexyzzy.NetIO.Models.GamePermalink;
import com.gianlu.pretendyourexyzzy.NetIO.NameValuePair;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.NetIO.PyxDiscoveryApi;
import com.gianlu.pretendyourexyzzy.NetIO.PyxException;
import com.gianlu.pretendyourexyzzy.NetIO.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.SpareActivities.ManageServersActivity;
import com.gianlu.pretendyourexyzzy.SpareActivities.TutorialActivity;
import com.gianlu.pretendyourexyzzy.Tutorial.Discovery;
import com.gianlu.pretendyourexyzzy.Tutorial.LoginTutorial;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import org.json.JSONObject;

import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;


public class LoadingActivity extends ActivityWithDialog implements Pyx.OnResult<FirstLoadedPyx>, TutorialManager.Listener {
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
                            } catch (NumberFormatException ex) {
                                Logging.log(ex);
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
                Logging.log(ex);
                welcomeMessage.setVisibility(View.GONE);
            }
        });
        discoveryApi.firstLoad(this, null, this);
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

        RecyclerMessageView rmv = new RecyclerMessageView(this);
        builder.setView(rmv);

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
                    Logging.log(ex);

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

                    Toaster.with(LoadingActivity.this).message(R.string.failedLoading).ex(ex).show();
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

        Toaster.with(this).message(R.string.failedLoading).ex(ex).show();
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
    public boolean buildSequence(@NonNull BaseTutorial tutorial) {
        tutorial.forView(registerNickname, R.string.tutorial_chooseNickname, R.string.tutorial_chooseNickname_desc);
        tutorial.forView(registerIdCode, R.string.tutorial_chooseIdCode, R.string.tutorial_chooseIdCode_desc);
        tutorial.forView(changeServer, R.string.tutorial_changeServer, R.string.tutorial_changeServer_desc);
        tutorial.forView(registerSubmit, R.string.tutorial_joinTheServer, R.string.tutorial_joinTheServer_desc);
        return true;
    }
}
