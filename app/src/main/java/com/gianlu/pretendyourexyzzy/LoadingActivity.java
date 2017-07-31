package com.gianlu.pretendyourexyzzy;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Logging;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.NetIO.Models.FirstLoad;
import com.gianlu.pretendyourexyzzy.NetIO.Models.User;
import com.gianlu.pretendyourexyzzy.NetIO.PYX;
import com.gianlu.pretendyourexyzzy.NetIO.PYXException;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;

import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.utils.URLEncodedUtils;

public class LoadingActivity extends AppCompatActivity implements PYX.IResult<FirstLoad> {
    private Intent goTo;
    private boolean finished = false;
    private ProgressBar loading;
    private LinearLayout register;
    private TextInputLayout registerNickname;
    private Button registerSubmit;
    private int launchGameId = -1;
    private String launchGamePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(this));

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
                if (goTo != null)
                    startActivity(goTo);
            }
        }, 1000);

        Logging.clearLogs(this);

        loading = (ProgressBar) findViewById(R.id.loading_loading);
        register = (LinearLayout) findViewById(R.id.loading_register);
        registerNickname = (TextInputLayout) findViewById(R.id.loading_registerNickname);
        registerSubmit = (Button) findViewById(R.id.loading_registerSubmit);
        Button changeServer = (Button) findViewById(R.id.loading_changeServer);
        changeServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedServer = CommonUtils.indexOf(PYX.Servers.values(), PYX.Servers.valueOf(Prefs.getString(LoadingActivity.this, Prefs.Keys.LAST_SERVER, PYX.Servers.PYX1.name())));
                AlertDialog.Builder builder = new AlertDialog.Builder(LoadingActivity.this);
                builder.setTitle(R.string.changeServer)
                        .setSingleChoiceItems(PYX.Servers.formalValues(), selectedServer, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                setServer(PYX.Servers.values()[which]);
                                recreate();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null);

                CommonUtils.showDialog(LoadingActivity.this, builder);
            }
        });

        if (Objects.equals(getIntent().getAction(), Intent.ACTION_VIEW) || Objects.equals(getIntent().getAction(), Intent.ACTION_SEND)) {
            Uri url = getIntent().getData();
            if (url != null) {
                PYX.Servers server = PYX.Servers.fromUrl(url.toString());
                if (server != null) setServer(server);

                String fragment = url.getFragment();
                if (fragment != null) {
                    List<NameValuePair> params = URLEncodedUtils.parse(fragment, Charset.forName("UTF-8"));
                    for (NameValuePair pair : params) {
                        if (Objects.equals(pair.getName(), "game")) {
                            try {
                                launchGameId = Integer.parseInt(pair.getValue());
                            } catch (NumberFormatException ex) {
                                Logging.logMe(this, ex);
                            }
                        } else if (Objects.equals(pair.getName(), "password")) {
                            launchGamePassword = pair.getValue();
                        }
                    }
                }
            }
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (CommonUtils.hasInternetAccess(false)) {
                    PYX.get(LoadingActivity.this).firstLoad(LoadingActivity.this);
                } else {
                    Toaster.show(LoadingActivity.this, Toaster.Message.OFFLINE, new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    });
                }
            }
        }).start();
    }

    private void setServer(PYX.Servers server) {
        PYX.invalidate();
        Prefs.putString(LoadingActivity.this, Prefs.Keys.LAST_SERVER, server.name());
    }

    @Override
    public void onDone(final PYX pyx, FirstLoad result) {
        if (result.nextOperation == FirstLoad.NextOp.REGISTER) {
            loading.setVisibility(View.GONE);
            register.setVisibility(View.VISIBLE);

            String lastNickname = Prefs.getString(LoadingActivity.this, Prefs.Keys.LAST_NICKNAME, null);
            if (lastNickname != null) //noinspection ConstantConditions
                registerNickname.getEditText().setText(lastNickname);

            registerSubmit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    @SuppressWarnings("ConstantConditions") String nick = registerNickname.getEditText().getText().toString();
                    pyx.registerUser(nick, new PYX.IResult<User>() {
                        @Override
                        public void onDone(PYX pyx, User result) {
                            pyx.startPolling();
                            Prefs.putString(LoadingActivity.this, Prefs.Keys.LAST_NICKNAME, result.nickname);
                            goTo(MainActivity.class, result);
                        }

                        @Override
                        public void onException(Exception ex) {
                            Logging.logMe(LoadingActivity.this, ex);
                            if (ex instanceof PYXException) {
                                switch (((PYXException) ex).errorCode) {
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
                                }
                            }

                            Toaster.show(LoadingActivity.this, Utils.Messages.FAILED_LOADING, ex);
                        }
                    });
                }
            });
        } else if (result.nextOperation == FirstLoad.NextOp.GAME) {
            launchGameId = result.gameId;
            goTo(MainActivity.class, new User(result.nickname));
        } else {
            pyx.startPolling();
            goTo(MainActivity.class, new User(result.nickname));
        }
    }

    @Override
    public void onException(Exception ex) {
        Toaster.show(LoadingActivity.this, Utils.Messages.FAILED_LOADING, ex, new Runnable() {
            @Override
            public void run() {
                finish();
            }
        });
    }

    private void goTo(Class goTo, @Nullable User user) {
        Intent intent = new Intent(LoadingActivity.this, goTo).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        if (user != null) intent.putExtra("user", user);
        if (launchGameId != -1) intent.putExtra("gid", launchGameId);
        if (launchGamePassword != null) intent.putExtra("password", launchGamePassword);
        if (finished) startActivity(intent);
        else this.goTo = intent;
    }
}
