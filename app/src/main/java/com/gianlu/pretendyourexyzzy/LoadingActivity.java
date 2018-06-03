package com.gianlu.pretendyourexyzzy;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetSequence;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.ConnectivityChecker;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.NameValuePair;
import com.gianlu.commonutils.OfflineActivity;
import com.gianlu.commonutils.Preferences.Prefs;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.NetIO.FirstLoadedPyx;
import com.gianlu.pretendyourexyzzy.NetIO.Models.FirstLoad;
import com.gianlu.pretendyourexyzzy.NetIO.Pyx;
import com.gianlu.pretendyourexyzzy.NetIO.PyxException;
import com.gianlu.pretendyourexyzzy.NetIO.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.SpareActivities.ManageServersActivity;
import com.gianlu.pretendyourexyzzy.SpareActivities.TutorialActivity;

import java.security.SecureRandom;
import java.util.List;
import java.util.Objects;


public class LoadingActivity extends ActivityWithDialog implements Pyx.OnResult<FirstLoadedPyx> {
    private Intent goTo;
    private boolean finished = false;
    private ProgressBar loading;
    private LinearLayout register;
    private TextInputLayout registerNickname;
    private Button registerSubmit;
    private int launchGameId = -1;
    private String launchGamePassword;
    private Button changeServer;
    private boolean launchGameShouldRequest;
    private TextInputLayout registerIdCode;

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

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finished = true;
                if (goTo != null) startActivity(goTo);
            }
        }, 1000);

        Logging.clearLogs(this);

        if (Prefs.getBoolean(this, PKeys.FIRST_RUN, true)) {
            startActivity(new Intent(this, TutorialActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            return;
        }

        loading = findViewById(R.id.loading_loading);
        loading.getIndeterminateDrawable().setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary), PorterDuff.Mode.SRC_IN);
        register = findViewById(R.id.loading_register);
        registerNickname = findViewById(R.id.loading_registerNickname);
        registerSubmit = findViewById(R.id.loading_registerSubmit);
        registerIdCode = findViewById(R.id.loading_registerIdCode);

        changeServer = findViewById(R.id.loading_changeServer);
        changeServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeServerDialog(true);
            }
        });

        Button generateIdCode = findViewById(R.id.loading_generateIdCode);
        generateIdCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CommonUtils.setText(registerIdCode, CommonUtils.randomString(100, new SecureRandom()));
            }
        });

        if (Objects.equals(getIntent().getAction(), Intent.ACTION_VIEW) || Objects.equals(getIntent().getAction(), Intent.ACTION_SEND)) {
            Uri url = getIntent().getData();
            if (url != null) {
                Pyx.Server server = Pyx.Server.fromPyxUrl(url.toString());
                if (server != null) setServer(server);

                String fragment = url.getFragment();
                if (fragment != null) {
                    List<NameValuePair> params = CommonUtils.splitQuery(fragment);
                    for (NameValuePair pair : params) {
                        if (Objects.equals(pair.key(), "game")) {
                            try {
                                launchGameId = Integer.parseInt(pair.value(""));
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

        ConnectivityChecker.checkAsync(new ConnectivityChecker.OnCheck() {
            @Override
            public void goodToGo() {
                Pyx.get(LoadingActivity.this).firstLoad(LoadingActivity.this);
            }

            @Override
            public void offline() {
                OfflineActivity.startActivity(LoadingActivity.this, R.string.app_name, LoadingActivity.class);
            }
        });
    }

    private void changeServerDialog(boolean dismissible) {
        final List<Pyx.Server> availableServers = Pyx.Server.loadAllServers(this);

        int selectedServer = Pyx.Server.indexOf(availableServers, Prefs.getString(LoadingActivity.this, PKeys.LAST_SERVER, "PYX1"));
        if (selectedServer < 0) selectedServer = 0;

        CharSequence[] availableStrings = new CharSequence[availableServers.size()];
        for (int i = 0; i < availableStrings.length; i++)
            availableStrings[i] = availableServers.get(i).name;

        AlertDialog.Builder builder = new AlertDialog.Builder(LoadingActivity.this);
        builder.setTitle(R.string.changeServer)
                .setCancelable(dismissible)
                .setSingleChoiceItems(availableStrings, selectedServer, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        setServer(availableServers.get(which));
                        recreate();
                    }
                });

        builder.setNeutralButton(R.string.manage, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(LoadingActivity.this, ManageServersActivity.class));
                dialog.dismiss();
            }
        });

        if (dismissible)
            builder.setNegativeButton(android.R.string.cancel, null);

        showDialog(builder);
    }

    private void setServer(@NonNull Pyx.Server server) {
        Pyx.invalidate();
        Prefs.putString(LoadingActivity.this, PKeys.LAST_SERVER, server.name);
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

        String lastNickname = Prefs.getString(LoadingActivity.this, PKeys.LAST_NICKNAME, null);
        if (lastNickname != null) CommonUtils.setText(registerNickname, lastNickname);

        String lastIdCode = Prefs.getString(LoadingActivity.this, PKeys.LAST_ID_CODE, null);
        if (lastIdCode != null) CommonUtils.setText(registerIdCode, lastIdCode);

        registerSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loading.setVisibility(View.VISIBLE);
                register.setVisibility(View.GONE);

                final String idCode = getIdCode();
                if (idCode == null && !pyx.config().insecureIdAllowed) {
                    registerIdCode.setError(getString(R.string.mustProvideIdCode));
                    return;
                }

                String nick = CommonUtils.getText(registerNickname);
                pyx.register(nick, idCode, new Pyx.OnResult<RegisteredPyx>() {
                    @Override
                    public void onDone(@NonNull RegisteredPyx result) {
                        Prefs.putString(LoadingActivity.this, PKeys.LAST_NICKNAME, result.user().nickname);
                        Prefs.putString(LoadingActivity.this, PKeys.LAST_ID_CODE, idCode);
                        goTo(MainActivity.class);
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
            }
        });

        if (TutorialManager.shouldShowHintFor(this, TutorialManager.Discovery.LOGIN)) {
            new TapTargetSequence(this)
                    .target(Utils.tapTargetForView(registerNickname, R.string.tutorial_chooseNickname, R.string.tutorial_chooseNickname_desc))
                    .target(Utils.tapTargetForView(changeServer, R.string.tutorial_changeServer, R.string.tutorial_changeServer_desc))
                    .target(Utils.tapTargetForView(registerSubmit, R.string.tutorial_joinTheServer, R.string.tutorial_joinTheServer_desc))
                    .listener(new TapTargetSequence.Listener() {
                        @Override
                        public void onSequenceFinish() {
                            TutorialManager.setHintShown(LoadingActivity.this, TutorialManager.Discovery.LOGIN);
                        }

                        @Override
                        public void onSequenceStep(TapTarget lastTarget, boolean targetClicked) {
                        }

                        @Override
                        public void onSequenceCanceled(TapTarget lastTarget) {
                        }
                    }).start();
        }
    }

    @Override
    public void onDone(@NonNull FirstLoadedPyx result) {
        FirstLoad fl = result.firstLoad();
        if (fl.inProgress && fl.user != null) {
            if (fl.nextOperation == FirstLoad.NextOp.GAME) {
                launchGameId = fl.gameId;
                launchGameShouldRequest = false;
            }

            result.upgrade(fl.user);
            goTo(MainActivity.class);
        } else {
            showRegisterUI(result);
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

        Toaster.with(LoadingActivity.this).message(R.string.failedLoading).ex(ex).show();
        changeServerDialog(false);
    }

    private void goTo(Class goTo) {
        Intent intent = new Intent(LoadingActivity.this, goTo).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("shouldRequest", launchGameShouldRequest);
        if (launchGameId != -1) intent.putExtra("gid", launchGameId);
        if (launchGamePassword != null) intent.putExtra("password", launchGamePassword);
        if (finished) startActivity(intent);
        else this.goTo = intent;
    }
}
