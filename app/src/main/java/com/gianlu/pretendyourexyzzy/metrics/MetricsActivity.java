package com.gianlu.pretendyourexyzzy.metrics;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.dialogs.ActivityWithDialog;
import com.gianlu.pretendyourexyzzy.api.LevelMismatchException;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.GamePermalink;
import com.gianlu.pretendyourexyzzy.api.models.metrics.SessionHistory;
import com.gianlu.pretendyourexyzzy.api.models.metrics.SimpleRound;
import com.gianlu.pretendyourexyzzy.api.models.metrics.UserHistory;
import com.gianlu.pretendyourexyzzy.databinding.FragmentNewMetricsBinding;
import com.gianlu.pretendyourexyzzy.dialogs.GameRoundDialog;

import org.jetbrains.annotations.NotNull;

public class MetricsActivity extends ActivityWithDialog implements MetricsDelegate.Listener, UserHistoryFragment.Listener, SessionHistoryFragment.Listener, RoundsAdapter.Listener {
    private static final String TAG = MetricsActivity.class.getSimpleName();
    private static final String TAG_ROUND = GameRoundDialog.class.getName();
    private MetricsDelegate delegate;

    @NotNull
    public static Intent getGameIntent(@NotNull Context context, @NotNull GamePermalink game) {
        Intent intent = new Intent(context, MetricsActivity.class);
        intent.putExtra("game", game);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FragmentNewMetricsBinding binding = FragmentNewMetricsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        RegisteredPyx pyx;
        try {
            pyx = RegisteredPyx.get();
        } catch (LevelMismatchException ex) {
            Log.e(TAG, "Failed getting Pyx instance.", ex);
            return;
        }

        binding.metricsFragmentBack.setOnClickListener(v -> onBackPressed());

        GamePermalink game = (GamePermalink) getIntent().getSerializableExtra("game");
        delegate = new MetricsDelegate(getSupportFragmentManager(), binding, game, this);
        delegate.setPyx(pyx);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (delegate != null) delegate.destroy();
    }

    @Override
    public void leave() {
        onBackPressed();
    }

    @Override
    public void onGameSelected(@NonNull SessionHistory.Game game) {
        if (delegate != null) delegate.loadGame(game);
    }

    @Override
    public void onSessionSelected(@NonNull UserHistory.Session session) {
        if (delegate != null) delegate.loadSession(session);
    }

    @Override
    public void onRoundSelected(@NonNull SimpleRound round) {
        showDialog(GameRoundDialog.get(round.id), TAG_ROUND + round.id);
    }
}
