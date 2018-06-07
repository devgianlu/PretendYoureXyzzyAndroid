package com.gianlu.pretendyourexyzzy.Metrics;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

import com.gianlu.commonutils.BreadcrumbsView;
import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.Dialogs.ActivityWithDialog;
import com.gianlu.commonutils.Toaster;
import com.gianlu.pretendyourexyzzy.Dialogs.GameRoundDialog;
import com.gianlu.pretendyourexyzzy.NetIO.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Metrics.SessionHistory;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Metrics.SimpleRound;
import com.gianlu.pretendyourexyzzy.NetIO.Models.Metrics.UserHistory;
import com.gianlu.pretendyourexyzzy.NetIO.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.R;

import java.util.Date;

public class MetricsActivity extends ActivityWithDialog implements BreadcrumbsView.Listener, UserHistoryAdapter.Listener, GamesAdapter.Listener, RoundsAdapter.Listener {
    private static final int TYPE_USER = 0;
    private static final int TYPE_SESSION = 1;
    private static final int TYPE_GAME = 2;
    private static final String TAG_USER = UserHistoryFragment.class.getName();
    private static final String TAG_SESSION = SessionHistoryFragment.class.getName();
    private static final String TAG_GAME = GameHistoryFragment.class.getName();
    private static final String TAG_ROUND = GameRoundDialog.class.getName();
    private RegisteredPyx pyx;
    private BreadcrumbsView breadcrumbs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_metrics);
        setSupportActionBar((Toolbar) findViewById(R.id.metrics_toolbar));
        setTitle(R.string.metrics);

        ActionBar bar = getSupportActionBar();
        if (bar != null) bar.setDisplayHomeAsUpEnabled(true);

        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            Toaster.with(this).message(R.string.failedLoading).ex(ex).show();
            onBackPressed();
            return;
        }

        breadcrumbs = findViewById(R.id.metrics_breadcrumbs);
        breadcrumbs.setListener(this);

        loadUserHistory();
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
        String text;
        if (session.server == null) text = session.id;
        else text = session.server.name;
        breadcrumbs.addItem(new BreadcrumbsView.Item(text, TYPE_SESSION, session));

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.metrics_container, SessionHistoryFragment.get(session.id), TAG_SESSION)
                .commit();
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
        if (breadcrumbs.navigateBack())
            super.onBackPressed();
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
        GameRoundDialog.get(round.id).show(getSupportFragmentManager(), TAG_ROUND + round.id);
    }
}
