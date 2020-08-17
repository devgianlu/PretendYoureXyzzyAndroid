package com.gianlu.pretendyourexyzzy.metrics;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.commonutils.dialogs.DialogUtils;
import com.gianlu.commonutils.misc.BreadcrumbsView;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.Pyx;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.GamePermalink;
import com.gianlu.pretendyourexyzzy.api.models.metrics.GameHistory;
import com.gianlu.pretendyourexyzzy.api.models.metrics.SessionHistory;
import com.gianlu.pretendyourexyzzy.api.models.metrics.SimpleRound;
import com.gianlu.pretendyourexyzzy.api.models.metrics.UserHistory;
import com.gianlu.pretendyourexyzzy.dialogs.GameRoundDialog;

import java.util.Date;

public class MetricsActivity extends ActivityWithDialog implements BreadcrumbsView.Listener, UserHistoryAdapter.Listener, GamesAdapter.Listener, RoundsAdapter.Listener {
    private static final int TYPE_USER = 0;
    private static final int TYPE_SESSION = 1;
    private static final int TYPE_GAME = 2;
    private static final String TAG_USER = UserHistoryFragment.class.getName();
    private static final String TAG_SESSION = SessionHistoryFragment.class.getName();
    private static final String TAG_GAME = GameHistoryFragment.class.getName();
    private static final String TAG_ROUND = GameRoundDialog.class.getName();
    private static final String TAG = MetricsActivity.class.getSimpleName();
    private RegisteredPyx pyx;
    private BreadcrumbsView breadcrumbs;

    public static void startActivity(Context context) {
        startActivity(context, null);
    }

    public static void startActivity(Context context, @Nullable GamePermalink game) {
        context.startActivity(new Intent(context, MetricsActivity.class)
                .putExtra("game", game));
    }

    private void loadUserHistory() {
        breadcrumbs.clear();
        breadcrumbs.addItem(new BreadcrumbsView.Item(pyx.user().nickname, TYPE_USER));

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.metrics_container, UserHistoryFragment.get(), TAG_USER)
                .commit();
    }

    private void loadSession(@NonNull UserHistory.Session session) {
        breadcrumbs.addItem(new BreadcrumbsView.Item(session.name(), TYPE_SESSION, session));

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.metrics_container, SessionHistoryFragment.get(session.id), TAG_SESSION)
                .commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metrics);
        setSupportActionBar(findViewById(R.id.metrics_toolbar));
        setTitle(R.string.metrics);

        ActionBar bar = getSupportActionBar();
        if (bar != null) bar.setDisplayHomeAsUpEnabled(true);

        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            Toaster.with(this).message(R.string.failedLoading).show();
            onBackPressed();
            return;
        }

        breadcrumbs = findViewById(R.id.metrics_breadcrumbs);
        breadcrumbs.clear();
        breadcrumbs.setListener(this);

        GamePermalink game = (GamePermalink) getIntent().getSerializableExtra("game");
        if (game == null) {
            loadUserHistory();
        } else {
            loadGame(game);
        }
    }

    private void loadGame(@NonNull GamePermalink game) {
        final String gameId = game.extractGameMetricsId();
        if (gameId == null) {
            Toaster.with(this).message(R.string.failedLoading).show();
            onBackPressed();
            return;
        }

        ProgressDialog pd = DialogUtils.progressDialog(this, R.string.loading);
        showDialog(pd);
        pyx.getGameHistory(gameId, this, new Pyx.OnResult<GameHistory>() {
            @Override
            public void onDone(@NonNull GameHistory result) {
                dismissDialog();

                breadcrumbs.addItem(new BreadcrumbsView.Item(getString(R.string.ongoingGame), TYPE_GAME, null));

                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.metrics_container, GameHistoryFragment.get(result), TAG_GAME)
                        .commit();
            }

            @Override
            public void onException(@NonNull Exception ex) {
                dismissDialog();
                Log.e(TAG, "Failed loading history.", ex);
                Toaster.with(MetricsActivity.this).message(R.string.failedLoading).show();
                onBackPressed();
            }
        });
    }

    private void loadGame(@NonNull final SessionHistory.Game game) {
        breadcrumbs.addItem(new BreadcrumbsView.Item(CommonUtils.getFullDateFormatter().format(new Date(game.timestamp)), TYPE_GAME, game));

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.metrics_container, GameHistoryFragment.get(game.id), TAG_GAME)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (breadcrumbs == null || breadcrumbs.navigateBack())
            super.onBackPressed();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSegmentSelected(@NonNull BreadcrumbsView.Item item) {
        breadcrumbs.removeFrom(item);

        switch (item.type) {
            case TYPE_USER:
                loadUserHistory();
                break;
            case TYPE_SESSION:
                if (item.data != null) loadSession((UserHistory.Session) item.data);
                break;
            case TYPE_GAME:
                if (item.data != null) loadGame((SessionHistory.Game) item.data);
                break;
        }
    }

    @Override
    public void onSessionSelected(@NonNull UserHistory.Session session) {
        loadSession(session);
    }

    @Override
    public void onGameSelected(@NonNull SessionHistory.Game game) {
        loadGame(game);
    }

    @Override
    public void onRoundSelected(@NonNull SimpleRound round) {
        showDialog(GameRoundDialog.get(round.id), TAG_ROUND + round.id);
    }
}
