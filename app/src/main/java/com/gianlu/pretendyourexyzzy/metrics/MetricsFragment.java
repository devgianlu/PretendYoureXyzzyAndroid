package com.gianlu.pretendyourexyzzy.metrics;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gianlu.commonutils.CommonUtils;
import com.gianlu.commonutils.misc.BreadcrumbsView;
import com.gianlu.commonutils.ui.Toaster;
import com.gianlu.pretendyourexyzzy.R;
import com.gianlu.pretendyourexyzzy.api.RegisteredPyx;
import com.gianlu.pretendyourexyzzy.api.models.GamePermalink;
import com.gianlu.pretendyourexyzzy.api.models.metrics.SessionHistory;
import com.gianlu.pretendyourexyzzy.api.models.metrics.SimpleRound;
import com.gianlu.pretendyourexyzzy.api.models.metrics.UserHistory;
import com.gianlu.pretendyourexyzzy.databinding.FragmentNewMetricsBinding;
import com.gianlu.pretendyourexyzzy.dialogs.GameRoundDialog;
import com.gianlu.pretendyourexyzzy.main.NewSettingsFragment;

import java.util.Date;

// TODO: Graphic can be improved!
public class MetricsFragment extends NewSettingsFragment.ChildFragment implements BreadcrumbsView.Listener, UserHistoryAdapter.Listener, GamesAdapter.Listener, RoundsAdapter.Listener {
    private static final int TYPE_USER = 0;
    private static final int TYPE_SESSION = 1;
    private static final int TYPE_GAME = 2;
    private static final String TAG_USER = UserHistoryFragment.class.getName();
    private static final String TAG_SESSION = SessionHistoryFragment.class.getName();
    private static final String TAG_GAME = GameHistoryFragment.class.getName();
    private static final String TAG_ROUND = GameRoundDialog.class.getName();
    private static final String TAG = MetricsFragment.class.getSimpleName();
    private RegisteredPyx pyx;
    private FragmentNewMetricsBinding binding;
    private GamePermalink game;

    public MetricsFragment() {
        setArguments(new Bundle());
    }

    private void loadUserHistory() {
        binding.metricsFragmentBreadcrumbs.clear();
        binding.metricsFragmentBreadcrumbs.addItem(new BreadcrumbsView.Item(pyx.user().nickname, TYPE_USER));

        getChildFragmentManager()
                .beginTransaction()
                .replace(binding.metricsFragmentContainer.getId(), UserHistoryFragment.get(), TAG_USER)
                .commit();
    }

    private void loadSession(@NonNull UserHistory.Session session) {
        binding.metricsFragmentBreadcrumbs.addItem(new BreadcrumbsView.Item(session.name(), TYPE_SESSION, session));

        getChildFragmentManager()
                .beginTransaction()
                .replace(binding.metricsFragmentContainer.getId(), SessionHistoryFragment.get(session.id), TAG_SESSION)
                .commit();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNewMetricsBinding.inflate(inflater, container, false);
        binding.metricsFragmentBreadcrumbs.setListener(this);
        binding.metricsFragmentBack.setOnClickListener(v -> super.goBack());

        game = (GamePermalink) requireArguments().getSerializable("game");

        return binding.getRoot();
    }

    @Override
    protected void onPyxReady(@NonNull RegisteredPyx pyx) {
        this.pyx = pyx;

        if (game == null) loadUserHistory();
        else loadGame(game);
    }

    @Override
    protected void onPyxInvalid(@Nullable Exception ex) {
        this.pyx = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.pyx = null;

        binding.metricsFragmentBreadcrumbs.clearListener();
    }

    private void loadGame(@NonNull GamePermalink game) {
        String gameId = game.extractGameMetricsId();
        if (gameId == null) {
            showToast(Toaster.build().message(R.string.failedLoading));
            onBackPressed();
            return;
        }

        showProgress(R.string.loading);
        pyx.getGameHistory(gameId)
                .addOnSuccessListener(requireActivity(), result -> {
                    dismissDialog();
                    binding.metricsFragmentBreadcrumbs.addItem(new BreadcrumbsView.Item(getString(R.string.ongoingGame), TYPE_GAME, null));
                    getChildFragmentManager()
                            .beginTransaction()
                            .replace(binding.metricsFragmentContainer.getId(), GameHistoryFragment.get(result), TAG_GAME)
                            .commit();
                })
                .addOnFailureListener(requireActivity(), ex -> {
                    dismissDialog();
                    Log.e(TAG, "Failed loading history.", ex);
                    showToast(Toaster.build().message(R.string.failedLoading));
                    onBackPressed();
                });
    }

    private void loadGame(@NonNull SessionHistory.Game game) {
        binding.metricsFragmentBreadcrumbs.addItem(new BreadcrumbsView.Item(CommonUtils.getFullDateFormatter().format(new Date(game.timestamp)), TYPE_GAME, game));

        getChildFragmentManager()
                .beginTransaction()
                .replace(binding.metricsFragmentContainer.getId(), GameHistoryFragment.get(game.id), TAG_GAME)
                .commit();
    }

    @Override
    protected boolean goBack() {
        if (binding == null)
            return false;

        if (binding.metricsFragmentBreadcrumbs.navigateBack()) return super.goBack();
        else return true;
    }

    @Override
    public void onSegmentSelected(@NonNull BreadcrumbsView.Item item) {
        binding.metricsFragmentBreadcrumbs.removeFrom(item);

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
