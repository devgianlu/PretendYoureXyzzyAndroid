package com.gianlu.pretendyourexyzzy.metrics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.GamePermalink;
import com.gianlu.pretendyourexyzzy.api.models.metrics.SessionHistory;
import com.gianlu.pretendyourexyzzy.api.models.metrics.SimpleRound;
import com.gianlu.pretendyourexyzzy.api.models.metrics.UserHistory;
import com.gianlu.pretendyourexyzzy.databinding.FragmentNewMetricsBinding;
import com.gianlu.pretendyourexyzzy.dialogs.GameRoundDialog;
import com.gianlu.pretendyourexyzzy.main.NewSettingsFragment;

public class MetricsFragment extends NewSettingsFragment.ChildFragment implements MetricsDelegate.Listener, UserHistoryFragment.Listener, SessionHistoryFragment.Listener, RoundsAdapter.Listener {
    private static final String TAG_ROUND = GameRoundDialog.class.getName();
    private MetricsDelegate delegate;

    public MetricsFragment() {
        setArguments(new Bundle());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentNewMetricsBinding binding = FragmentNewMetricsBinding.inflate(inflater, container, false);
        binding.metricsFragmentBack.setOnClickListener(v -> super.goBack());

        GamePermalink game = (GamePermalink) requireArguments().getSerializable("game");
        delegate = new MetricsDelegate(getChildFragmentManager(), binding, game, this);

        return binding.getRoot();
    }

    @Override
    protected void onPyxReady(@NonNull RegisteredPyx pyx) {
        if (delegate != null) delegate.setPyx(pyx);
    }

    @Override
    protected void onPyxInvalid(@Nullable Exception ex) {
        if (delegate != null) delegate.unsetPyx();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (delegate != null) delegate.destroy();
    }

    @Override
    protected boolean goBack() {
        if (delegate == null)
            return false;

        if (delegate.navigateBack()) return super.goBack();
        else return true;
    }

    @Override
    public void leave() {
        super.goBack();
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
